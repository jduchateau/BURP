package burp.parse

import burp.Main
import burp.model.*
import burp.model.gathermaputil.GatherMapMixin
import burp.vocabularies.RML
import burp.vocabularies.YS
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.lib.ShLib
import org.apache.jena.util.FileUtils
import org.apache.jena.vocabulary.RDF
import java.nio.file.Paths

class Parse {
    var triplesMaps: MutableMap<Resource?, TriplesMap> = mutableMapOf()

    @Throws(Exception::class)
    fun parseMappingFile(mappingFile: String): List<TriplesMap> {
        val mpath = Paths.get(mappingFile).toAbsolutePath().parent.toString()
        val mapping = RDFDataMgr.loadModel(mappingFile)
        Main.prefixes = mapping.nsPrefixMap
        if (!Main.conf.skipShapeValidation)
            if (!isValid(mapping)) throw RuntimeException("Mapping did not satisfy shapes.")
        var tms = parseMapping(mapping, mpath)

        if (Main.conf.indexedJoins) {
            tms = prepareForIndexedJoins(tms)
        }

        return tms
    }

    /**
     * Parse the mapping graph to TriplesMap objects
     *
     * @param mapping the mapping graph
     * @param mpath   the folder containing the mapping file
     * @return the list of triples maps
     */
    @Throws(Exception::class)
    fun parseMapping(mapping: Model, mpath: String?): List<TriplesMap> {
        triplesMaps = HashMap()


        // Replace rml:subject, rml:object, ... with constant expression maps
        normalizeConstants(mapping)

        // Look for the triples maps
        val list = mapping.listSubjectsWithProperty(RML.logicalSource).toList()

        // Process each triples map
        for (r in list) {
            val tm = triplesMaps.computeIfAbsent(r) { TriplesMap(it) }

            val ls = r.getPropertyResourceValue(RML.logicalSource)
            tm.logicalSource = prepareLogicalSource(ls, mpath)

            val sm = r.getPropertyResourceValue(RML.subjectMap)
            tm.subjectMap = prepareSubjectMap(sm)
            tm.predicateObjectMaps = r.listProperties(RML.predicateObjectMap)
                .mapWith { preparePredicateObjectMap(it.getObject().asResource()) }.toList()
        }

        return triplesMaps.values.toList()
    }

    private fun isValid(mapping: Model): Boolean {
        val core = ModelFactory.createDefaultModel()
        core.read(Parse::class.java.getResourceAsStream("/shapes/core.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(Parse::class.java.getResourceAsStream("/shapes/cc.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(Parse::class.java.getResourceAsStream("/shapes/io.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(Parse::class.java.getResourceAsStream("/shapes/fnml.ttl"), "urn:dummy", FileUtils.langTurtle)

        val report = ShaclValidator.get().validate(core.graph, mapping.graph)
        if (!report.conforms()) {
            ShLib.printReport(report)
            return false
        }

        return true
    }

    private fun normalizeConstants(mapping: Model) {
        val CONSTRUCTSMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:subjectMap [ r:constant ?y ]. } WHERE { ?x r:subject ?y. }"
        val CONSTRUCTOMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:objectMap [ r:constant ?y ]. } WHERE { ?x r:object ?y. }"
        val CONSTRUCTPMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:predicateMap [ r:constant ?y ]. } WHERE { ?x r:predicate ?y. }"
        val CONSTRUCTGMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:graphMap [ r:constant ?y ]. } WHERE { ?x r:graph ?y. }"

        mapping.add(QueryExecutionFactory.create(CONSTRUCTSMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTOMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTPMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTGMAPS, mapping).execConstruct())

        val CONSTRUCTLMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:languageMap [ r:constant ?y ]. } WHERE { ?x r:language ?y. }"
        val CONSTRUCTDMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:datatypeMap [ r:constant ?y ]. } WHERE { ?x r:datatype ?y. }"

        mapping.add(QueryExecutionFactory.create(CONSTRUCTLMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTDMAPS, mapping).execConstruct())

        val CONSTRUCTChMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:childMap [ r:reference ?y ]. } WHERE { ?x r:child ?y. }"
        val CONSTRUCTPaMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:parentMap [ r:reference ?y ]. } WHERE { ?x r:parent ?y. }"

        mapping.add(QueryExecutionFactory.create(CONSTRUCTChMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTPaMAPS, mapping).execConstruct())

        val CONSTRUCTRETURNMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:returnMap [ r:constant ?y ]. } WHERE { ?x r:return ?y. }"
        val CONSTRUCTFUNCTIONMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:functionMap [ r:constant ?y ]. } WHERE { ?x r:function ?y. }"
        val CONSTRUCTPARAMETERMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:parameterMap [ r:constant ?y ]. } WHERE { ?x r:parameter ?y. }"
        val INPUTVALUEMAPS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:inputValueMap [ r:constant ?y ]. } WHERE { ?x r:inputValue ?y. }"

        mapping.add(QueryExecutionFactory.create(CONSTRUCTRETURNMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTFUNCTIONMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(CONSTRUCTPARAMETERMAPS, mapping).execConstruct())
        mapping.add(QueryExecutionFactory.create(INPUTVALUEMAPS, mapping).execConstruct())

        val TERMTYPESTOCONSTANTS =
            "PREFIX r: <http://w3id.org/rml/> CONSTRUCT { ?x r:constant ?y ; r:termType ?z . } WHERE { ?x r:constant ?y. BIND(IF(ISLITERAL(?y), r:Literal, IF(ISIRI(?y), r:IRI, r:BlankNode)) AS ?z)}"
        mapping.add(QueryExecutionFactory.create(TERMTYPESTOCONSTANTS, mapping).execConstruct())

        // Graph maps, subject maps, and object maps can have no reference
        // They will generate blank nodes, thus add term type BN
        //FIXME Why not doing it during parsing ?
        val IMPLICITTERMTYPE = """
                PREFIX r: <http://w3id.org/rml/>
                CONSTRUCT { ?x r:termType r:BlankNode . }
                WHERE {
                  VALUES (?rmlMap) { (r:subjectMap) (r:graphMap) (r:objectMap) }
                  [] ?rmlMap ?x .
                  OPTIONAL { ?x r:template ?a . }
                  OPTIONAL { ?x r:reference ?b . }
                  OPTIONAL { ?x r:constant ?c . }
                  OPTIONAL { ?x r:functionExecution ?d . }
                  FILTER (!bound(?a) && !bound(?b) && !bound(?c) && !bound(?d))
                }
                
                """.trimIndent()
        mapping.add(QueryExecutionFactory.create(IMPLICITTERMTYPE, mapping).execConstruct())
    }

    @Throws(Exception::class)
    private fun prepareLogicalSource(ls: Resource, mpath: String?): LogicalSource {
        val referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation)

        if (RML.CSV == referenceFormulation) return LogicalSourceFactory.createCSVSource(ls, mpath)

        if (RML.JSONPath == referenceFormulation) return LogicalSourceFactory.createJSONSource(ls, mpath)

        if (RML.XPath == referenceFormulation) return LogicalSourceFactory.createXMLSource(ls, mpath)

        if (referenceFormulation.hasProperty(
                RDF.type,
                RML.XPathReferenceFormulation
            )
        ) return LogicalSourceFactory.createXMLSource(ls, mpath)

        if (RML.SQL2008Table == referenceFormulation || RML.SQLTable == referenceFormulation) return LogicalSourceFactory.createSQL2008TableSource(
            ls,
            mpath
        )

        if (RML.SQL2008Query == referenceFormulation || RML.SQLQuery == referenceFormulation) return LogicalSourceFactory.createSQL2008QuerySource(
            ls,
            mpath
        )

        if (RML.SPARQL_Results_CSV == referenceFormulation) return LogicalSourceFactory.createSPARQLSource(
            ls,
            mpath,
            false
        )

        if (RML.SPARQL_Results_TSV == referenceFormulation) return LogicalSourceFactory.createSPARQLSource(
            ls,
            mpath,
            true
        )

        if (RML.SPARQL_Results_XML == referenceFormulation) return LogicalSourceFactory.createSPARQLSource(
            ls,
            mpath,
            false
        )

        if (RML.SPARQL_Results_JSON == referenceFormulation) return LogicalSourceFactory.createSPARQLSource(
            ls,
            mpath,
            false
        )

        if (referenceFormulation.hasProperty(
                RDF.type,
                YS.NetconfQuerySource
            )
        ) return LogicalSourceFactory.createNetconfQuerySource(ls)

        throw Exception("Reference formulation not (yet) supported: $referenceFormulation")
    }

    private fun prepareSubjectMap(sm: Resource): SubjectMap {
        val subjectMap = SubjectMap(prepareExpression(sm))

        sm.listProperties(RML.clazz).forEach { s: Statement? ->
            subjectMap.classes.add(s!!.getObject().asResource())
        }

        sm.listProperties(RML.graphMap).forEach { s: Statement? ->
            val gm = prepareGraphMap(s!!.getObject().asResource())
            subjectMap.graphMaps.add(gm)
        }

        val termType = sm.getPropertyResourceValue(RML.termType)
        if (termType != null)  // PROVIDE THE TERM TYPE THAT IS GIVEN
            subjectMap.termType = termType
        else if (hasNoTemplateReferenceConstantOrFunction(sm)) {
            // IF NO REFERENCE, TEMPLATE, CONSTANT, OR FUNCTION
            // THEN WE GENERATE BLANK NODES (BASED ON THE ITERATION)
            subjectMap.termType = RML.BLANKNODE
        }

        val gm = sm.getPropertyResourceValue(RML.gather)
        if (gm != null) {
            // This object map has a gather, so we process it as a gather map
            subjectMap.gatherMap = prepareGatherMap(sm)
        }

        return subjectMap
    }

    private fun preparePredicateObjectMap(pom: Resource): PredicateObjectMap {
        val predicateObjectMap = PredicateObjectMap()

        pom.listProperties(RML.graphMap).forEach { s: Statement? ->
            val gm = prepareGraphMap(s!!.getObject().asResource())
            predicateObjectMap.graphMaps.add(gm)
        }

        pom.listProperties(RML.predicateMap).forEach { s: Statement? ->
            val pm = preparePredicateMap(s!!.getObject().asResource())
            predicateObjectMap.predicateMaps.add(pm)
        }

        pom.listProperties(RML.objectMap).forEach { s: Statement? ->
            if (s!!.getObject().asResource().getProperty(RML.parentTriplesMap) == null) {
                val om = prepareObjectMap(s.getObject().asResource())
                predicateObjectMap.objectMaps.add(om)
            } else {
                val rom = prepareReferencingObjectMap(s.getObject().asResource())
                predicateObjectMap.refObjectMaps.add(rom)
            }
        }

        return predicateObjectMap
    }

    private fun prepareGraphMap(r: Resource): GraphMap {
        val gm = GraphMap(prepareExpression(r))

        val termType = r.getPropertyResourceValue(RML.termType)
        if (termType != null)  // PROVIDE THE TERM TYPE THAT IS GIVEN
            gm.termType = termType
        else if (hasNoTemplateReferenceConstantOrFunction(r)) {
            // IF NO REFERENCE, TEMPLATE, CONSTANT, OR FUNCTION
            // THEN WE GENERATE BLANK NODES (BASED ON THE ITERATION)
            gm.termType = RML.BLANKNODE
        }

        return gm
    }

    private fun preparePredicateMap(pm: Resource): PredicateMap {
        return PredicateMap(prepareExpression(pm))
    }

    private fun prepareObjectMap(om: Resource): ObjectMap {
        val objectMap = ObjectMap(prepareExpression(om))

        val termType = om.getPropertyResourceValue(RML.termType)
        if (termType != null)  // PROVIDE THE TERM TYPE THAT IS GIVEN
            objectMap.termType = termType
        else if (hasNoTemplateReferenceConstantOrFunction(om)) {
            // IF NO REFERENCE, TEMPLATE, CONSTANT,
            // OR FUNCTION THEN WE GENERATE BLANK NODES (BASED ON THE ITERATION)
            objectMap.termType = RML.BLANKNODE
        }

        val lam = om.getPropertyResourceValue(RML.languageMap)
        if (lam != null) objectMap.languageMap = prepareLanguageMap(lam)

        val dtm = om.getPropertyResourceValue(RML.datatypeMap)
        if (dtm != null) objectMap.datatypeMap = prepareDatatypeMap(dtm)

        if (termType == null && (lam != null || dtm != null || objectMap.expression is Reference || objectMap.expression is FunctionExecution)) objectMap.termType =
            RML.LITERAL

        val gm = om.getPropertyResourceValue(RML.gather)
        if (gm != null) {
            // This object map has a gather, so we process it as a gather map
            objectMap.gatherMap = prepareGatherMap(om)
        }

        return objectMap
    }

    private fun prepareGatherMap(gm: Resource): GatherMapMixin {
        val gatherMap = GatherMapMixin()

        if (gm.hasProperty(RML.allowEmptyListAndContainer)) {
            gatherMap.allowEmptyListAndContainer =
                gm.getProperty(RML.allowEmptyListAndContainer).getObject().asLiteral().boolean
        }

        if (gm.hasProperty(RML.gatherAs)) {
            gatherMap.gatherAs = gm.getPropertyResourceValue(RML.gatherAs)
        }

        if (gm.hasProperty(RML.strategy)) {
            gatherMap.strategy = gm.getPropertyResourceValue(RML.strategy)
        }

        val list = gm.getPropertyResourceValue(RML.gather).`as`<RDFList>(RDFList::class.java)
        val iter = list.iterator()
        while (iter.hasNext()) {
            val r = iter.next()!!.asResource()

            if (r.hasProperty(RML.parentTriplesMap)) {
                val rom = prepareReferencingObjectMap(r)
                gatherMap.gatherMaps.add(rom)
            } else {
                val om = prepareObjectMap(r)
                gatherMap.gatherMaps.add(om)
            }
        }

        return gatherMap
    }

    private fun prepareDatatypeMap(dtm: Resource): DatatypeMap {
        return DatatypeMap(prepareExpression(dtm))
    }

    private fun prepareLanguageMap(lam: Resource): LanguageMap {
        return LanguageMap(prepareExpression(lam))
    }

    private fun prepareReferencingObjectMap(rom: Resource): ReferencingObjectMap {
        val referencingObjectMap = ReferencingObjectMap()

        val p = rom.getPropertyResourceValue(RML.parentTriplesMap)
        referencingObjectMap.parentTriplesMap = triplesMaps.computeIfAbsent(p) { TriplesMap(it) }

        referencingObjectMap.joinConditions = rom.listProperties(RML.joinCondition).mapWith { stmt ->
            val jc = JoinCondition()
            val jcr = stmt.getObject().asResource()

            val parentMapRes: Resource? = jcr.getPropertyResourceValue(RML.parentMap)
            jc.parentMap = prepareExpressionMap(
                parentMapRes
                    ?: throw Exception("A join condition must have a parent map in $jcr")
            )

            val childMapRes: Resource? = jcr.getPropertyResourceValue(RML.childMap)
            jc.childMap = prepareExpressionMap(
                childMapRes
                    ?: throw Exception("A join condition must have a child map in $jcr")
            )
            jc
        }.toList()

        return referencingObjectMap
    }

    private fun prepareExpressionMap(em: Resource): ConcreteExpressionMap {
        return ConcreteExpressionMap(prepareExpression(em))
    }

    private fun prepareExpression(r: Resource): Expression? {
        if (r.hasProperty(RML.constant)) {
            val constant = r.getProperty(RML.constant).getObject()
            return RDFNodeConstant(constant)
        }

        if (r.hasProperty(RML.reference)) {
            val reference = r.getProperty(RML.reference).getObject().asLiteral().string
            return Reference(reference)
        }

        if (r.hasProperty(RML.template)) {
            val template = r.getProperty(RML.template).getObject().asLiteral().string
            if (Main.conf.allowPrefixInTemplate) {
                for ((prefix, ns) in Main.prefixes) {
                    val fullPrefix = "$prefix:"
                    if (template.startsWith(fullPrefix))
                        return Template(template.replaceFirst(fullPrefix, ns))
                }
            }
            return Template(template)
        }

        if (r.hasProperty(RML.functionExecution)) {
            val fer = r.getPropertyResourceValue(RML.functionExecution)

            val fe = FunctionExecution()
            fe.functionMap = prepareFunctionMap(fer.getPropertyResourceValue(RML.functionMap))

            // Return Maps are siblings of Function Execution Maps
            if (r.hasProperty(RML.returnMap)) fe.returnMap = prepareReturnMap(r.getPropertyResourceValue(RML.returnMap))

            val iter = fer.listProperties(RML.input)
            while (iter.hasNext()) {
                val x = iter.next()
                fe.inputs.add(prepareInput(x.getObject().asResource()))
            }

            return fe
        }

        return null
    }

    private fun prepareInput(r: Resource): Input {
        val input = Input()

        input.parameterMap = ParameterMap(prepareExpression(r.getPropertyResourceValue(RML.parameterMap)))
        input.inputValueMap = prepareInputValueMap(r.getPropertyResourceValue(RML.inputValueMap))

        return input
    }

    private fun prepareFunctionMap(r: Resource): FunctionMap {
        return FunctionMap(prepareExpression(r))
    }

    private fun prepareReturnMap(r: Resource): ReturnMap {
        return ReturnMap(prepareExpression(r))
    }

    private fun prepareInputValueMap(om: Resource): InputValueMap {
        val im = InputValueMap(prepareExpression(om))

        val termType = om.getPropertyResourceValue(RML.termType)
        if (termType != null) im.termType = termType

        val lam = om.getPropertyResourceValue(RML.languageMap)
        if (lam != null) im.languageMap = prepareLanguageMap(lam)

        val dtm = om.getPropertyResourceValue(RML.datatypeMap)
        if (dtm != null) im.datatypeMap = prepareDatatypeMap(dtm)

        if (termType == null && (lam != null || dtm != null || im.expression is Reference || im.expression is FunctionExecution)) im.termType =
            RML.LITERAL

        return im
    }

    private fun hasNoTemplateReferenceConstantOrFunction(r: Resource): Boolean {
        return !r.hasProperty(RML.template) && !r.hasProperty(RML.reference) && !r.hasProperty(RML.constant) && !r.hasProperty(
            RML.functionExecution
        )
    }
}
