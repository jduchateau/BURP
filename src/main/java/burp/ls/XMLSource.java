package burp.ls;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;

import burp.reporting.BurpException;
import burp.reporting.Origin;
import burp.reporting.RmlError;
import burp.vocabularies.RER;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import burp.model.Iteration;
import burp.util.SimpleNamespaceContext;

public class XMLSource extends FileBasedLogicalSource {

    public HashMap<String, String> prefixMap;

    @Override
    public Iterator<Iteration> iterator() throws BurpException {
        try {
            if (iterations == null) {
                iterations = new ArrayList<>();

                String contents = Files.readString(Paths.get(getDecompressedFile()), encoding);

                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                if (prefixMap != null) {
                    // Required for prefix evaluation of XPath expression
                    builderFactory.setNamespaceAware(true);
                }
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(IOUtils.toInputStream(contents, encoding));

                XPath xPath = XPathFactory.newInstance().newXPath();
                if (prefixMap != null) {
                    SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefixMap);
                    xPath.setNamespaceContext(namespaces);
                }

                NodeList nodes = (NodeList) xPath.compile(iterator).evaluate(xmlDocument, XPathConstants.NODESET);

                for (int i = 0; i < nodes.getLength(); i++) {
                    Node node = nodes.item(i);
                    iterations.add(new XMLIteration(node, nulls, prefixMap));
                }
            }
            return iterations.iterator();
        } catch (XPathExpressionException e) {
            throw new BurpException(new RmlError(e.getMessage(), iteratorOrigin, RER.ReferenceFormulationSyntaxError, e, Collections.emptyMap()));
        } catch (Exception e) {
            throw new BurpException(new RmlError(e.getMessage(), iteratorOrigin, RER.ReferenceFormulationExecutionError, e, Collections.emptyMap()));
        }
    }

}

