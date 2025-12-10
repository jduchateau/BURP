package burp.ls;

import burp.model.Iteration;
import burp.reporting.Origin;
import burp.util.SimpleNamespaceContext;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XMLIteration extends Iteration {

	private final Node node;
	private final Map<String, String> prefixMap;

	public XMLIteration(Node node, Set<Object> nulls, Map<String, String> prefixMap) {
		super(nulls);

		this.node = node;
		this.prefixMap = prefixMap;
	}

	@Override
	public List<Object> getValuesFor(@NotNull String reference, Origin origin) {
		// We need to explicitly convert the objects
		// to strings because RML has not worked out
		// "6.6.1 Automatically deriving datatypes" yet
		List<Object> l2 = new ArrayList<>();
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			if (prefixMap != null) {
				SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefixMap);
				xPath.setNamespaceContext(namespaces);
			}
			NodeList nodes = (NodeList) xPath.compile(reference).evaluate(node, XPathConstants.NODESET);
			for(int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(0);
				if(node.getTextContent() != null && !nulls.contains(node.getTextContent()))
					l2.add(node.getTextContent());
			}

		} catch (Exception e) {
			// No data, silently ignore
			e.printStackTrace();
		}
		return l2;
	}

	@Override
	public List<String> getStringsFor(@NotNull String reference, Origin origin) {
		List<String> l2 = new ArrayList<>();
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			if (prefixMap != null) {
				SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefixMap);
				xPath.setNamespaceContext(namespaces);
			}
			NodeList nodes = (NodeList) xPath.compile(reference).evaluate(node, XPathConstants.NODESET);
			for(int i = 0; i < nodes.getLength(); i++) {
				Node node = nodes.item(0);
				if(node.getTextContent() != null && !nulls.contains(node.getTextContent()))
					l2.add(node.getTextContent());
			}

		} catch (Exception e) {
			// No data, silently ignore
			e.printStackTrace();
		}
		return l2;
	}

    @Override
    public String asString() {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting Node to String", e);
        }
    }

}
