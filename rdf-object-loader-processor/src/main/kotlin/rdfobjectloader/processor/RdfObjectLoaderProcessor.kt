package rdfobjectloader.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import rdfobjectloader.annotations.*
import java.io.OutputStreamWriter
import kotlin.reflect.KClass

class RdfObjectLoaderProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val processedClasses = mutableListOf<KSClassDeclaration>()
    private val processedClassNames = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = getAllClassDeclarations(resolver)
            .filter { isRdfMappedClass(it) }
            .filter { it.qualifiedName?.asString() !in processedClassNames }
            .toList()

        if (symbols.isEmpty()) return emptyList()

        for (symbol in symbols) {
            symbol.qualifiedName?.asString()?.let { processedClassNames.add(it) }
            generateMapperForClass(symbol)
            processedClasses.add(symbol)
        }

        generateRegistry()
        return emptyList()
    }

    private fun getAllClassDeclarations(resolver: Resolver): List<KSClassDeclaration> {
        val classes = mutableListOf<KSClassDeclaration>()
        fun visit(decl: KSDeclaration) {
            if (decl is KSClassDeclaration) {
                classes.add(decl)
                decl.declarations.forEach { visit(it) }
            }
        }
        resolver.getAllFiles().forEach { file ->
            file.declarations.forEach { visit(it) }
        }
        return classes
    }

    private fun isRdfMappedClass(classDecl: KSClassDeclaration): Boolean {
        if (classDecl.classKind != ClassKind.CLASS) return false
        if (classDecl.modifiers.contains(Modifier.ABSTRACT)) return false

        val classAnnotations = setOf(RdfType::class, MappedByPredicate::class)
            .map { it.qualifiedName.toString() }.toSet()
        if (classDecl.annotations.any { it.annotationType.resolve().declaration.qualifiedName.toString() in classAnnotations }) {
            return true
        }

        val propAnnotations = setOf(
            RdfId::class,
            RdfProperty::class,
            RdfLiteral::class,
            RdfShortcutProperty::class,
            RdfMappedFrom::class,
            OriginOfProperty::class
        ).map { it.qualifiedName.toString() }.toSet()

        for (prop in classDecl.getAllProperties()) {
            if (prop.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() in propAnnotations }) {
                return true
            }
        }

        classDecl.primaryConstructor?.parameters?.forEach { param ->
            if (param.annotations.any { it.annotationType.resolve().declaration.qualifiedName?.asString() in propAnnotations }) {
                return true
            }
        }

        return false
    }

    private fun generateMapperForClass(classDecl: KSClassDeclaration) {
        val packageName = classDecl.packageName.asString()
        val className = classDecl.simpleName.asString()
        val mapperClassName = "${className}__Mapper"

        val file = codeGenerator.createNewFile(
            Dependencies(false, classDecl.containingFile!!),
            packageName,
            mapperClassName
        )

        OutputStreamWriter(file, Charsets.UTF_8).use { writer ->
            writer.write("package $packageName\n\n")
            writer.write("import rdf.DatasetCore\n")
            writer.write("import rdf.Term\n")
            writer.write("import rdfkt.NamedTerm\n")
            writer.write("import rdfkt.BlankTerm\n")
            writer.write("import rdfobjectloader.RdfModelMapper\n")
            writer.write("import rdfobjectloader.RdfObjectLoader\n")
            writer.write("import rdfobjectloader.StatementParts\n\n")

            writer.write("class $mapperClassName : RdfModelMapper<$className> {\n")
            writer.write("    override fun map(\n")
            writer.write("        dataset: DatasetCore,\n")
            writer.write("        resource: Term,\n")
            writer.write("        loader: RdfObjectLoader,\n")
            writer.write("        cache: MutableMap<Term, Any>\n")
            writer.write("    ): $className {\n")
            writer.write("        if (cache.containsKey(resource)) {\n")
            writer.write("            @Suppress(\"UNCHECKED_CAST\")\n")
            writer.write("            return cache[resource] as $className\n")
            writer.write("        }\n\n")

            // 1. Generate constructor parameter parsing
            val constructor = classDecl.primaryConstructor
            val parameters = constructor?.parameters ?: emptyList()

            for (param in parameters) {
                val paramName = param.name!!.asString()
                val paramType = param.type.resolve()
                val typeName = paramType.declaration.qualifiedName?.asString() ?: ""

                // Extract annotations
                val rdfIdAnn = getAnnotation(param, RdfId::class)
                val rdfPropertyAnn = getAnnotation(param, RdfProperty::class)
                val originOfPropAnn = getAnnotation(param, OriginOfProperty::class)
                val rdfLiteralAnn = getAnnotation(param, RdfLiteral::class)
                val rdfShortcutAnn = getAnnotation(param, RdfShortcutProperty::class)
                val rdfMappedFromAnn = getAnnotation(param, RdfMappedFrom::class)

                writer.write("        // Parse parameter: $paramName\n")

                if (rdfIdAnn != null) {
                    writer.write("        val $paramName = resource.value\n")
                } else if (rdfMappedFromAnn != null) {
                    generateMappedFromCode(writer, paramName, rdfMappedFromAnn, typeName, paramType)
                } else if (rdfShortcutAnn != null) {
                    val shortcutUri = getAnnotationArgument(rdfShortcutAnn, "uri") as String
                    val shortcutFor = getAnnotationArgument(rdfShortcutAnn, "shortcutFor") as String
                    val standardPropertyUri = rdfPropertyAnn?.let { getAnnotationArgument(it, "uri") as String } ?: ""

                    generateShortcutMappingCode(
                        writer,
                        paramName,
                        shortcutUri,
                        shortcutFor,
                        standardPropertyUri,
                        typeName,
                        paramType
                    )
                } else if (rdfPropertyAnn != null) {
                    val propertyUri = getAnnotationArgument(rdfPropertyAnn, "uri") as String
                    generatePropertyMappingCode(writer, paramName, propertyUri, typeName, paramType)
                } else if (originOfPropAnn != null) {
                    val propertyName = getAnnotationArgument(originOfPropAnn, "propertyName") as String
                    val propUri = findPropertyUriInClass(classDecl, propertyName)
                    writer.write("        val ${paramName}Quad = dataset.match(subject = resource, predicate = NamedTerm(\"$propUri\")).firstOrNull()\n")
                    if (paramType.isMarkedNullable) {
                        writer.write("        val $paramName = ${paramName}Quad?.let { StatementParts.fromObject(it) }\n")
                    } else {
                        writer.write("        val $paramName = if (${paramName}Quad != null) StatementParts.fromObject(${paramName}Quad) else throw IllegalArgumentException(\"Missing origin for property $propertyName\")\n")
                    }
                } else if (rdfLiteralAnn != null) {
                    writer.write("        val $paramName = resource.value\n")
                } else {
                    writer.write("        val $paramName = null\n")
                }
            }

            // 2. Instantiate class
            writer.write("\n        val instance = $className(")
            writer.write(parameters.joinToString(", ") { it.name!!.asString() })
            writer.write(")\n")
            writer.write("        cache[resource] = instance\n\n")

            // 3. Generate property injection for mutable fields
            val properties = classDecl.getAllProperties()
                .filter { it.isMutable }
                .toList()

            for (prop in properties) {
                val propName = prop.simpleName.asString()
                val propType = prop.type.resolve()
                val typeName = propType.declaration.qualifiedName?.asString() ?: ""

                val rdfPropertyAnn = getAnnotation(prop, RdfProperty::class)
                val originOfPropAnn = getAnnotation(prop, OriginOfProperty::class)
                val rdfShortcutAnn = getAnnotation(prop, RdfShortcutProperty::class)
                val rdfMappedFromAnn = getAnnotation(prop, RdfMappedFrom::class)

                writer.write("        // Map mutable property: $propName\n")

                if (rdfShortcutAnn != null) {
                    val shortcutUri = getAnnotationArgument(rdfShortcutAnn, "uri") as String
                    val shortcutFor = getAnnotationArgument(rdfShortcutAnn, "shortcutFor") as String
                    val standardPropertyUri = rdfPropertyAnn?.let { getAnnotationArgument(it, "uri") as String } ?: ""

                    generateShortcutMappingCode(
                        writer,
                        "${propName}Val",
                        shortcutUri,
                        shortcutFor,
                        standardPropertyUri,
                        typeName,
                        propType
                    )
                    writer.write("        instance.$propName = ${propName}Val\n")
                } else if (rdfMappedFromAnn != null) {
                    generateMappedFromCode(writer, "${propName}Val", rdfMappedFromAnn, typeName, propType)
                    writer.write("        instance.$propName = ${propName}Val\n")
                } else if (rdfPropertyAnn != null) {
                    val propertyUri = getAnnotationArgument(rdfPropertyAnn, "uri") as String
                    generatePropertyMappingCode(writer, "${propName}Val", propertyUri, typeName, propType)
                    writer.write("        instance.$propName = ${propName}Val\n")
                } else if (originOfPropAnn != null) {
                    val propertyName = getAnnotationArgument(originOfPropAnn, "propertyName") as String
                    val propUri = findPropertyUriInClass(classDecl, propertyName)
                    writer.write("        val ${propName}Quad = dataset.match(subject = resource, predicate = NamedTerm(\"$propUri\")).firstOrNull()\n")
                    if (propType.isMarkedNullable) {
                        writer.write("        instance.$propName = ${propName}Quad?.let { StatementParts.fromObject(it) }\n")
                    } else {
                        writer.write("        instance.$propName = if (${propName}Quad != null) StatementParts.fromObject(${propName}Quad) else throw IllegalArgumentException(\"Missing origin for property $propertyName\")\n")
                    }
                }
            }

            writer.write("\n        return instance\n")
            writer.write("    }\n")
            writer.write("}\n")
        }
    }

    private fun generatePropertyMappingCode(
        writer: java.io.Writer,
        name: String,
        uri: String,
        typeName: String,
        type: KSType
    ) {
        val isList = typeName == "kotlin.collections.List"

        if (isList) {
            val elementType = type.arguments.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""
            writer.write("        val ${name}Quads = dataset.match(subject = resource, predicate = NamedTerm(\"$uri\")).toList()\n")

            if (isPrimitive(elementTypeName)) {
                val castExpr = getPrimitiveCastExpr("it.`object`.value", elementTypeName)
                writer.write("        val $name = ${name}Quads.map { $castExpr }\n")
            } else {
                writer.write("        val $name = ${name}Quads.map { quad -> loader.map(dataset, quad.`object`, setOf($elementTypeName::class)) }\n")
            }
        } else {
            writer.write("        val ${name}Quads = dataset.match(subject = resource, predicate = NamedTerm(\"$uri\")).toList()\n")
            writer.write("        val ${name}Quad = ${name}Quads.firstOrNull()\n")

            if (isPrimitive(typeName)) {
                val castExpr = getPrimitiveCastExpr("${name}Quad.`object`.value", typeName)
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.let { $castExpr }\n")
                } else {
                    writer.write("        val $name = if (${name}Quad != null) $castExpr else throw IllegalArgumentException(\"Missing required property $uri\")\n")
                }
            } else {
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.`object`?.let { loader.map(dataset, it, setOf($typeName::class)) }\n")
                } else {
                    writer.write("        val $name = if (${name}Quad != null) loader.map(dataset, ${name}Quad.`object`, setOf($typeName::class)) else throw IllegalArgumentException(\"Missing required property $uri\")\n")
                }
            }
        }
    }

    private fun generateShortcutMappingCode(
        writer: java.io.Writer,
        name: String,
        shortcutUri: String,
        shortcutFor: String,
        standardPropertyUri: String,
        typeName: String,
        type: KSType
    ) {
        val targetClassDecl = type.declaration as? KSClassDeclaration
        val primaryConstructor = targetClassDecl?.primaryConstructor
        val singleParam = primaryConstructor?.parameters?.singleOrNull()
        val singleParamType = singleParam?.type?.resolve()
        val isSingleListParam = singleParamType?.declaration?.qualifiedName?.asString() == "kotlin.collections.List"

        if (isSingleListParam) {
            val listPropName = singleParam!!.name!!.asString()
            val elementType = singleParamType?.arguments?.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""

            writer.write("        val ${name}ShortcutQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$shortcutUri\")).toList()\n")
            if (standardPropertyUri.isNotEmpty()) {
                writer.write("        val ${name}ExpressionQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$standardPropertyUri\")).toList()\n")
                writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty() || ${name}ExpressionQuads.isNotEmpty()) {\n")
                writer.write("            val combinedList = mutableListOf<$elementTypeName>()\n")
                writer.write("            for (quad in ${name}ShortcutQuads) {\n")
                writer.write("                val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
                writer.write("                val virtualDataset = rdfkt.InMemoryDatasetCore()\n")
                writeDatasetCopyCode(writer)
                writer.write("                virtualDataset.add(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject))\n")
                writer.write("                val nested = loader.map(virtualDataset, virtualSubject, setOf($typeName::class))\n")
                writer.write("                combinedList.addAll(nested.$listPropName)\n")
                writer.write("            }\n")
                writer.write("            for (quad in ${name}ExpressionQuads) {\n")
                writer.write("                val nested = loader.map(dataset, quad.`object`, setOf($typeName::class))\n")
                writer.write("                combinedList.addAll(nested.$listPropName)\n")
                writer.write("            }\n")
                writer.write("            $typeName(combinedList)\n")
                writer.write("        } else {\n")
                writer.write("            null\n")
                writer.write("        }\n")
            } else {
                writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty()) {\n")
                writer.write("            val combinedList = mutableListOf<$elementTypeName>()\n")
                writer.write("            for (quad in ${name}ShortcutQuads) {\n")
                writer.write("                val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
                writer.write("                val virtualDataset = rdfkt.InMemoryDatasetCore()\n")
                writeDatasetCopyCode(writer)
                writer.write("                virtualDataset.add(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject))\n")
                writer.write("                val nested = loader.map(virtualDataset, virtualSubject, setOf($typeName::class))\n")
                writer.write("                combinedList.addAll(nested.$listPropName)\n")
                writer.write("            }\n")
                writer.write("            $typeName(combinedList)\n")
                writer.write("        } else {\n")
                writer.write("            null\n")
                writer.write("        }\n")
            }
        } else {
            writer.write("        val ${name}ShortcutQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$shortcutUri\")).toList()\n")
            writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty()) {\n")
            writer.write("            val quad = ${name}ShortcutQuads.first()\n")
            writer.write("            val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
            writer.write("            val virtualDataset = rdfkt.InMemoryDatasetCore()\n")
            writeDatasetCopyCode(writer)
            writer.write("            virtualDataset.add(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject))\n")
            writer.write("            loader.map(virtualDataset, virtualSubject, setOf($typeName::class))\n")
            writer.write("        } else {\n")
            if (standardPropertyUri.isNotEmpty()) {
                writer.write("            val ${name}ExpressionQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$standardPropertyUri\")).toList()\n")
                writer.write("            if (${name}ExpressionQuads.isNotEmpty()) {\n")
                writer.write("                val exprNode = ${name}ExpressionQuads.first().`object`\n")
                writer.write("                loader.map(dataset, exprNode, setOf($typeName::class))\n")
                writer.write("            } else {\n")
                writer.write("                null\n")
                writer.write("            }\n")
            } else {
                writer.write("            null\n")
            }
            writer.write("        }\n")
        }
    }

    private fun writeDatasetCopyCode(writer: java.io.Writer) {
        writer.write("                for (q in dataset) {\n")
        writer.write("                    val s = when (val term = q.subject) {\n")
        writer.write("                        is rdfkt.Term -> term\n")
        writer.write("                        is rdf.NamedNode -> rdfkt.NamedTerm(term.value)\n")
        writer.write("                        is rdf.BlankNode -> rdfkt.BlankTerm(term.value)\n")
        writer.write("                        else -> throw IllegalArgumentException()\n")
        writer.write("                    }\n")
        writer.write("                    val p = when (val term = q.predicate) {\n")
        writer.write("                        is rdfkt.Term -> term\n")
        writer.write("                        is rdf.NamedNode -> rdfkt.NamedTerm(term.value)\n")
        writer.write("                        else -> throw IllegalArgumentException()\n")
        writer.write("                    }\n")
        writer.write("                    val o = when (val term = q.`object`) {\n")
        writer.write("                        is rdfkt.Term -> term\n")
        writer.write("                        is rdf.NamedNode -> rdfkt.NamedTerm(term.value)\n")
        writer.write("                        is rdf.BlankNode -> rdfkt.BlankTerm(term.value)\n")
        writer.write("                        is rdf.Literal -> rdfkt.Literal(term.value, term.datatype?.let { rdfkt.NamedTerm(it.value) }, term.language.ifEmpty { null })\n")
        writer.write("                        else -> throw IllegalArgumentException()\n")
        writer.write("                    }\n")
        writer.write("                    val g = when (val term = q.graph) {\n")
        writer.write("                        is rdfkt.Term -> term\n")
        writer.write("                        is rdf.NamedNode -> rdfkt.NamedTerm(term.value)\n")
        writer.write("                        is rdf.BlankNode -> rdfkt.BlankTerm(term.value)\n")
        writer.write("                        is rdf.DefaultGraph -> rdfkt.DefaultGraph\n")
        writer.write("                        else -> throw IllegalArgumentException()\n")
        writer.write("                    }\n")
        writer.write("                    virtualDataset.add(rdfkt.Quad(s as rdfkt.BlankNodeOrIRI, p as rdfkt.NamedTerm, o, g as rdfkt.Graph))\n")
        writer.write("                }\n")
        writer.write("                val mappedObject = when (val term = quad.`object`) {\n")
        writer.write("                    is rdfkt.Term -> term\n")
        writer.write("                    is rdf.NamedNode -> rdfkt.NamedTerm(term.value)\n")
        writer.write("                    is rdf.BlankNode -> rdfkt.BlankTerm(term.value)\n")
        writer.write("                    is rdf.Literal -> rdfkt.Literal(term.value, term.datatype?.let { rdfkt.NamedTerm(it.value) }, term.language.ifEmpty { null })\n")
        writer.write("                    else -> throw IllegalArgumentException()\n")
        writer.write("                }\n")
    }

    private fun generateMappedFromCode(
        writer: java.io.Writer,
        name: String,
        annotation: KSAnnotation,
        typeName: String,
        type: KSType
    ) {
        val isList = typeName == "kotlin.collections.List"
        val classes = (getAnnotationArgument(annotation, "classes") as? List<*>)
            ?.filterIsInstance<KSType>()
            ?.map { it.declaration as KSClassDeclaration }
            ?: emptyList()

        if (isList) {
            val elementType = type.arguments.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""

            writer.write("        val $name = mutableListOf<$elementTypeName>()\n")
            for (classDecl in classes) {
                val classQualName = classDecl.qualifiedName?.asString() ?: ""
                val mappedByPredicateAnn = getAnnotation(classDecl, MappedByPredicate::class)
                if (mappedByPredicateAnn != null) {
                    val predicateUri = getAnnotationArgument(mappedByPredicateAnn, "uri") as String
                    writer.write("        val ${classDecl.simpleName.asString()}Quads = dataset.match(subject = resource, predicate = NamedTerm(\"$predicateUri\")).toList()\n")
                    writer.write("        for (quad in ${classDecl.simpleName.asString()}Quads) {\n")
                    writer.write("            val expr = loader.map(dataset, quad.`object`, setOf($classQualName::class))\n")
                    writer.write("            $name.add(expr)\n")
                    writer.write("        }\n")
                }
            }
        } else {
            writer.write("        var $name: $typeName? = null\n")
            for (classDecl in classes) {
                val classQualName = classDecl.qualifiedName?.asString() ?: ""
                val mappedByPredicateAnn = getAnnotation(classDecl, MappedByPredicate::class)
                if (mappedByPredicateAnn != null) {
                    val predicateUri = getAnnotationArgument(mappedByPredicateAnn, "uri") as String
                    writer.write("        val ${classDecl.simpleName.asString()}Quads = dataset.match(subject = resource, predicate = NamedTerm(\"$predicateUri\")).toList()\n")
                    writer.write("        if (${classDecl.simpleName.asString()}Quads.isNotEmpty() && $name == null) {\n")
                    writer.write("            $name = loader.map(dataset, ${classDecl.simpleName.asString()}Quads.first().`object`, setOf($classQualName::class))\n")
                    writer.write("        }\n")
                }
            }
        }
    }

    private fun generateRegistry() {
        if (processedClasses.isEmpty()) return

        val file = codeGenerator.createNewFile(
            Dependencies(true),
            "rdfobjectloader",
            "GeneratedMappersRegistry"
        )

        OutputStreamWriter(file, Charsets.UTF_8).use { writer ->
            writer.write("package rdfobjectloader\n\n")
            writer.write("import rdfobjectloader.CommonRdfObjectLoader\n\n")

            writer.write("object GeneratedMappersRegistry {\n")
            writer.write("    fun registerAll(loader: CommonRdfObjectLoader) {\n")

            for (classDecl in processedClasses) {
                val packageName = classDecl.packageName.asString()
                val className = classDecl.simpleName.asString()
                writer.write("        loader.registerMapper($packageName.$className::class, $packageName.${className}__Mapper())\n")
            }

            writer.write("    }\n")
            writer.write("}\n")
        }
    }

    private fun isPrimitive(name: String): Boolean {
        return name == "kotlin.String" ||
                name == "kotlin.Int" ||
                name == "kotlin.Long" ||
                name == "kotlin.Boolean" ||
                name == "kotlin.Float" ||
                name == "kotlin.Double"
    }

    private fun getPrimitiveCastExpr(expr: String, name: String): String {
        return when (name) {
            "kotlin.String" -> expr
            "kotlin.Int" -> "$expr.toInt()"
            "kotlin.Long" -> "$expr.toLong()"
            "kotlin.Boolean" -> "$expr.toBoolean()"
            "kotlin.Float" -> "$expr.toFloat()"
            "kotlin.Double" -> "$expr.toDouble()"
            else -> expr
        }
    }

    private fun findPropertyUriInClass(classDecl: KSClassDeclaration, propertyName: String): String {
        val constructorParam = classDecl.primaryConstructor?.parameters?.find { it.name?.asString() == propertyName }
        if (constructorParam != null) {
            val rdfPropertyAnn = getAnnotation(constructorParam, RdfProperty::class)
            if (rdfPropertyAnn != null) {
                return getAnnotationArgument(rdfPropertyAnn, "uri") as String
            }
            val shortcutAnn = getAnnotation(constructorParam, RdfShortcutProperty::class)
            if (shortcutAnn != null) {
                return getAnnotationArgument(shortcutAnn, "uri") as String
            }
        }

        val prop = classDecl.getAllProperties().find { it.simpleName.asString() == propertyName }
        if (prop != null) {
            val rdfPropertyAnn = getAnnotation(prop, RdfProperty::class)
            if (rdfPropertyAnn != null) {
                return getAnnotationArgument(rdfPropertyAnn, "uri") as String
            }
            val shortcutAnn = getAnnotation(prop, RdfShortcutProperty::class)
            if (shortcutAnn != null) {
                return getAnnotationArgument(shortcutAnn, "uri") as String
            }
        }

        return ""
    }

    private fun getAnnotation(annotated: KSAnnotated, name: KClass<*>): KSAnnotation? {
        return annotated.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == name.qualifiedName.toString()
        }
    }

    private fun getAnnotationArgument(annotation: KSAnnotation, name: String): Any? {
        return annotation.arguments.find { it.name?.asString() == name }?.value
    }
}

class RdfObjectLoaderProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RdfObjectLoaderProcessor(environment.codeGenerator, environment.logger)
    }
}
