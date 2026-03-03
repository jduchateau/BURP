package burp.model.gathermap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.jspecify.annotations.Nullable;

public class SubGraph {

	public RDFNode node;
	@Nullable
	public Model model;

	public SubGraph(RDFNode n, @Nullable Model m) {
		this.node = n;
		this.model = m;
	}

	public SubGraph() {}

	public void updateNode(RDFNode n) {
		node = ResourceUtils.renameResource(node.asResource(), n.asResource().getURI());
	}

	public boolean isList() { return !isBag() && !isSeq() && !isAlt(); }
	public boolean isAlt() { return model.contains(node.asResource(), RDF.type, RDF.Alt); }
	public boolean isBag() { return model.contains(node.asResource(), RDF.type, RDF.Bag); }
	public boolean isSeq() { return model.contains(node.asResource(), RDF.type, RDF.Seq); }
	
	public String toString() {
		return node == null ? null : node.toString();
	}
}
