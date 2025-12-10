package burp.model;

import burp.reporting.Origin;
import burp.reporting.PlanNode;
import org.apache.jena.rdf.model.Resource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class Iterable implements PlanNode {

    public String iterator;
    public Origin iteratorOrigin;
    public Resource referenceFormulation;

}