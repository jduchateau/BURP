package burp

class TestRMLCC : TestRMLModule() {
    override fun getBase(): String {
        return Companion.base
    }

    companion object {
        var base: String = "./src/test/resources/rml-cc/"
    }
}
