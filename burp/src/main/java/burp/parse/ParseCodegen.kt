package burp.parse

import burp.Main
import burp.model.*
import burp.model.gathermap.GatherMap
import burp.model.lv.*
import burp.reporting.*
import burp.vocabularies.RER
import burp.vocabularies.RML
import burp.vocabularies.Rml
import org.apache.jena.query.ParameterizedSparqlString
import org.apache.jena.query.QueryParseException
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.validation.ReportEntry
import org.apache.jena.sparql.path.P_NegPropSet
import org.apache.jena.sparql.path.P_Path0
import org.apache.jena.sparql.path.P_Path1
import org.apache.jena.sparql.path.P_Path2
import org.apache.jena.update.UpdateAction
import org.apache.jena.update.UpdateFactory
import org.apache.jena.util.FileUtils
import rdfkt.JenaBlankNode
import rdfkt.JenaDataset
import rdfkt.JenaNamedNode
import rdfkt.toTerm
import rdfobjectloader.CommonRdfObjectLoader
import rdfobjectloader.GeneratedMappersRegistry
import rdfobjectloader.StatementPart
import rdfobjectloader.StatementParts
import turtleprov.parseTurtleFromFile
import java.nio.file.Path

class ParseCodegen {
    private var mappingDirectory: Path? = null
    private var mappingFile: Path? = null
    private var currentDirectory: Path? = null
    private var mapping: Model? = null

    @Throws(Exception::class)
    fun parseMappingFile(mappingPath: Path, currentDirectory: Path?): MutableList<TriplesMap> {
        this.mappingFile = mappingPath.toAbsolutePath().normalize()
        this.mappingDirectory = mappingFile!!.parent
        this.currentDirectory = currentDirectory

        val guessType = RDFDataMgr.determineLang(mappingPath.toString(), null, null)

        if (guessType === Lang.TURTLE) {
            val dataset = parseTurtleFromFile(mappingPath.toFile())
            mapping = dataset.getDefaultModel()
        } else {
            mapping = RDFDataMgr.loadModel(mappingPath.toString())
        }

        if (!isValid(mapping!!))
            throw BurpException(RmlError("Mapping did not satisfy shapes.", null, RER.MappingError))

        normalizeConstantsUpdate(mapping!!)

        // Use the codegen loader
        val loader = CommonRdfObjectLoader()
        GeneratedMappersRegistry.registerAll(loader)
        loader.registerTypeDecider(burp.model.deciders.ObjectMapTypeDecider())
        loader.registerTypeDecider(burp.model.deciders.LogicalSourceTypeDecider())
        loader.registerTypeDecider(burp.model.deciders.ExpressionTypeDecider())

        // Register our manual mappers
        loader.registerMapper(AbstractLogicalSource::class, AbstractLogicalSourceMapper(mapping!!, mappingDirectory!!, currentDirectory!!))
        loader.registerMapper(LogicalView::class, LogicalViewMapper(mapping!!))
        loader.registerMapper(ViewJoin::class, ViewJoinMapper(mapping!!))
        loader.registerMapper(Field::class, FieldMapper(mapping!!))
        loader.registerMapper(ExpressionField::class, ExpressionFieldMapper(mapping!!))
        loader.registerMapper(IterableField::class, IterableFieldMapper(mapping!!))
        loader.registerMapper(GatherMap::class, GatherMapMapper())

        val dataset = JenaDataset(mapping!!)

        // Look for the triples maps
        val list = mapping!!.listSubjectsWithProperty(RML.logicalSource).toList()
        val triplesMaps = mutableListOf<TriplesMap>()

        // Process each triples map
        for (r in list) {
            val tm = loader.map(dataset, JenaNamedNode(r), setOf(TriplesMap::class))
            triplesMaps.add(tm)
        }

        // Post-process term types for all TermMaps to assign default/dynamic termType if not explicitly given in RDF
        for ((term, instance) in loader.getCache()) {
            if (instance is TermMap) {
                val hasExplicitTermType = dataset.match(subject = term, predicate = rdfkt.NamedTerm("http://w3id.org/rml/termType")).any()
                if (!hasExplicitTermType) {
                    val expr = instance.expression
                    if (expr is RDFNodeConstant) {
                        val constVal = expr.constant
                        if (constVal is rdf.Literal) {
                            instance.termType = rdfkt.NamedTerm(Rml.Literal)
                        } else if (constVal is rdf.BlankNode) {
                            instance.termType = rdfkt.NamedTerm(Rml.BlankNode)
                        } else {
                            instance.termType = rdfkt.NamedTerm(Rml.IRI)
                        }
                    } else {
                        if (instance is SubjectMap) {
                            instance.termType = rdfkt.NamedTerm(Rml.IRI)
                        } else if (instance is ObjectMap) {
                            val isLiteralDefault = instance.languageMap != null ||
                                    instance.datatypeMap != null ||
                                    instance.expression is Reference ||
                                    instance.expression is FunctionExecution
                            if (isLiteralDefault) {
                                instance.termType = rdfkt.NamedTerm(Rml.Literal)
                            } else {
                                instance.termType = rdfkt.NamedTerm(Rml.IRI)
                            }
                        } else if (instance is GraphMap || instance is PredicateMap || instance is FunctionMap || instance is ParameterMap || instance is ReturnMap) {
                            instance.termType = rdfkt.NamedTerm(Rml.IRI)
                        } else if (instance is InputValueMap) {
                            instance.termType = rdfkt.NamedTerm(Rml.Literal)
                        }
                    }
                }
            }
        }

        // Populate logical targets for all mapped elements
        populateLogicalTargets(mapping!!, loader)

        // Wiring parent relations (similar to prepareReferencingObjectMap and prepareLogicalSource)
        for (tm in triplesMaps) {
            tm.subjectMap.parent = tm
            tm.logicalSource?.parent = tm
            for (pom in tm.predicateObjectMaps) {
                pom.parent = tm
                for (om in pom.objectMaps) {
                    om.parent = pom
                }
            }
        }

        return triplesMaps
    }

    private fun populateLogicalTargets(mapping: Model, loader: CommonRdfObjectLoader) {
        val cache = loader.getCache()
        val logicalTargetsMap = mutableMapOf<Resource, LogicalTarget>()

        fun getOrCreateLogicalTarget(r: Resource): LogicalTarget {
            return logicalTargetsMap.computeIfAbsent(r) { res ->
                val targetStmt = res.getProperty(RML.target) ?: throw BurpException(
                    RmlError("LogicalTarget has no target", null, RER.MappingError)
                )
                val targetRes = targetStmt.resource
                val target: RMLTarget = if (targetRes.hasProperty(RML.path)) {
                    val path = targetRes.getProperty(RML.path).getObject().asLiteral().getString()
                    val root = targetRes.getPropertyResourceValue(RML.root) ?: RML.CurrentWorkingDirectory
                    FilePathTarget(path, root.toTerm())
                } else {
                    throw BurpException(RmlError("Unsupported target type", null, RER.MappingError))
                }

                val serialization = res.getPropertyResourceValue(RML.serialization)
                val compression = res.getPropertyResourceValue(RML.compression)
                val encoding = res.getPropertyResourceValue(RML.encoding)

                LogicalTarget(target, serialization?.toTerm(), compression?.toTerm(), encoding?.toTerm())
            }
        }

        for ((term, instance) in cache) {
            val jenaResource = when (term) {
                is JenaNamedNode -> term.node
                is JenaBlankNode -> term.node
                else -> continue
            }

            if (instance is LogicalTargetScope) {
                jenaResource.listProperties(RML.logicalTarget).forEach { stmt ->
                    instance.logicalTargets.add(getOrCreateLogicalTarget(stmt.resource))
                }
            }
        }
    }

    private fun isValid(mapping: Model): Boolean {
        val core = ModelFactory.createDefaultModel()
        core.read(ParseCodegen::class.java.getResourceAsStream("/shapes/rml-core/core.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(ParseCodegen::class.java.getResourceAsStream("/shapes/rml-cc/cc.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(ParseCodegen::class.java.getResourceAsStream("/shapes/rml-lv/lv.ttl"), "urn:dummy", FileUtils.langTurtle)

        val report = ShaclValidator.get().validate(core.graph, mapping.graph)
        if (!report.conforms()) {
            report.entries.forEach { vr ->
                val focusOrigins = extractStatementsFromShaclViolation(vr, mapping)
                Main.report.errors.add(
                    RmlError(
                        "${vr.message()} \nNode=${vr.focusNode()}\nPath=${vr.resultPath()}\nValue: ${vr.value()}\n",
                        Origin(sourceStatements = focusOrigins.ifEmpty { null }),
                        RER.MappingError,
                        )
                )
            }
            return false
        }

        return true
    }

    fun extractStatementsFromShaclViolation(vr: ReportEntry, mapping: Model): List<StatementParts> {
        val results = mutableListOf<StatementParts>()

        val focusNode = vr.focusNode()
        val resultPath = vr.resultPath()
        val value = vr.value()

        if (focusNode == null) return results

        val isURI = focusNode.isURI
        val isBlank = focusNode.isBlank

        if (!isURI && !isBlank) return results

        val focusResource = if (isURI) mapping.getResource(focusNode.toString()) else null

        val valueNode: RDFNode? = if (value != null && value.isConcrete) {
            when {
                value.isURI -> mapping.getResource(value.toString())
                value.isBlank -> mapping.getResource(value.toString())
                value.isLiteral -> mapping.createTypedLiteral(value.literalLexicalForm, value.literalDatatype)
                else -> null
            }
        } else null

        if (resultPath != null) {
            when (resultPath) {
                is P_Path0 -> {
                    val predicate = mapping.getProperty(resultPath.node.uri)
                    if (valueNode != null) {
                        mapping.listStatements(focusResource, predicate, valueNode).forEach { stmt ->
                            results.add(
                                StatementParts.from(
                                    rdfkt.JenaQuad(stmt),
                                    StatementPart.Subject,
                                    StatementPart.Predicate,
                                    StatementPart.Object
                                )
                            )
                        }
                    } else {
                        mapping.listStatements(focusResource, predicate, null as RDFNode?).forEach { stmt ->
                            results.add(
                                StatementParts.from(
                                    rdfkt.JenaQuad(stmt),
                                    StatementPart.Subject,
                                    StatementPart.Predicate,
                                    StatementPart.Object
                                )
                            )
                        }
                    }
                }

                is P_Path1 -> {
                    if (resultPath is org.apache.jena.sparql.path.P_Inverse) {
                        val subPath = resultPath.subPath
                        if (subPath is P_Path0) {
                            val predicate = mapping.getProperty(subPath.node.uri)
                            mapping.listStatements(
                                if (valueNode?.isResource == true) valueNode.asResource() else null,
                                predicate,
                                focusResource
                            ).forEach { stmt ->
                                results.add(
                                    StatementParts.from(
                                        rdfkt.JenaQuad(stmt),
                                        StatementPart.Subject,
                                        StatementPart.Predicate,
                                        StatementPart.Object
                                    )
                                )
                            }
                        }
                    }
                }

                is P_Path2 -> {}
                is P_NegPropSet -> {}
            }
        }

        if (results.isEmpty()) {
            mapping.listStatements(focusResource, null, valueNode).forEach { stmt ->
                results.add(StatementParts.from(rdfkt.JenaQuad(stmt), StatementPart.Subject))
            }

            mapping.listStatements(null, null, focusResource).forEach { stmt ->
                if (!results.any { it.stmt == rdfkt.JenaQuad(stmt) }) {
                    results.add(StatementParts.from(rdfkt.JenaQuad(stmt), StatementPart.Object))
                }
            }
        }

        return results
    }

    private fun normalizeConstantsUpdate(mapping: Model) {
        val conditionShortcutExpand = """
        PREFIX rml: <http://w3id.org/rml/>
        PREFIX idlab-fn: <https://w3id.org/imec/idlab/function#>
        
        DELETE {
            ?map rml:condition ?condition .
            ?map ?prop ?value .
        }
        INSERT {
            ?map rml:functionExecution [
                rml:function idlab-fn:IF ;
                rml:input [
                    rml:parameter idlab-fn:boolParameter ;
                    rml:inputValueMap ?condition
                ] , [
                    rml:parameter idlab-fn:expressionParameter ;
                    rml:inputValueMap [
                        ?prop ?value
                    ]
                ]
            ] .
        }
        WHERE {
            ?map rml:condition ?condition .
            ?map ?prop ?value .
            VALUES ?prop { rml:constant rml:reference rml:template rml:functionExecution }
        }
    """

        val constructTermTypes = """
        PREFIX r: <http://w3id.org/rml/>
        INSERT { ?x r:constant ?y ; r:termType ?z . }
        WHERE {
            ?x r:constant ?y.
            BIND(IF(ISLITERAL(?y), r:Literal, IF(ISIRI(?y), r:IRI, r:BlankNode)) AS ?z)
        }
    """

        val updateQueries = listOf(
            conditionShortcutExpand
        )

        updateQueries.forEach { query ->
            try {
                val update = UpdateFactory.create(query)
                UpdateAction.execute(update, mapping)
            } catch (e: QueryParseException) {
                throw BurpException(
                    RmlError(
                        "Normalize mapping failed: $query",
                        null,
                        RER.UnexpectedError,
                        exception = e
                    )
                )
            }

        }
    }

    fun expandShortcut(mapType: Property, expressionType: Property, mapTypeShort: Property): String {
        val pss = ParameterizedSparqlString(
            """
            INSERT { ?x ?mapType [ ?expressionType ?y ]. }
            WHERE { ?x ?mapTypeShort ?y . }
            """
        )
        pss.setIri("mapType", mapType.uri)
        pss.setIri("expressionType", expressionType.uri)
        pss.setIri("mapTypeShort", mapTypeShort.uri)
        return pss.toString()

    }

    // Helper function to generate the implicit term type query
    fun constructImplicitTermTypeQuery(mapType: Property): String {
        val pss = ParameterizedSparqlString(
            """
        PREFIX rml: <http://w3id.org/rml/>
        INSERT { ?x rml:termType rml:BlankNode }
        WHERE {
           [] ?mapType ?x .
           OPTIONAL { ?x rml:template ?a }
           OPTIONAL { ?x rml:reference ?b }
           OPTIONAL { ?x rml:constant ?c }
           OPTIONAL { ?x rml:functionExecution ?d }
           FILTER(!BOUND(?a) && !BOUND(?b) && !BOUND(?c) && !BOUND(?d))
        }
        """
        )
        pss.setIri("mapType", mapType.uri)
        return pss.toString()
    }
}

class GatherMapMapper : rdfobjectloader.RdfModelMapper<GatherMap> {
    override fun map(
        dataset: rdf.DatasetCore,
        resource: rdf.Term,
        loader: rdfobjectloader.RdfObjectLoader,
        cache: MutableMap<rdf.Term, Any>
    ): GatherMap {
        if (cache.containsKey(resource)) {
            @Suppress("UNCHECKED_CAST")
            return cache[resource] as GatherMap
        }

        val instance = GatherMap()
        cache[resource] = instance

        // Find the parent TermMap resource that points to this RDF List via rml:gather
        val parentQuads = dataset.match(subject = null, predicate = rdfkt.NamedTerm("http://w3id.org/rml/gather"), `object` = resource).toList()
        val parentResource = parentQuads.firstOrNull()?.subject ?: resource

        // Map allowEmptyListAndContainer from parent
        val allowEmptyQuads = dataset.match(subject = parentResource, predicate = rdfkt.NamedTerm("http://w3id.org/rml/allowEmptyListAndContainer")).toList()
        if (allowEmptyQuads.isNotEmpty()) {
            instance.allowEmptyListAndContainer = allowEmptyQuads.first().`object`.value.toBoolean()
        }

        // Map gatherAs from parent
        val gatherAsQuads = dataset.match(subject = parentResource, predicate = rdfkt.NamedTerm("http://w3id.org/rml/gatherAs")).toList()
        if (gatherAsQuads.isNotEmpty()) {
            instance.gatherAs = gatherAsQuads.first().`object`
        }

        // Map strategy from parent
        val strategyQuads = dataset.match(subject = parentResource, predicate = rdfkt.NamedTerm("http://w3id.org/rml/strategy")).toList()
        if (strategyQuads.isNotEmpty()) {
            instance.strategy = strategyQuads.first().`object`
        }

        // Map gatherMaps (this is the RDF List itself, so it's matched on the current resource)
        val gatherMapsValRawTerms = run {
            val hasFirst = dataset.match(subject = resource, predicate = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).firstOrNull() != null
            if (hasFirst) {
                val list = mutableListOf<rdf.Term>()
                var current = resource
                while (current.value != "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil") {
                    val first = dataset.match(subject = current, predicate = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#first")).firstOrNull()?.`object`
                    if (first != null) list.add(first)
                    val rest = dataset.match(subject = current, predicate = rdfkt.NamedTerm("http://www.w3.org/1999/02/22-rdf-syntax-ns#rest")).firstOrNull()?.`object`
                    current = rest ?: break
                }
                list
            } else {
                emptyList<rdf.Term>()
            }
        }
        val gatherMapsVal = gatherMapsValRawTerms.map { term -> loader.map(dataset, term, setOf(burp.model.TermGenerator::class)) }.toMutableList()
        instance.gatherMaps = gatherMapsVal

        return instance
    }
}
