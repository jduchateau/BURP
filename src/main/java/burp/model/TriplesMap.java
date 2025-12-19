package burp.model;

import org.apache.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

public class TriplesMap {

    public TriplesMap(Resource subject) {
        this.subject = subject;
    }

    public Resource subject;

    public AbstractLogicalSource logicalSource = null;
    public SubjectMap subjectMap = null;
    public List<PredicateObjectMap> predicateObjectMaps = new ArrayList<>();
    public String baseIRI = null;


    public int countGeneratedStatements = 0;

}