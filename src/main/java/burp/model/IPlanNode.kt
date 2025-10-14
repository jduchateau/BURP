package burp.model

import kotlin.reflect.KClass

interface IPlanNode {
    var parent: IPlanNode?
    //fun children(): List<IPlanNode>

    var origin: RmlOrigin?

    fun ancestors(): List<IPlanNode> {
        val ancestors = mutableListOf<IPlanNode>()
        var current: IPlanNode? = this.parent
        while (current != null) {
            ancestors.add(current)
            current = current.parent
        }
        return ancestors
    }

    fun <T : IPlanNode> ancestorOfType(clazz: KClass<T>, includeCurrent: Boolean = false): T? {
        var current: IPlanNode? = if (includeCurrent) this else parent
        while (current != null) {
            if (clazz.isInstance(current)) {
                @Suppress("UNCHECKED_CAST")
                return current as T
            }
            current = current.parent
        }
        return null
    }
}