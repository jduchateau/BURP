package burp.parse

import burp.ls.LogicalSourceFactory
import burp.model.AbstractLogicalSource
import burp.model.ConcreteExpressionMap
import burp.model.JoinCondition
import burp.model.lv.*
import org.apache.jena.rdf.model.AnonId
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import rdf.DatasetCore
import rdf.Term
import rdfkt.JenaBlankNode
import rdfkt.JenaNamedNode
import rdfkt.toTerm
import rdfobjectloader.RdfModelMapper
import rdfobjectloader.RdfObjectLoader
import rdfobjectloader.StatementPart
import java.nio.file.Path

fun resolveResource(resource: Term, mapping: Model): Resource {
    return when (resource) {
        is JenaNamedNode -> mapping.getResource(resource.value)
        is JenaBlankNode -> mapping.createResource(AnonId(resource.value))
        else -> throw IllegalArgumentException("Cannot extract resource: $resource")
    }
}

class AbstractLogicalSourceMapper(
    private val mapping: Model,
    private val mappingDirectory: Path,
    private val currentDirectory: Path
) : RdfModelMapper<AbstractLogicalSource> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): AbstractLogicalSource {
        val jenaResource = resolveResource(resource, mapping)
        val viewOnProp = ResourceFactory.createProperty("http://w3id.org/rml/viewOn")
        if (jenaResource.hasProperty(viewOnProp)) {
            return loader.map(dataset, resource, setOf(LogicalView::class))
        }
        val source = LogicalSourceFactory.create(jenaResource, mappingDirectory, currentDirectory)
        cache[resource] = source
        return source
    }
}

class LogicalViewMapper(private val mapping: Model) : RdfModelMapper<LogicalView> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): LogicalView {
        val jenaResource = resolveResource(resource, mapping)
        val view = LogicalView()
        cache[resource] = view

        val viewOnProp = ResourceFactory.createProperty("http://w3id.org/rml/viewOn")
        val viewOnResource = jenaResource.getPropertyResourceValue(viewOnProp)
            ?: throw IllegalArgumentException("LogicalView has no rml:viewOn")
        
        view.logicalSource = loader.map(dataset, JenaNamedNode(viewOnResource), setOf(AbstractLogicalSource::class))

        val fieldProp = ResourceFactory.createProperty("http://w3id.org/rml/field")
        jenaResource.listProperties(fieldProp).forEach { stmt ->
            val fieldRes = stmt.`object`.asResource()
            val field = loader.map(dataset, JenaNamedNode(fieldRes), setOf(Field::class))
            view.addField(field)
        }

        val leftJoinProp = ResourceFactory.createProperty("http://w3id.org/rml/leftJoin")
        jenaResource.listProperties(leftJoinProp).forEach { stmt ->
            val joinRes = stmt.`object`.asResource()
            val join = loader.map(dataset, JenaNamedNode(joinRes), setOf(ViewJoin::class))
            join.joinType = JoinType.LEFT
            view.addJoin(join)
        }

        val innerJoinProp = ResourceFactory.createProperty("http://w3id.org/rml/innerJoin")
        jenaResource.listProperties(innerJoinProp).forEach { stmt ->
            val joinRes = stmt.`object`.asResource()
            val join = loader.map(dataset, JenaNamedNode(joinRes), setOf(ViewJoin::class))
            join.joinType = JoinType.INNER
            view.addJoin(join)
        }

        return view
    }
}

class ViewJoinMapper(private val mapping: Model) : RdfModelMapper<ViewJoin> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): ViewJoin {
        val jenaResource = resolveResource(resource, mapping)
        val viewJoin = ViewJoin()
        cache[resource] = viewJoin

        val parentLogicalViewProp = ResourceFactory.createProperty("http://w3id.org/rml/parentLogicalView")
        val plvRes = jenaResource.getRequiredProperty(parentLogicalViewProp).`object`.asResource()
        viewJoin.parentLogicalView = loader.map(dataset, JenaNamedNode(plvRes), setOf(LogicalView::class))

        val joinConditionProp = ResourceFactory.createProperty("http://w3id.org/rml/joinCondition")
        jenaResource.listProperties(joinConditionProp).forEach { stmt ->
            val jcRes = stmt.`object`.asResource()
            val jc = loader.map(dataset, JenaNamedNode(jcRes), setOf(JoinCondition::class))
            viewJoin.joinConditions.add(jc)
        }

        val fieldProp = ResourceFactory.createProperty("http://w3id.org/rml/field")
        jenaResource.listProperties(fieldProp).forEach { stmt ->
            val fieldRes = stmt.`object`.asResource()
            val field = loader.map(dataset, JenaNamedNode(fieldRes), setOf(Field::class))
            viewJoin.addField(field)
        }

        return viewJoin
    }
}

class ExpressionFieldMapper(private val mapping: Model) : RdfModelMapper<ExpressionField> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): ExpressionField {
        val jenaResource = resolveResource(resource, mapping)
        val f = ExpressionField()
        cache[resource] = f

        val fieldNameProp = ResourceFactory.createProperty("http://w3id.org/rml/fieldName")
        f.fieldName = jenaResource.getRequiredProperty(fieldNameProp).`object`.asLiteral().string

        val fem = loader.map(dataset, resource, setOf(ConcreteExpressionMap::class))
        f.fieldExpressionMap = fem

        val fieldProp = ResourceFactory.createProperty("http://w3id.org/rml/field")
        jenaResource.listProperties(fieldProp).forEach { stmt ->
            val subFieldRes = stmt.`object`.asResource()
            val subField = loader.map(dataset, JenaNamedNode(subFieldRes), setOf(Field::class))
            f.addField(subField)
        }

        return f
    }
}

class IterableFieldMapper(private val mapping: Model) : RdfModelMapper<IterableField> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): IterableField {
        val jenaResource = resolveResource(resource, mapping)
        val f = IterableField()
        cache[resource] = f

        val fieldNameProp = ResourceFactory.createProperty("http://w3id.org/rml/fieldName")
        f.fieldName = jenaResource.getRequiredProperty(fieldNameProp).`object`.asLiteral().string

        val iteratorProp = ResourceFactory.createProperty("http://w3id.org/rml/iterator")
        if (jenaResource.hasProperty(iteratorProp)) {
            f.iterator = jenaResource.getProperty(iteratorProp).`object`.asLiteral().string
        }

        val refFormulationProp = ResourceFactory.createProperty("http://w3id.org/rml/referenceFormulation")
        if (jenaResource.hasProperty(refFormulationProp)) {
            val stmt = jenaResource.getProperty(refFormulationProp)
            f.declaredReferenceFormulation = stmt.`object`.toTerm()
            f.declaredReferenceFormulationOrigin = burp.reporting.Origin(stmt, StatementPart.Object)
        }

        val fieldProp = ResourceFactory.createProperty("http://w3id.org/rml/field")
        jenaResource.listProperties(fieldProp).forEach { stmt ->
            val subFieldRes = stmt.`object`.asResource()
            val subField = loader.map(dataset, JenaNamedNode(subFieldRes), setOf(Field::class))
            f.addField(subField)
        }

        return f
    }
}

class FieldMapper(private val mapping: Model) : RdfModelMapper<Field> {
    override fun map(
        dataset: DatasetCore,
        resource: Term,
        loader: RdfObjectLoader,
        cache: MutableMap<Term, Any>
    ): Field {
        val jenaResource = resolveResource(resource, mapping)
        val hasExpression = listOf(
            "http://w3id.org/rml/constant",
            "http://w3id.org/rml/reference",
            "http://w3id.org/rml/template",
            "http://w3id.org/rml/functionExecution"
        ).any { uri ->
            jenaResource.hasProperty(ResourceFactory.createProperty(uri))
        }

        return if (hasExpression) {
            loader.map(dataset, resource, setOf(ExpressionField::class))
        } else {
            loader.map(dataset, resource, setOf(IterableField::class))
        }
    }
}
