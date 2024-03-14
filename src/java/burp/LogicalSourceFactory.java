package burp;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VOID;

public class LogicalSourceFactory {

	public static LogicalSource createCSVSource(Resource ls, String mpath) {
		String file = getFile(ls);
		CSVSource source = new CSVSource();
		source.file = getAbsoluteOrRelative(file, mpath);
		source.encoding = getEncoding(ls);
		source.compression = getCompression(ls);
		return source;
	}
	
	public static LogicalSource createJSONSource(Resource ls, String mpath) {
		String file = getFile(ls);
		String iterator = ls.getProperty(RML.iterator).getLiteral().getString();
		JSONSource source = new JSONSource();
		source.file = getAbsoluteOrRelative(file, mpath);
		source.iterator = iterator;
		source.encoding = getEncoding(ls);
		source.compression = getCompression(ls);
		return source;
	}
	
	public static LogicalSource createXMLSource(Resource ls, String mpath) {
		String file = getFile(ls);
		String iterator = ls.getProperty(RML.iterator).getLiteral().getString();
		XMLSource source = new XMLSource();
		source.file = getAbsoluteOrRelative(file, mpath);
		source.iterator = iterator;
		source.encoding = getEncoding(ls);
		source.compression = getCompression(ls);
		return source;
	}
	
	public static LogicalSource createSQL2008TableSource(Resource ls, String mpath) {
		Resource s = ls.getPropertyResourceValue(RML.source);
		String jdbcDSN = s.getProperty(D2RQ.jdbcDSN).getLiteral().getString();
		String jdbcDriver = s.getProperty(D2RQ.jdbcDriver).getLiteral().getString();
		String username = s.getProperty(D2RQ.username).getLiteral().getString();
		String password = s.getProperty(D2RQ.password).getLiteral().getString();
		
		Statement t = ls.getProperty(RML.iterator);
		String query = "(SELECT * FROM " + t.getLiteral() + ")";
		
		RDBSource source = new RDBSource();
		source.jdbcDSN = jdbcDSN;
		source.jdbcDriver = jdbcDriver;
		source.username = username;
		source.password = password;
		
		// Apache jena "escapes" double quotes, so "Name" becomes \"Name\"
		// which is internally stored as \\"Name\\". We thus need to remove
		// occurrences of \\
		source.query = query.replace("\\", "");
		return source;
	}
	
	public static LogicalSource createSQL2008QuerySource(Resource ls, String mpath) {
		Resource s = ls.getPropertyResourceValue(RML.source);
		String jdbcDSN = s.getProperty(D2RQ.jdbcDSN).getLiteral().getString();
		String jdbcDriver = s.getProperty(D2RQ.jdbcDriver).getLiteral().getString();
		String username = s.getProperty(D2RQ.username).getLiteral().getString();
		String password = s.getProperty(D2RQ.password).getLiteral().getString();
		
		Statement t = ls.getProperty(RML.iterator);
		String query = t.getLiteral().toString();
		
		RDBSource source = new RDBSource();
		source.jdbcDSN = jdbcDSN;
		source.jdbcDriver = jdbcDriver;
		source.username = username;
		source.password = password;
		
		// Apache jena "escapes" double quotes, so "Name" becomes \"Name\"
		// which is internally stored as \\"Name\\". We thus need to remove
		// occurrences of \\
		source.query = query.replace("\\", "");
		return source;
	}
	
	public static LogicalSource createSPARQLCSVSource(Resource ls, String mpath) {
		String file = getFile(ls);
		String iterator = ls.getProperty(RML.iterator).getLiteral().getString();
		SPARQLCSVSource source = new SPARQLCSVSource();
		source.file = getAbsoluteOrRelativeFromFileProtocol(file, mpath);
		source.encoding = getEncoding(ls);
		source.iterator = iterator;
		return source;
	}
	
	// TODO: RML.SPARQL_Results_JSON
	// TODO: RML.SPARQL_Results_XML

	
	private static String getFile(Resource ls) {		
		Resource source = ls.getPropertyResourceValue(RML.source);
		
		if(source.hasProperty(RDF.type, RML.RelativePathSource)) {
			String file = source.getProperty(RML.path).getLiteral().getString();
			
			Resource root = source.getPropertyResourceValue(RML.root);
			if(root != null && !RML.MappingDirectory.equals(root)) {
				throw new RuntimeException("Root not yet implemented");
			}
			
			// By default BURP treats it relative to mapping.
			return file;
		}

		if(source.hasProperty(RDF.type, DCAT.Distribution)) {
			String url = source.getPropertyResourceValue(DCAT.downloadURL).getURI();
			return Util.downloadFile(url);
		}
		
		if(source.hasProperty(RDF.type, VOID.Dataset)) {
			String file = source.getPropertyResourceValue(VOID.dataDump).getURI();
			return file;
		}
		
		if(source.hasProperty(RDF.type, CSVW.Table)) {
			String file = source.getPropertyResourceValue(CSVW.url).getURI();
			return file;
		}
		
		throw new RuntimeException("Source from other way not yet implemented");
	}
	
	private static Resource getCompression(Resource ls) {
		Resource r = ls.getPropertyResourceValue(RML.source);
		
		if(r == null)
			return RML.none;
		
		r = r.getPropertyResourceValue(RML.compression);
		
		if(r == null || RML.none.equals(r)) return RML.none;
		if(RML.zip.equals(r)) return RML.zip;
		if(RML.gzip.equals(r)) return RML.gzip;
		if(RML.targz.equals(r)) return RML.targz;
		if(RML.tarxz.equals(r)) return RML.tarxz;
		
		throw new RuntimeException("Provided compression " + r + " not supported.");
	}

	private static Charset getEncoding(Resource ls) {
		Resource r = ls.getPropertyResourceValue(RML.source);
		
		if(r == null)
			return StandardCharsets.UTF_8;
		
		r = r.getPropertyResourceValue(RML.encoding);
		
		if(r == null || RML.UTF8.equals(r)) return StandardCharsets.UTF_8;
		if(RML.UTF16.equals(r)) return StandardCharsets.UTF_16;
		
		throw new RuntimeException("Provided Character Set " + r + " not supported.");
	}

	private static String getAbsoluteOrRelative(String file, String mpath) {
		if(new File(file).isAbsolute())
			return file;
		return new File(mpath, file).getAbsolutePath();
	}
	
	private static String getAbsoluteOrRelativeFromFileProtocol(String file, String mpath) {
		try {
			URL url = new URL(file);
			if(Util.isAbsoluteAndValidIRI(file))
				return file;
			String abs = new File(mpath, url.getPath()).toURI().toURL().toString();
			return abs;
		} catch (MalformedURLException e) {
			throw new RuntimeException(file + " is not an URL.");
		}
	}

}