package burp.model.gathermap

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.util.ResourceUtils
import org.apache.jena.vocabulary.RDF

class SubGraph(var node: RDFNode?, var model: Model?) {

    fun updateNode(n: RDFNode) {
        node = ResourceUtils.renameResource(node!!.asResource(), n.asResource().getURI())
    }

    val isList: Boolean
        get() = !this.isBag && !this.isSeq && !this.isAlt
    val isAlt: Boolean
        get() = model!!.contains(
            node!!.asResource(),
            RDF.type,
            RDF.Alt
        )
    val isBag: Boolean
        get() = model!!.contains(
            node!!.asResource(),
            RDF.type,
            RDF.Bag
        )
    val isSeq: Boolean
        get() = model!!.contains(
            node!!.asResource(),
            RDF.type,
            RDF.Seq
        )

    override fun toString(): String {
        return (if (node == null) null else node.toString())!!
    }
}
