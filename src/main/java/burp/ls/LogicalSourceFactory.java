package burp.ls;

import burp.model.Iteration;
import burp.model.LogicalSource;
import burp.vocabularies.RML;
import com.jayway.jsonpath.JsonPath;
import com.opencsv.CSVReader;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LogicalSourceFactory {

    private static final ServiceLoader<LogicalSourceProvider> LOADER = ServiceLoader.load(LogicalSourceProvider.class);

    public static LogicalSource create(Resource ls, Path mappingDirectory, Path currentWorkingDirectory) {
        Resource referenceFormulation = ls.getPropertyResourceValue(RML.referenceFormulation);
        for (LogicalSourceProvider provider : LOADER) {
            if (provider.supports(referenceFormulation)) {
                return provider.create(ls, mappingDirectory, currentWorkingDirectory);
            }
        }

        String supported = LOADER.stream().map(p -> p.type().getName()).collect(Collectors.joining(", "));
        throw new RuntimeException("Reference formulation not (yet) supported: " + referenceFormulation + ". Are supported: " + supported);
    }

    public static List<Iteration> changeIterator(String iterationAsString, Resource rf, String iterator) {
        try {
            if (RML.JSONPath.equals(rf)) {
                // Create JSON iterations
                List<Iteration> iterations = new ArrayList<>();
                String contents = iterationAsString;
                List<Map<String, Object>> nodes = JsonPath.using(JSONSource.configuration).parse(contents).read(iterator);
                for (Map<String, Object> n : nodes)
                    // TODO: How do we provide null values?
                    iterations.add(new JSONIteration(JSONObject.toJSONString(n), new HashSet<>()));
                return iterations;

            } else if (RML.CSV.equals(rf)) {
                // Create CSV iterations
                CSVReader reader = new CSVReader(new StringReader(iterationAsString));
                List<String[]> all = reader.readAll();
                reader.close();
                String[] header = all.remove(0);
                List<Iteration> iterations = new ArrayList<>();
                for (String[] rec : all)
                    // TODO: How do we provide null values?
                    iterations.add(new CSVIteration(header, rec, new HashSet<>()));
                return iterations;

            } else if (RML.XPath.equals(rf)) {
                // Create XPATH iterations
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(IOUtils.toInputStream(iterationAsString));
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodes = (NodeList) xPath.compile(iterator).evaluate(xmlDocument, XPathConstants.NODESET);

                List<Iteration> iterations = new ArrayList<>();
                for (int index = 0; index < nodes.getLength(); index++) {
                    Node node = nodes.item(index);
                    // TODO: How do we provide null values?
                    // TODO: How do we provide the prefix mappings?
                    iterations.add(new XMLIteration(node, new HashSet<>(), new HashMap<>()));
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        throw new RuntimeException("Other reference formulations for iterable fields are not yet supported: " + rf);
    }
}
