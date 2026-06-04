package burp.model

import burp.vocabularies.Rml
import rdfobjectloader.annotations.RdfProperty
import rdfobjectloader.annotations.RdfShortcutProperty
import rdfobjectloader.annotations.RdfType

@RdfType(Rml.Input)
class Input {
    @RdfProperty(Rml.parameterMap)
    @RdfShortcutProperty(Rml.parameter, Rml.constant)
    lateinit var parameterMap: ParameterMap

    @RdfProperty(Rml.inputValueMap)
    @RdfShortcutProperty(Rml.inputValue, Rml.constant)
    lateinit var inputValueMap: InputValueMap
}
