package burp.parse

import burp.Main
import burp.ls.LogicalSourceFactory
import burp.model.*
import burp.model.gathermap.GatherMap
import burp.model.lv.*
import burp.reporting.*
import burp.vocabularies.RER
import burp.vocabularies.RML
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
import turtleprov.parseTurtleFromFile
import java.nio.file.Path


class Parse {
    val triplesMaps: MutableMap<Resource?, TriplesMap> = mutableMapOf()
    val logicalViews: MutableMap<Resource?, LogicalView> = mutableMapOf()

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

        // Replace rml:subject, rml:object, ... with constant expression maps
        normalizeConstantsUpdate(mapping!!)

        // Look for the triples maps
        val list = mapping!!.listSubjectsWithProperty(RML.logicalSource).toList()

        // Process each triples map
        for (r in list) {
            val tm = triplesMaps.computeIfAbsent(r) { TriplesMap(it) }

            val ls = r.getPropertyResourceValue(RML.logicalSource)
            val lsStmt = r.getProperty(RML.logicalSource)
            tm.logicalSource = prepareLogicalSource(ls)

            val subjectMapList = r.listProperties(RML.subjectMap).toList()
            if (subjectMapList.isEmpty()) {
                Main.report.errors.add(
                    RmlError(
                        "No subject maps in $tm",
                        Origin(lsStmt, StatementPart.Subject),
                        RER.MappingError,
                        null
                    )
                )
                continue
            }
            if (subjectMapList.size > 1) {
                val originStatements = subjectMapList.stream()
                    .map { stmt: Statement? -> StatementParts.fromPredicateObject(stmt!!) }
                    .toList()
                Main.report.errors.add(
                    RmlError(
                        "Multiple subject maps in $tm",
                        Origin(null, originStatements),
                        RER.MappingError,
                        null
                    )
                )
            }
            val sm = r.getPropertyResourceValue(RML.subjectMap)
            tm.subjectMap = prepareSubjectMap(sm)

            if (r.hasProperty(RML.baseIRI)) tm.baseIRI = r.getPropertyResourceValue(RML.baseIRI).getURI()

            r.listProperties(RML.predicateObjectMap).forEach { s: Statement ->
                val pom = preparePredicateObjectMap(s.getObject().asResource())
                tm.predicateObjectMaps.add(pom)
            }
        }

        return triplesMaps.values.toMutableList()
    }

    private fun isValid(mapping: Model): Boolean {
        val core = ModelFactory.createDefaultModel()
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-core/core.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-cc/cc.ttl"), "urn:dummy", FileUtils.langTurtle)
        //core.read(Parse.class.getResourceAsStream("/shapes/rml-io/io.ttl"), "urn:dummy", FileUtils.langTurtle);
        //core.read(Parse.class.getResourceAsStream("/shapes/rml-fnml/fnml.ttl"), "urn:dummy", FileUtils.langTurtle);
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-lv/lv.ttl"), "urn:dummy", FileUtils.langTurtle)
        //core.read(Parse.class.getResourceAsStream("/shapes/rml-star/star.ttl"), "urn:dummy", FileUtils.langTurtle);

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
            conditionShortcutExpand,
            expandShortcut(RML.subjectMap, RML.constant, RML.subject),
            expandShortcut(RML.objectMap, RML.constant, RML.`object`),
            expandShortcut(RML.predicateMap, RML.constant, RML.predicate),
            expandShortcut(RML.graphMap, RML.constant, RML.graph),

            expandShortcut(RML.languageMap, RML.constant, RML.language),
            expandShortcut(RML.datatypeMap, RML.constant, RML.datatype),

            expandShortcut(RML.childMap, RML.reference, RML.child),
            expandShortcut(RML.parentMap, RML.reference, RML.parent),

            expandShortcut(RML.returnMap, RML.constant, RML.return_),
            expandShortcut(RML.functionMap, RML.constant, RML.function),
            expandShortcut(RML.parameterMap, RML.constant, RML.parameter),
            expandShortcut(RML.inputValueMap, RML.constant, RML.inputValue),
            constructTermTypes,
            constructImplicitTermTypeQuery(RML.subjectMap),
            constructImplicitTermTypeQuery(RML.graphMap),
            constructImplicitTermTypeQuery(RML.objectMap)
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

    @Throws(Exception::class)
    private fun prepareLogicalSource(ls: Resource): AbstractLogicalSource {
        // This is RML-LV
        if (ls.hasProperty(RML.viewOn)) {
            return prepareLogicalView(ls)
        }

        return LogicalSourceFactory.create(ls, mappingDirectory!!, currentDirectory!!)
    }

    private fun prepareLogicalView(ls: Resource): LogicalView {
        try {
            val view = ls.getPropertyResourceValue(RML.viewOn)
            val lv = logicalViews.computeIfAbsent(ls) { LogicalView() }

            lv.logicalSource = prepareLogicalSource(view)

            ls.listProperties(RML.field).forEach { s: Statement ->
                lv.addField(prepareField(s.getObject().asResource()))
            }

            ls.listProperties(RML.leftJoin).forEach { s: Statement ->
                lv.addJoin(prepareLeftJoin(s.getObject().asResource()))
            }

            ls.listProperties(RML.innerJoin).forEach { s: Statement ->
                lv.addJoin(prepareInnerJoin(s.getObject().asResource()))
            }

            return lv
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun prepareLeftJoin(resource: Resource): ViewJoin {
        return prepareViewJoin(JoinType.LEFT, resource)
    }

    private fun prepareInnerJoin(resource: Resource): ViewJoin {
        return prepareViewJoin(JoinType.INNER, resource)
    }

    private fun prepareViewJoin(joinType: JoinType, resource: Resource): ViewJoin {
        val viewJoin = ViewJoin()
        viewJoin.joinType = joinType

        val plv = resource.getRequiredProperty(RML.parentLogicalView).getObject().asResource()
        viewJoin.parentLogicalView = prepareLogicalView(plv)

        viewJoin.joinConditions =
            resource.listProperties(RML.joinCondition).mapWith { prepareJoinCondition(it) }.toList()

        // We need the fields on the Logical View Join
        resource.listProperties(RML.field).forEach { s: Statement ->
            viewJoin.addField(prepareField(s.getObject().asResource()))
        }

        return viewJoin
    }

    private fun prepareSubjectMap(sm: Resource): SubjectMap {
        val subjectMap = prepareExpression(sm, SubjectMap())

        sm.listProperties(RML.clazz).forEach { s: Statement ->
            subjectMap.classes.add(s.getObject().asResource())
        }

        sm.listProperties(RML.graphMap).forEach { s: Statement ->
            val gm = prepareGraphMap(s.getObject().asResource())
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

        pom.listProperties(RML.graphMap).forEach { s: Statement ->
            val gm = prepareGraphMap(s.getObject().asResource())
            predicateObjectMap.graphMaps.add(gm)
        }

        pom.listProperties(RML.predicateMap).forEach { s: Statement ->
            val pm = preparePredicateMap(s.getObject().asResource())
            predicateObjectMap.predicateMaps.add(pm)
        }

        pom.listProperties(RML.objectMap).forEach { s: Statement ->
            if (s.getObject().asResource().getProperty(RML.parentTriplesMap) == null) {
                val om = prepareObjectMap(s.getObject().asResource())
                predicateObjectMap.objectMaps.add(om)
            } else {
                val rom = prepareReferencingObjectMap(s.getObject().asResource())
                predicateObjectMap.refObjectMaps.add(rom)
            }
        }

        return predicateObjectMap
    }

    private fun prepareGraphMap(r: Resource) = prepareTermMapMinimal(r, GraphMap())

    private fun preparePredicateMap(pm: Resource) = prepareExpression(pm, PredicateMap())

    private fun <TM : TermMap> prepareTermMapMinimal(tmRdf: Resource, tm: TM): TM {
        val (expr, origin) = prepareExpression(tmRdf)
        tm.expression = expr
        tm.expressionOrigin = origin

        val termType = tmRdf.getPropertyResourceValue(RML.termType)
        if (termType != null)  // PROVIDE THE TERM TYPE THAT IS GIVEN
            tm.termType = termType
        else if (hasNoTemplateReferenceConstantOrFunction(tmRdf)) {
            // IF NO REFERENCE, TEMPLATE, CONSTANT,
            // OR FUNCTION THEN WE GENERATE BLANK NODES (BASED ON THE ITERATION)
            tm.termType = RML.BLANKNODE
        }

        return tm
    }

    private fun prepareObjectMap(om: Resource): ObjectMap = prepareTermMapFull(om, ObjectMap())

    private fun <TM : TermMap> prepareTermMapFull(om: Resource, termMap: TM): TM {
        val objectMap = prepareTermMapMinimal(om, termMap)


        val lam = om.getPropertyResourceValue(RML.languageMap)
        if (lam != null) objectMap.languageMap = prepareLanguageMap(lam)

        val dtm = om.getPropertyResourceValue(RML.datatypeMap)
        if (dtm != null) objectMap.datatypeMap = prepareDatatypeMap(dtm)

        val termType = om.getPropertyResourceValue(RML.termType)
        if (termType == null && (lam != null || dtm != null || objectMap.expression is Reference || objectMap.expression is FunctionExecution)) {
            objectMap.termType = RML.LITERAL
        }

        val gm = om.getPropertyResourceValue(RML.gather)
        if (gm != null) {
            // This object map has a gather, so we process it as a gather map
            objectMap.gatherMap = prepareGatherMap(om)
        }

        return objectMap
    }

    private fun prepareGatherMap(gm: Resource): GatherMap {
        val gatherMap = GatherMap()

        if (gm.hasProperty(RML.allowEmptyListAndContainer)) {
            gatherMap.allowEmptyListAndContainer =
                gm.getProperty(RML.allowEmptyListAndContainer).getObject().asLiteral().getBoolean()
        }

        if (gm.hasProperty(RML.gatherAs)) {
            gatherMap.gatherAs = gm.getPropertyResourceValue(RML.gatherAs)
        }

        if (gm.hasProperty(RML.strategy)) {
            gatherMap.strategy = gm.getPropertyResourceValue(RML.strategy)
        }

        val list = gm.getPropertyResourceValue(RML.gather).`as`(RDFList::class.java)
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

    private fun prepareDatatypeMap(dtm: Resource) = prepareExpression(dtm, DatatypeMap())

    private fun prepareLanguageMap(lam: Resource): LanguageMap {
        val x = prepareExpression(lam, LanguageMap())
        return x
    }

    private fun prepareField(p: Resource): Field {
        val (e, _) = prepareExpression(p)
        var field: Field
        if (e == null) {
            // Create IterableField
            val f = IterableField()

            if (p.hasProperty(RML.iterator)) f.iterator =
                p.getProperty(RML.iterator).getObject().asLiteral().getString()

            if (p.hasProperty(RML.referenceFormulation)) {
                val stmt = p.getProperty(RML.referenceFormulation)
                f.declaredReferenceFormulation = stmt.getObject().asResource()
                f.declaredReferenceFormulationOrigin = Origin(stmt, StatementPart.Object)
            }

            field = f
        } else {
            val f = ExpressionField()
            val fem = ConcreteExpressionMap()
            prepareExpression(p, fem)
            f.fieldExpressionMap = fem
            field = f
        }

        field.fieldName = p.getRequiredProperty(RML.fieldName).getObject().asLiteral().getString()

        // Add the subfields
        val finalField: Field = field
        p.listProperties(RML.field).forEach { s ->
            finalField.addField(prepareField(s.getObject().asResource()))
        }

        return finalField
    }

    private fun prepareJoinCondition(joinConditionStmt: Statement): JoinCondition {
        val jc = JoinCondition()
        val jcr = joinConditionStmt.getObject().asResource()

        var r = jcr.getPropertyResourceValue(RML.parentMap)
        if (r != null) jc.parentMap = prepareExpressionMap(r)

        r = jcr.getPropertyResourceValue(RML.childMap)
        if (r != null) jc.childMap = prepareExpressionMap(r)
        return jc
    }

    private fun prepareReferencingObjectMap(rom: Resource): ReferencingObjectMap {
        val referencingObjectMap = ReferencingObjectMap()

        val p = rom.getPropertyResourceValue(RML.parentTriplesMap)
        referencingObjectMap.parentTriplesMap = triplesMaps.computeIfAbsent(p) { TriplesMap(it) }

        referencingObjectMap.joinConditions =
            rom.listProperties(RML.joinCondition).mapWith { prepareJoinCondition(it) }.toList()

        return referencingObjectMap
    }

    private fun prepareExpressionMap(em: Resource): ConcreteExpressionMap =
        prepareExpression(em, ConcreteExpressionMap())

    private fun <EM : ExpressionMap> prepareExpression(r: Resource, em: EM): EM {
        val (expr, origin) = prepareExpression(r)
        em.expression = expr
        em.expressionOrigin = origin
        return em
    }

    private fun prepareExpression(r: Resource): Pair<Expression?, Origin?> {
        if (r.hasProperty(RML.constant)) {
            val constant = r.getProperty(RML.constant).getObject()
            val term = when {
                constant.isURIResource -> IRITerm(constant.asResource().uri)
                constant.isLiteral -> {
                    val lit = constant.asLiteral()
                    val dt = if (lit.datatypeURI != null) IRITerm(lit.datatypeURI) else null
                    val lang = if (lit.language != null && lit.language.isNotEmpty()) lit.language else null
                    LiteralTerm(lit.lexicalForm, datatype = dt, language = lang)
                }

                else -> BlankNodeTerm(constant.asResource().id.labelString)
            }
            return RDFNodeConstant(term) to
                    Origin(r.getProperty(RML.constant), StatementPart.Object)
        }

        if (r.hasProperty(RML.reference)) {
            val reference = r.getProperty(RML.reference).getObject().asLiteral().getString()
            val origin = Origin(r.getProperty(RML.reference), StatementPart.Object)
            return RawReference(reference, origin) to origin

        }

        if (r.hasProperty(RML.template)) {
            val template = r.getProperty(RML.template).getObject().asLiteral().getString()
            val origin = Origin(r.getProperty(RML.template), StatementPart.Object)
            return Template(template, r.getProperty(RML.template)) to origin

        }

        if (r.hasProperty(RML.functionExecution)) {
            val fe = FunctionExecution()

            val feStmt = r.getProperty(RML.functionExecution)
            val fer = feStmt.resource
            fe.callStmt = StatementParts.from(feStmt, StatementPart.Object)

            fe.functionMap = prepareFunctionMap(fer.getPropertyResourceValue(RML.functionMap))
            fe.functionMapStmt = StatementParts.fromPredicateObject(fer.getProperty(RML.functionMap))

            // Return Maps are siblings of Function Execution Maps
            if (r.hasProperty(RML.returnMap)) {
                fe.returnMap = prepareReturnMap(r.getPropertyResourceValue(RML.returnMap))
                fe.returnMapStmt = StatementParts.fromPredicateObject(r.getProperty(RML.returnMap))
            }

            val inputStmts = fer.listProperties(RML.input).toList()
            fe.inputs.addAll(inputStmts.map { prepareInput(it.resource) })
            fe.inputsStmt = inputStmts.map { StatementParts.from(it, StatementPart.Object) }

            return fe to Origin(feStmt, StatementPart.Object)
        }

        return null to null
    }

    private fun prepareInput(r: Resource): Input {
        val input = Input()

        val pm = prepareExpression(r.getPropertyResourceValue(RML.parameterMap), ParameterMap())
        input.parameterMap = pm

        input.inputValueMap = prepareInputValueMap(r.getPropertyResourceValue(RML.inputValueMap))

        return input
    }

    private fun prepareFunctionMap(r: Resource) = prepareExpression(r, FunctionMap())

    private fun prepareReturnMap(r: Resource): ReturnMap {
        val rm = ReturnMap()
        prepareExpression(r, rm)
        return rm
    }

    private fun prepareInputValueMap(om: Resource): InputValueMap = prepareTermMapFull(om, InputValueMap())

    private fun hasNoTemplateReferenceConstantOrFunction(r: Resource): Boolean {
        if (r.hasProperty(RML.constant)) return false
        if (r.hasProperty(RML.reference)) return false
        if (r.hasProperty(RML.template)) return false
        return !r.hasProperty(RML.functionExecution)
    }

    /**
     * Attempt to identify the Statements Parts causing the SHACL Error from a SHACL ReportEntry
     */
    fun extractStatementsFromShaclViolation(vr: ReportEntry, mapping: Model): List<StatementParts> {
        val results = mutableListOf<StatementParts>()

        val focusNode = vr.focusNode()
        val resultPath = vr.resultPath()
        val value = vr.value()

        if (focusNode == null) return results

        // Check if the focus node is URI or blank
        val isURI = focusNode.isURI
        val isBlank = focusNode.isBlank

        if (!isURI && !isBlank) return results

        // Get the resource
        val focusResource = if (isURI) mapping.getResource(focusNode.toString()) else null
        // If it is a BlankNode we probably don't have the right blank node identifier

        val valueNode: RDFNode? = if (value != null && value.isConcrete) {
            when {
                value.isURI -> mapping.getResource(value.toString())
                value.isBlank -> mapping.getResource(value.toString())
                value.isLiteral -> mapping.createTypedLiteral(value.literalLexicalForm, value.literalDatatype)
                else -> null
            }
        } else null

        // If we have a path, use it to find the specific statements
        if (resultPath != null) {
            when (resultPath) {
                is P_Path0 -> {
                    // It's a simple predicate path
                    val predicate = mapping.getProperty(resultPath.node.uri)
                    // If we have the exact value node that failed, we find that specific statement
                    if (valueNode != null) {
                        mapping.listStatements(focusResource, predicate, valueNode).forEach { stmt ->
                            results.add(
                                StatementParts.from(
                                    stmt,
                                    StatementPart.Subject,
                                    StatementPart.Predicate,
                                    StatementPart.Object
                                )
                            )
                        }
                    } else {
                        // If value is null (e.g. minCount violation), the statements causing this are any existing ones for this property
                        mapping.listStatements(focusResource, predicate, null as RDFNode?).forEach { stmt ->
                            results.add(
                                StatementParts.from(
                                    stmt,
                                    StatementPart.Subject,
                                    StatementPart.Predicate,
                                    StatementPart.Object
                                )
                            )
                        }
                    }
                }

                is P_Path1 -> {
                    // Inverse paths, ZeroOrMore, OneOrMore, ZeroOrOne
                    // Simplified: if it's an inverse path, we look for (valueNode, predicate, focusResource)
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
                                        stmt,
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
            // Try as subject
            mapping.listStatements(focusResource, null, valueNode).forEach { stmt ->
                results.add(StatementParts.from(stmt, StatementPart.Subject))
            }

            // Try focusNode as object
            mapping.listStatements(null, null, focusResource).forEach { stmt ->
                if (!results.any { it.stmt == stmt }) {
                    results.add(StatementParts.from(stmt, StatementPart.Object))
                }
            }
        }

        return results
    }
}
