package burp.model

import burp.model.TemplateReferenceSafety.Unsafe
import burp.model.gathermap.GatherMap
import burp.reporting.BurpException
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.vocabularies.RER
import burp.vocabularies.Rml
import rdfobjectloader.RDFPointer
import rdfobjectloader.annotations.RdfProperty

class ReferencingObjectMap : TermGenerator, PlanNode, ParentJoinReferenceScope, BaseObjectMap {
    @RdfProperty(Rml.parentTriplesMap)
    var parentTriplesMap: TriplesMap? = null

    @RdfProperty(Rml.joinCondition)
    var joinConditions = mutableListOf<JoinCondition>()
    var logicalTargets: MutableSet<LogicalTarget> = mutableSetOf()

    @RdfProperty(Rml.gather)
    var gatherMap: GatherMap? = null

    override var parent: PlanNode? = null
    override fun children(): Sequence<PlanNode> = sequence {
        yieldAll(joinConditions)
        if (gatherMap != null) {
            yield(gatherMap!!)
        }
    }

    override fun dependencies(): Sequence<PlanNode> =
        sequence {
            yieldAll(children())
            yield(parentTriplesMap!!.subjectMap)
        }

    override fun generateTerms(i: Iteration): List<Term> {

        if (gatherMap != null) {
            @Suppress("UNCHECKED_CAST") // Cast guaranteed because SubjectMap
            val generatedIds = generateTermsFromParentJoins(i) as List<BlankNodeOrIRI>
            return gatherMap!!.generateTerms(i, generatedIds)
        }

        return generateTermsFromParentJoins(i)
    }

    fun generateTermsFromParentJoins(i: Iteration): List<Term> {
        // If there are no join conditions, then we generate resources
        // from the child iteration. This is only guaranteed to work
        // for logical sources of the same type or if the parent triple
        // map' subject map only uses simple references.
        if (joinConditions.isEmpty()) {
            return parentTriplesMap!!.subjectMap.generateTerms(i)
        } else {
            val list = mutableListOf<Term>()
            val parentIterator = parentTriplesMap!!.logicalSource?.iterator() ?: throw BurpException(
                RmlError(
                    "Constant triples map in referencing object map $this for triples map $parentTriplesMap (without logical source) are not supported.",
                    null,
                    RER.UnsupportedMapping
                )
            )
            parentIterator.forEach { parentIteration ->
                // Expression Maps are multi-valued. We thus need for each join condition at least one match.
                var ok = true
                for (jc in joinConditions) {
                    val valuesChild = jc.childMap.generateValues(i, Unsafe)
                    val valuesParent = jc.parentMap.generateValues(parentIteration, Unsafe)

                    if (!valuesChild.any { vC -> valuesParent.any { vP -> valuesMatch(vC, vP) } }) {
                        // No match, break.
                        ok = false
                        break
                    }
                }

                if (ok) {
                    list.addAll(parentTriplesMap!!.subjectMap.generateTerms(parentIteration))
                }
            }

            return list
        }
    }

    override fun buildParentJoinReference(reference: String, origin: RDFPointer): Reference {
        if (parentTriplesMap == null)
            throw BurpException(
                RmlError(
                    "ReferencingObjectMap is missing parentTriplesMap",
                    Origin(this, origin),
                    RER.UnsupportedMapping
                )
            )
        return parentTriplesMap!!.buildLocalReference(reference, origin)
    }
}