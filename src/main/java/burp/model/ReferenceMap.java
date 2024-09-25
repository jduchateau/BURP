package burp.model;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ReferenceMap extends Expression {
    private final Expression expression;

    public ReferenceMap(Expression expression) {
        this.expression = expression;
    }


    private List<String> generateReferenceExpression(Iteration i, String baseIRI) {

        if (expression instanceof RDFNodeConstant rdfNodeConstant) {
            // It is assumed to be a literal, otherwise the shapes
            // Would have caught the error.
            return Collections.singletonList(rdfNodeConstant.constant.toString());
        }

        if (expression instanceof Template template) {
            return template.values(i);
        }

        if (expression instanceof Reference reference) {
            return reference.values(i).stream().map(Object::toString).collect(Collectors.toList());
        }

        if (expression instanceof ReferenceMap referenceMap) {
            return referenceMap.values(i, baseIRI).stream().map(Object::toString).collect(Collectors.toList());
        }

        if (expression instanceof FunctionExecution functionExecution) {
            return functionExecution.values(i, baseIRI).stream().map(Object::toString).collect(Collectors.toList());

        }

        throw new RuntimeException("Error generating literal or value.");
    }


    // If the term map is a reference-valued term map,
    // then the generated RDF term is determined by applying the
    // term generation rules to its reference value.
    public List<Object> values(Iteration i, String baseIRI) {
        List<String> reference = generateReferenceExpression(i, baseIRI);
        return reference.stream().flatMap(ref -> i.getValuesFor(ref).stream()).collect(Collectors.toList());
    }


}
