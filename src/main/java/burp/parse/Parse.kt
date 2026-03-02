package burp.parse

import burp.Main
import burp.ls.LogicalSourceFactory
import burp.model.*
import burp.model.gathermap.GatherMapMixin
import burp.model.lv.*
import burp.reporting.Origin
import burp.reporting.RmlError
import burp.reporting.StatementPart
import burp.reporting.StatementParts
import burp.vocabularies.RER
import burp.vocabularies.RML
import org.apache.jena.rdf.model.*
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.lib.ShLib
import org.apache.jena.util.FileUtils
import turtleprov.parseTurtleFromFile
import java.nio.file.Path

class Parse {
    var triplesMaps: MutableMap<Resource?, TriplesMap>? = null
    var logicalViews: MutableMap<Resource?, LogicalView>? = null

    private var mappingDirectory: Path? = null
    private var mappingFile: Path? = null
    private var currentDirectory: Path? = null
    private var mapping: Model? = null

    @Throws(Exception::class)
    fun parseMappingFile(mappingPath: Path, currentDirectory: Path?): MutableList<TriplesMap> {
        this.mappingFile = mappingPath.toAbsolutePath().normalize()
        this.mappingDirectory = mappingFile!!.parent
        this.currentDirectory = currentDirectory

        triplesMaps = mutableMapOf()
        logicalViews = mutableMapOf()

        val guessType = RDFDataMgr.determineLang(mappingPath.toString(), null, null)

        if (guessType === Lang.TURTLE) {
            val dataset = parseTurtleFromFile(mappingPath.toFile())
            mapping = dataset.getDefaultModel()
        } else {
            mapping = RDFDataMgr.loadModel(mappingPath.toString())
        }

        // if(!isValid(mapping))
        // 	throw new RuntimeException("Mapping did not satisfy shapes.");

        // Replace rml:subject, rml:object, ... with constant expression maps
        normalizeConstants(mapping!!)

        // Look for the triples maps
        val list = mapping!!.listSubjectsWithProperty(RML.logicalSource).toList()

        // Process each triples map
        for (r in list) {
            val tm = triplesMaps!!.computeIfAbsent(r) { TriplesMap(it) }

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

        return triplesMaps!!.values.toMutableList()
    }

    private fun isValid(mapping: Model): Boolean {
        val core = ModelFactory.createDefaultModel()
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-core/core.ttl"), "urn:dummy", FileUtils.langTurtle)
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-cc/cc.ttl"), "urn:dummy", FileUtils.langTurtle)
        //core.read(Parse.class.getResourceAsStream("/shapes/rml-io/io.ttl"), "urn:dummy", FileUtils.langTurtle);
        //core.read(Parse.class.getResourceAsStream("/shapes/rml-fnml/fnml.ttl"), "urn:dummy", FileUtils.langTurtle);
        core.read(Parse::class.java.getResourceAsStream("/shapes/rml-lv/lv.ttl"), "urn:dummy", FileUtils.langTurtle)

        //core.read(Parse.class.getResourceAsStream("/shapes/rml-star/star.ttl"), "urn:dummy", FileUtils.langTurtle);
        val report = ShaclValidator.get().validate(core.getGraph(), mapping.getGraph())
        if (!report.conforms()) {
            ShLib.printReport(report)
            System.err.println(report)
            return false
        }

        return true
    }

    private fun createMapWithProperty(mapping: Model, sourceProp: Property, targetMapProp: Property, valueProp: Property) {
        val stmts = mapping.listStatements(null, sourceProp, null as RDFNode?)
        val toAdd = mutableListOf<Statement>()
        stmts.forEach { stmt ->
            val bnode = mapping.createResource()
            toAdd.add(mapping.createStatement(stmt.subject, targetMapProp, bnode))
            toAdd.add(mapping.createStatement(bnode, valueProp, stmt.`object`))
        }
        mapping.add(toAdd)
    }

    private fun normalizeConstants(mapping: Model) {


        createMapWithProperty(mapping, RML.subject, RML.subjectMap, RML.constant)
        createMapWithProperty(mapping, RML.`object`, RML.objectMap, RML.constant)
        createMapWithProperty(mapping, RML.predicate, RML.predicateMap, RML.constant)
        createMapWithProperty(mapping, RML.graph, RML.graphMap, RML.constant)

        createMapWithProperty(mapping, RML.language, RML.languageMap, RML.constant)
        createMapWithProperty(mapping, RML.datatype, RML.datatypeMap, RML.constant)

        createMapWithProperty(mapping, RML.child, RML.childMap, RML.reference)
        createMapWithProperty(mapping, RML.parent, RML.parentMap, RML.reference)

        createMapWithProperty(mapping, RML.return_, RML.returnMap, RML.constant)
        createMapWithProperty(mapping, RML.function, RML.functionMap, RML.constant)
        createMapWithProperty(mapping, RML.parameter, RML.parameterMap, RML.constant)
        createMapWithProperty(mapping, RML.inputValue, RML.inputValueMap, RML.constant)

        val toAdd = mutableListOf<Statement>()
        mapping.listStatements(null, RML.constant, null as RDFNode?).forEach { stmt ->
            val node = stmt.`object`
            val termType = if (node.isLiteral) RML.LITERAL else if (node.isURIResource) RML.IRI else RML.BLANKNODE
            toAdd.add(mapping.createStatement(stmt.subject, RML.termType, termType))
        }

        listOf(RML.subjectMap, RML.graphMap, RML.objectMap).forEach { mapProp ->
            mapping.listStatements(null, mapProp, null as RDFNode?).forEach { stmt ->
                val x = stmt.`object`
                if (x.isResource) {
                    val r = x.asResource()
                    if (!r.hasProperty(RML.template) &&
                        !r.hasProperty(RML.reference) &&
                        !r.hasProperty(RML.constant) &&
                        !r.hasProperty(RML.functionExecution)
                    ) {
                        toAdd.add(mapping.createStatement(r, RML.termType, RML.BLANKNODE))
                    }
                }
            }
        }
        mapping.add(toAdd)
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
            val lv = logicalViews!!.computeIfAbsent(ls) { LogicalView() }

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
        return prepareViewJoin(false, resource)
    }

    private fun prepareInnerJoin(resource: Resource): ViewJoin {
        return prepareViewJoin(true, resource)
    }

    private fun prepareViewJoin(isInnerJoin: Boolean, resource: Resource): ViewJoin {
        val viewJoin = ViewJoin()
        viewJoin.isInnerJoin = isInnerJoin

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

    private fun prepareGraphMap(r: Resource) = prepareTermMap(r, GraphMap())

    private fun preparePredicateMap(pm: Resource) = prepareExpression(pm, PredicateMap())

    private fun <TM : TermMap> prepareTermMap(tmRdf: Resource, tm: TM): TM {
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

    private fun prepareObjectMap(om: Resource): ObjectMap {
        val objectMap = prepareTermMap(om, ObjectMap())


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

    private fun prepareGatherMap(gm: Resource): GatherMapMixin {
        val gatherMap = GatherMapMixin()

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

    private fun prepareDatatypeMap(dtm: Resource) = prepareExpression(dtm, DatatypeMap())

    private fun prepareLanguageMap(lam: Resource): LanguageMap {
        val x = prepareExpression(lam, LanguageMap())
        return x
    }

    private fun prepareField(p: Resource): Field {
        val (e, _) = prepareExpression(p)
        var field: Field? = null
        if (e == null) {
            // Create IterableField
            val f = IterableField()

            if (p.hasProperty(RML.iterator)) f.iterator =
                p.getProperty(RML.iterator).getObject().asLiteral().getString()

            if (p.hasProperty(RML.referenceFormulation)) f.referenceFormulation =
                p.getProperty(RML.referenceFormulation).getObject().asResource()

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
        referencingObjectMap.parent = triplesMaps!!.computeIfAbsent(p) { TriplesMap(it) }

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
            return RDFNodeConstant(constant) to
                    Origin(r.getProperty(RML.constant), StatementPart.Object)

        }

        if (r.hasProperty(RML.reference)) {
            val reference = r.getProperty(RML.reference).getObject().asLiteral().getString()
            return Reference(reference) to Origin(r.getProperty(RML.reference), StatementPart.Object)

        }

        if (r.hasProperty(RML.template)) {
            val template = r.getProperty(RML.template).getObject().asLiteral().getString()
            return Template(template, r.getProperty(RML.template)) to Origin(
                r.getProperty(RML.template),
                StatementPart.Object
            )

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

            return fe to Origin(r.getProperty(RML.functionExecution), StatementPart.Object)
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

    private fun prepareInputValueMap(om: Resource): InputValueMap {
        val im = prepareExpression(om, InputValueMap())

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
        if (r.hasProperty(RML.constant)) return false
        if (r.hasProperty(RML.reference)) return false
        if (r.hasProperty(RML.template)) return false
        return !r.hasProperty(RML.functionExecution)
    }
}
