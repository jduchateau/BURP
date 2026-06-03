package turtleprov.manifest

import rdfobjectloader.CommonRdfObjectLoader
import rdfobjectloader.GeneratedMappersRegistry

actual fun registerGeneratedMappers(loader: CommonRdfObjectLoader) {
    GeneratedMappersRegistry.registerAll(loader)
}
