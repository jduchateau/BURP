package burp.model

import burp.model.fnmlutil.Return

abstract class RMLFunction {
    abstract fun apply(map: MutableMap<String?, Any?>?): MutableList<Return?>?
}
