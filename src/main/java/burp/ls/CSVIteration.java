package burp.ls;

import burp.model.Iteration;
import burp.reporting.BurpException;
import burp.reporting.Origin;
import burp.reporting.RmlError;
import burp.vocabularies.RER;
import com.opencsv.CSVWriter;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class CSVIteration extends Iteration {

    // Use a LinkedHashMap to preserve a correspondence between keys and values
	private final Map<String, String> map = new LinkedHashMap<>();
	
	public CSVIteration(String[] header, String[] rec, Set<Object> nulls) {
		super(nulls);

		for(int i = 0; i < header.length; i++) {
			map.put(header[i], rec[i]);
		}
	}

	@Override
    public List<Object> getValuesFor(@NotNull String reference, Origin origin) {
        List<Object> l = new ArrayList<>();
        if (!map.containsKey(reference))
            throw new BurpException(new RmlError(
                    "Attribute " + reference + " does not exist.\n"
                            + "Available references are: " + String.join(", ", map.keySet()),
                    origin,
                    RER.ReferenceFormulationExecutionError,
                    null));

        String o = map.get(reference);
        if (nulls == null || !nulls.contains(o)) l.add(o);

        return l;
    }

	@Override
	public List<String> getStringsFor(@NotNull String reference, Origin origin) {
		return getValuesFor(reference, origin).stream().map(Object::toString).collect(Collectors.toList());
	}

    @Override
    public String asString() {
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter writer = new CSVWriter(stringWriter)) {
            String[] header = map.keySet().toArray(new String[0]);
            writer.writeNext(header);
            String[] rec = map.values().toArray(new String[0]);
            writer.writeNext(rec);
        } catch(Exception e) {
            throw new RuntimeException("Error representing CSV iteration as CSV.");
        }
        return stringWriter.toString();
    }

}
