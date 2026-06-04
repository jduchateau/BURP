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

    data class ClassInfo(val packageName: String, val className: String)

    private val processedClasses = mutableListOf<ClassInfo>()
    private val processedClassNames = mutableSetOf<String>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = getAllClassDeclarations(resolver)
            .filter { isRdfMappedClass(it) }
            .filter { it.qualifiedName?.asString() !in processedClassNames }
            .toList()

        for (symbol in symbols) {
            symbol.qualifiedName?.asString()?.let { processedClassNames.add(it) }
            generateMapperForClass(symbol)
            processedClasses.add(ClassInfo(symbol.packageName.asString(), symbol.simpleName.asString()))
        }

        return emptyList()
    }

    override fun finish() {
        generateRegistry()
    }

    private fun getAllClassDeclarations(resolver: Resolver): List<KSClassDeclaration> {
        val classes = mutableListOf<KSClassDeclaration>()
        fun visit(decl: KSDeclaration) {
            if (decl is KSClassDeclaration) {
                classes.add(decl)
                decl.declarations.forEach { visit(it) }
            }
        }
        resolver.getNewFiles().forEach { file ->
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

        logger.info("Generating mapper for class $className")
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
            writer.write("import rdfkt.JenaNamedNode\n")
            writer.write("import rdfkt.JenaBlankNode\n")
            writer.write("import rdfkt.UnionDataset\n")
            writer.write("import rdfkt.toRdfkt\n")
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
                    if (typeName == "org.apache.jena.rdf.model.Resource") {
                        writer.write("        val $paramName = when (resource) {\n")
                        writer.write("            is rdfkt.JenaNamedNode -> resource.node\n")
                        writer.write("            is rdfkt.JenaBlankNode -> resource.node\n")
                        writer.write("            else -> throw IllegalArgumentException(\"Cannot extract Resource from term: \$resource\")\n")
                        writer.write("        }\n")
                    } else if (typeName == "rdf.Term") {
                        writer.write("        val $paramName = resource\n")
                    } else if (typeName == "rdf.NamedNode") {
                        writer.write("        val $paramName = resource as rdf.NamedNode\n")
                    } else if (typeName == "rdf.BlankNode") {
                        writer.write("        val $paramName = resource as rdf.BlankNode\n")
                    } else {
                        writer.write("        val $paramName = resource.value\n")
                    }
                } else if (rdfMappedFromAnn != null) {
                    generateMappedFromCode(writer, paramName, rdfMappedFromAnn, typeName, paramType)
                } else if (rdfShortcutAnn != null) {
                    val shortcutUri = getAnnotationArgument(rdfShortcutAnn, "uri") as String
                    val shortcutFor =
                        getAnnotationArgument(rdfShortcutAnn, "shortcutFor") as String
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
                    generateOriginOfPropQuadCode(writer, classDecl, originOfPropAnn, paramName)
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
            val primaryConstructorParamNames = constructor?.parameters?.mapNotNull { it.name?.asString() }?.toSet() ?: emptySet()
            val properties = classDecl.getAllProperties()
                .filter { it.isMutable }
                .filter { it.simpleName.asString() !in primaryConstructorParamNames }
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
                    if (propType.isMarkedNullable) {
                        writer.write("        if (${propName}Val != null) instance.$propName = ${propName}Val\n")
                    } else {
                        writer.write("        instance.$propName = ${propName}Val\n")
                    }
                } else if (rdfMappedFromAnn != null) {
                    generateMappedFromCode(writer, "${propName}Val", rdfMappedFromAnn, typeName, propType)
                    if (propType.isMarkedNullable) {
                        writer.write("        if (${propName}Val != null) instance.$propName = ${propName}Val\n")
                    } else {
                        writer.write("        instance.$propName = ${propName}Val\n")
                    }
                } else if (rdfPropertyAnn != null) {
                    val propertyUri = getAnnotationArgument(rdfPropertyAnn, "uri") as String
                    generatePropertyMappingCode(writer, "${propName}Val", propertyUri, typeName, propType)
                    if (propType.isMarkedNullable) {
                        writer.write("        if (${propName}Val != null) instance.$propName = ${propName}Val\n")
                    } else {
                        writer.write("        instance.$propName = ${propName}Val\n")
                    }
                } else if (originOfPropAnn != null) {
                    val propertyName = getAnnotationArgument(originOfPropAnn, "propertyName") as String
                    generateOriginOfPropQuadCode(writer, classDecl, originOfPropAnn, propName)
                    if (propType.isMarkedNullable) {
                        writer.write("        val ${propName}Val = ${propName}Quad?.let { StatementParts.fromObject(it) }\n")
                        writer.write("        if (${propName}Val != null) instance.$propName = ${propName}Val\n")
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
        val isCollection = isCollection(typeName)

        if (isCollection) {
            val elementType = type.arguments.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""
            writer.write("        val ${name}RawTerms: List<Term> = run {\n")
            writer.write("            val quads = dataset.match(subject = resource, predicate = NamedTerm(\"$uri\")).toList()\n")
            writer.write("            if (quads.isEmpty()) emptyList()\n")
            writer.write("            else {\n")
            writer.write("                val firstQuad = quads.first()\n")
            writer.write("                val hasFirst = dataset.match(subject = firstQuad.`object`, predicate = NamedTerm(\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\")).firstOrNull() != null\n")
            writer.write("                if (quads.size == 1 && hasFirst) {\n")
            writer.write("                    val list = mutableListOf<Term>()\n")
            writer.write("                    var current = firstQuad.`object`\n")
            writer.write("                    while (current.value != \"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\") {\n")
            writer.write("                        val first = dataset.match(subject = current, predicate = NamedTerm(\"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\")).firstOrNull()?.`object`\n")
            writer.write("                        if (first != null) list.add(first)\n")
            writer.write("                        val rest = dataset.match(subject = current, predicate = NamedTerm(\"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\")).firstOrNull()?.`object`\n")
            writer.write("                        current = rest ?: break\n")
            writer.write("                    }\n")
            writer.write("                    list\n")
            writer.write("                } else {\n")
            writer.write("                    quads.map { it.`object` }\n")
            writer.write("                }\n")
            writer.write("            }\n")
            writer.write("        }\n")

            val conversion = when (typeName) {
                "kotlin.collections.Set" -> ".toSet()"
                "kotlin.collections.MutableSet" -> ".toMutableSet()"
                "kotlin.collections.MutableList", "kotlin.collections.MutableCollection" -> ".toMutableList()"
                else -> ""
            }

            if (isPrimitive(elementTypeName)) {
                val castExpr = getPrimitiveCastExpr("it.value", elementTypeName)
                writer.write("        val $name = ${name}RawTerms.map { $castExpr }$conversion\n")
            } else if (elementTypeName == "org.apache.jena.rdf.model.Resource") {
                writer.write("""
        val $name = ${name}RawTerms.map { term ->
            when (term) {
                is rdfkt.JenaNamedNode -> term.node
                is rdfkt.JenaBlankNode -> term.node
                else -> throw IllegalArgumentException("Cannot extract Resource from term: ${'$'}term")
            }
        }$conversion
                """.trimIndent() + "\n")
            } else if (elementTypeName == "rdf.Term") {
                writer.write("        val $name = ${name}RawTerms$conversion\n")
            } else if (elementTypeName == "rdf.NamedNode") {
                writer.write("        val $name = ${name}RawTerms.map { it as rdf.NamedNode }$conversion\n")
            } else if (elementTypeName == "rdf.BlankNode") {
                writer.write("        val $name = ${name}RawTerms.map { it as rdf.BlankNode }$conversion\n")
            } else {
                writer.write("        val $name = ${name}RawTerms.map { term -> loader.map(dataset, term, setOf($elementTypeName::class)) }$conversion\n")
            }
        } else {
            writer.write("        val ${name}Quads = dataset.match(subject = resource, predicate = NamedTerm(\"$uri\")).toList()\n")
            writer.write("        val ${name}Quad = ${name}Quads.firstOrNull()\n")

            if (isPrimitive(typeName)) {
                val castExpr = getPrimitiveCastExpr("${name}Quad.`object`.value", typeName)
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.let { $castExpr }\n")
                } else {
                    writer.write("        val $name = if (${name}Quad != null) $castExpr else throw IllegalArgumentException(\"Missing required property $uri for type $typeName in resource \$resource\")\n")
                }
            } else if (typeName == "org.apache.jena.rdf.model.Resource") {
                if (type.isMarkedNullable) {
                    writer.write(
                        $$"""
        val $$name = $${name}Quad?.`object`?.let { term ->
            when (term) {
                is rdfkt.JenaNamedNode -> term.node
                is rdfkt.JenaBlankNode -> term.node
                else -> throw IllegalArgumentException("Cannot extract Resource from term: $term")
            }
        }
                    """.trimIndent() + "\n")
                } else {
                    writer.write(
                        $$"""
        val $$name = if ($${name}Quad != null) {
            val term = $${name}Quad.`object`
            when (term) {
                is rdfkt.JenaNamedNode -> term.node
                is rdfkt.JenaBlankNode -> term.node
                else -> throw IllegalArgumentException("Cannot extract Resource from term: $term")
            }
        } else throw IllegalArgumentException("Missing required property $$uri for type Resource in resource $resource")
                    """.trimIndent() + "\n")
                }
            } else if (typeName == "rdf.Term") {
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.`object`\n")
                } else {
                    writer.write($$"        val $$name = if ($${name}Quad != null) $${name}Quad.`object` else throw IllegalArgumentException(\"Missing required property $$uri for type rdf.Term in resource $resource\")\n")
                }
            } else if (typeName == "rdf.NamedNode") {
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.`object` as? rdf.NamedNode\n")
                } else {
                    writer.write($$"        val $$name = if ($${name}Quad != null) $${name}Quad.`object` as rdf.NamedNode else throw IllegalArgumentException(\"Missing required property $$uri for type rdf.NamedNode in resource $resource\")\n")
                }
            } else if (typeName == "rdf.BlankNode") {
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.`object` as? rdf.BlankNode\n")
                } else {
                    writer.write($$"        val $$name = if ($${name}Quad != null) $${name}Quad.`object` as rdf.BlankNode else throw IllegalArgumentException(\"Missing required property $$uri for type rdf.BlankNode in resource $resource\")\n")
                }
            } else {
                if (type.isMarkedNullable) {
                    writer.write("        val $name = ${name}Quad?.`object`?.let { loader.map(dataset, it, setOf($typeName::class)) }\n")
                } else {
                    writer.write("        val $name = if (${name}Quad != null) loader.map(dataset, ${name}Quad.`object`, setOf($typeName::class)) else throw IllegalArgumentException(\"Missing required property $uri for type $typeName in resource \$resource\")\n")
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
        val isCollection = isCollection(typeName)

        val targetClassDecl = type.declaration as? KSClassDeclaration
        val primaryConstructor = targetClassDecl?.primaryConstructor
        val singleParam = primaryConstructor?.parameters?.singleOrNull()
        val singleParamType = singleParam?.type?.resolve()
        val isSingleListParam = singleParamType?.declaration?.qualifiedName?.asString() == "kotlin.collections.List"

        if (isCollection) {
            val elementType = type.arguments.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""
            val conversion = when (typeName) {
                "kotlin.collections.Set" -> ".toSet()"
                "kotlin.collections.MutableSet" -> ".toMutableSet()"
                "kotlin.collections.MutableList", "kotlin.collections.MutableCollection" -> ".toMutableList()"
                else -> ""
            }
            generateCollectionShortcutCode(writer, name, shortcutUri, shortcutFor, standardPropertyUri, elementTypeName, conversion)
        } else if (isSingleListParam) {
            val listPropName = singleParam.name!!.asString()
            val elementType = singleParamType.arguments.firstOrNull()?.type?.resolve()
            val elementTypeName = elementType?.declaration?.qualifiedName?.asString() ?: ""
            generateSingleListParamShortcutCode(writer, name, shortcutUri, shortcutFor, standardPropertyUri, typeName, type, listPropName, elementTypeName)
        } else {
            generateSingleValueShortcutCode(writer, name, shortcutUri, shortcutFor, standardPropertyUri, typeName, type)
        }
    }

    private fun generateCollectionShortcutCode(
        writer: java.io.Writer,
        name: String,
        shortcutUri: String,
        shortcutFor: String,
        standardPropertyUri: String,
        elementTypeName: String,
        conversion: String
    ) {
        writer.write("        val ${name}ShortcutQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$shortcutUri\")).toList()\n")
        if (standardPropertyUri.isNotEmpty()) {
            writer.write("        val ${name}ExpressionQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$standardPropertyUri\")).toList()\n")
            writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty() || ${name}ExpressionQuads.isNotEmpty()) {\n")
        } else {
            writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty()) {\n")
        }
        writer.write("            val combinedList = mutableListOf<$elementTypeName>()\n")
        writer.write("            for (quad in ${name}ShortcutQuads) {\n")
        writer.write("                val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
        writer.write("                val mappedObject = quad.`object`.toRdfkt()\n")
        writer.write("                val virtualDataset = rdfkt.UnionDataset(dataset, mutableSetOf(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject)))\n")
        writer.write("                val nested = loader.map(virtualDataset, virtualSubject, setOf($elementTypeName::class))\n")
        writer.write("                combinedList.add(nested)\n")
        writer.write("            }\n")
        if (standardPropertyUri.isNotEmpty()) {
            writer.write("            for (quad in ${name}ExpressionQuads) {\n")
            writer.write("                val nested = loader.map(dataset, quad.`object`, setOf($elementTypeName::class))\n")
            writer.write("                combinedList.add(nested)\n")
            writer.write("            }\n")
        }
        writer.write("            combinedList$conversion\n")
        writer.write("        } else {\n")
        writer.write("            emptyList<$elementTypeName>()$conversion\n")
        writer.write("        }\n")
    }

    private fun generateSingleListParamShortcutCode(
        writer: java.io.Writer,
        name: String,
        shortcutUri: String,
        shortcutFor: String,
        standardPropertyUri: String,
        typeName: String,
        type: KSType,
        listPropName: String,
        elementTypeName: String
    ) {
        writer.write("        val ${name}ShortcutQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$shortcutUri\")).toList()\n")
        if (standardPropertyUri.isNotEmpty()) {
            writer.write("        val ${name}ExpressionQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$standardPropertyUri\")).toList()\n")
            writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty() || ${name}ExpressionQuads.isNotEmpty()) {\n")
        } else {
            writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty()) {\n")
        }
        writer.write("            val combinedList = mutableListOf<$elementTypeName>()\n")
        writer.write("            for (quad in ${name}ShortcutQuads) {\n")
        writer.write("                val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
        writer.write("                val mappedObject = quad.`object`.toRdfkt()\n")
        writer.write("                val virtualDataset = rdfkt.UnionDataset(dataset, mutableSetOf(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject)))\n")
        writer.write("                val nested = loader.map(virtualDataset, virtualSubject, setOf($typeName::class))\n")
        writer.write("                combinedList.addAll(nested.$listPropName)\n")
        writer.write("            }\n")
        if (standardPropertyUri.isNotEmpty()) {
            writer.write("            for (quad in ${name}ExpressionQuads) {\n")
            writer.write("                val nested = loader.map(dataset, quad.`object`, setOf($typeName::class))\n")
            writer.write("                combinedList.addAll(nested.$listPropName)\n")
            writer.write("            }\n")
        }
        writer.write("            $typeName(combinedList)\n")
        writer.write("        } else {\n")
        if (type.isMarkedNullable) {
            writer.write("            null\n")
        } else {
            if (standardPropertyUri.isNotEmpty()) {
                writer.write("            throw IllegalArgumentException(\"Missing required shortcut/expression property $shortcutUri/$standardPropertyUri\")\n")
            } else {
                writer.write("            throw IllegalArgumentException(\"Missing required shortcut property $shortcutUri\")\n")
            }
        }
        writer.write("        }\n")
    }

    private fun generateSingleValueShortcutCode(
        writer: java.io.Writer,
        name: String,
        shortcutUri: String,
        shortcutFor: String,
        standardPropertyUri: String,
        typeName: String,
        type: KSType
    ) {
        writer.write("        val ${name}ShortcutQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$shortcutUri\")).toList()\n")
        writer.write("        val $name = if (${name}ShortcutQuads.isNotEmpty()) {\n")
        writer.write("            val quad = ${name}ShortcutQuads.first()\n")
        writer.write("            val virtualSubject = rdfkt.BlankTerm(\"virtual_bnode_shortcut_\${quad.`object`.value}\")\n")
        writer.write("            val mappedObject = quad.`object`.toRdfkt()\n")
        writer.write("            val virtualDataset = rdfkt.UnionDataset(dataset, mutableSetOf(rdfkt.Quad(virtualSubject, NamedTerm(\"$shortcutFor\"), mappedObject)))\n")
        writer.write("            loader.map(virtualDataset, virtualSubject, setOf($typeName::class))\n")
        writer.write("        } else {\n")
        if (standardPropertyUri.isNotEmpty()) {
            writer.write("            val ${name}ExpressionQuads = dataset.match(subject = resource, predicate = NamedTerm(\"$standardPropertyUri\")).toList()\n")
            writer.write("            if (${name}ExpressionQuads.isNotEmpty()) {\n")
            writer.write("                val exprNode = ${name}ExpressionQuads.first().`object`\n")
            writer.write("                loader.map(dataset, exprNode, setOf($typeName::class))\n")
            writer.write("            } else {\n")
            if (type.isMarkedNullable) {
                writer.write("                null\n")
            } else {
                writer.write("                throw IllegalArgumentException(\"Missing required shortcut/expression property $shortcutUri/$standardPropertyUri\")\n")
            }
            writer.write("            }\n")
        } else {
            if (type.isMarkedNullable) {
                writer.write("            null\n")
            } else {
                writer.write("            throw IllegalArgumentException(\"Missing required shortcut property $shortcutUri\")\n")
            }
        }
        writer.write("        }\n")
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

        logger.info("Generating registry")
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

            for (info in processedClasses) {
                val packageName = info.packageName
                val className = info.className
                writer.write("        loader.registerMapper($packageName.$className::class, $packageName.${className}__Mapper())\n")
            }

            writer.write("    }\n")
            writer.write("}\n")
        }
    }

    private val primitiveTypes = setOf(
        "kotlin.String",
        "kotlin.Int",
        "kotlin.Long",
        "kotlin.Boolean",
        "kotlin.Float",
        "kotlin.Double"
    )

    private val collectionTypes = setOf(
        "kotlin.collections.List",
        "kotlin.collections.MutableList",
        "kotlin.collections.Set",
        "kotlin.collections.MutableSet",
        "kotlin.collections.Collection",
        "kotlin.collections.MutableCollection"
    )

    private fun isPrimitive(name: String): Boolean = name in primitiveTypes

    private fun isCollection(name: String): Boolean = name in collectionTypes

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

    private fun getPropertyUri(annotated: KSAnnotated): String? {
        val rdfPropertyAnn = getAnnotation(annotated, RdfProperty::class)
        if (rdfPropertyAnn != null) {
            return getAnnotationArgument(rdfPropertyAnn, "uri") as? String
        }
        val shortcutAnn = getAnnotation(annotated, RdfShortcutProperty::class)
        if (shortcutAnn != null) {
            return getAnnotationArgument(shortcutAnn, "uri") as? String
        }
        return null
    }

    private fun generateOriginOfPropQuadCode(
        writer: java.io.Writer,
        classDecl: KSClassDeclaration,
        originOfPropAnn: KSAnnotation,
        varPrefix: String
    ) {
        val propertyName = getAnnotationArgument(originOfPropAnn, "propertyName") as String
        val mappedByPredicateAnn = getAnnotation(classDecl, MappedByPredicate::class)
        if (mappedByPredicateAnn != null) {
            val propUri = getAnnotationArgument(mappedByPredicateAnn, "uri") as String
            writer.write("        val ${varPrefix}Quad = dataset.match(subject = null, predicate = NamedTerm(\"$propUri\"), `object` = resource).firstOrNull()\n")
        } else {
            val propUri = findPropertyUriInClass(classDecl, propertyName)
            writer.write("        val ${varPrefix}Quad = dataset.match(subject = resource, predicate = NamedTerm(\"$propUri\")).firstOrNull()\n")
        }
    }

    private fun findPropertyUriInClass(classDecl: KSClassDeclaration, propertyName: String): String {
        val constructorParam = classDecl.primaryConstructor?.parameters?.find { it.name?.asString() == propertyName }
        if (constructorParam != null) {
            getPropertyUri(constructorParam)?.let { return it }
        }

        val prop = classDecl.getAllProperties().find { it.simpleName.asString() == propertyName }
        if (prop != null) {
            getPropertyUri(prop)?.let { return it }
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
