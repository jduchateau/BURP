package burp.model

interface TermGenerator : PlanNode {
    fun generateTerms(i: Iteration): List<Term>
}
