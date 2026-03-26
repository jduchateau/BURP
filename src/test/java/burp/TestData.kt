package burp

class TestData(
    @JvmField var ID: String,
    @JvmField var title: String,
    @JvmField var description: String,
    @JvmField var specification: String,
    @JvmField var baseIRI: String,
    @JvmField var mapping: String,
    @JvmField var input_format1: String?,
    @JvmField var input_format2: String?,
    @JvmField var input_format3: String?,
    @JvmField var output_format1: String?,
    @JvmField var output_format2: String?,
    @JvmField var output_format3: String?,
    @JvmField var input1: String?,
    @JvmField var input2: String?,
    @JvmField var input3: String?,
    @JvmField var output1: String?,
    @JvmField var output2: String?,
    @JvmField var output3: String?,
    @JvmField var error: Boolean
) {

    constructor(data: Map<String, String>) : this(
        data.getValue("ID"),
        data.getValue("title"),
        data.getValue("description"),
        data.getValue("specification"),
        data.getOrDefault("base_iri", "http://example.com/base/"),
        data.getValue("mapping"),
        data["input_format1"],
        data["input_format2"],
        data["input_format3"],
        data["output_format1"],
        data["output_format2"],
        data["output_format3"],
        data["input1"],
        data["input2"],
        data["input3"],
        data["output1"],
        data["output2"],
        data["output3"],
        "true".equals(data["error"], ignoreCase = true)
    )


    override fun toString() = ID
}

