package burp.model.lv

interface ContainsFields {
    val iterableFields: List<IterableField>
    val expressionFields: List<ExpressionField>

    fun addField(field: Field)
}
