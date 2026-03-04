package burp.model;

import burp.reporting.BurpException;
import burp.reporting.ErrorsKt;
import burp.vocabularies.RER;
import org.apache.jena.langtagx.LangTagX;

import java.util.ArrayList;
import java.util.List;

public class LanguageMap extends ExpressionMap {

	public List<String> generateStrings(Iteration i) {
		List<String> set = new ArrayList<>();
		
		if(expression instanceof RDFNodeConstant) {
			// It is assumed to be a string, otherwise the shapes
			// Would have caught the error.
			set.add(((RDFNodeConstant) expression).constant.toString());
		}
		else if(expression instanceof Template) {
			set.addAll(((Template) expression).values(i));
		}
		else if(expression instanceof Reference) {
			for(Object o : ((Reference) expression).values(i))
				set.add(o.toString());
		}
		
		set.forEach((l) -> {
			if(!isValidLanguageCode(l))
				throw new BurpException(ErrorsKt.InvalidRDF("Invalid language code: " + l, expressionOrigin, RER.InvalidLanguageTagError));
		});
		
		return set;
	}

	private boolean isValidLanguageCode(String lang) {
		return LangTagX.checkLanguageTag(lang);
	}

}