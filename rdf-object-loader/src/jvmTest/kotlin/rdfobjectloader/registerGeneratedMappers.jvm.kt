package rdfobjectloader

actual fun registerGeneratedMappers(loader: CommonRdfObjectLoader) {
    GeneratedMappersRegistry.registerAll(loader)
}
