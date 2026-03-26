package burp.model

interface PlanNode {
    var parent: PlanNode?
    val dependents: MutableSet<PlanNode>
        get() = mutableSetOf()

    fun children(): Sequence<PlanNode>
    fun dependencies(): Sequence<PlanNode>
    fun dependents(): Sequence<PlanNode> = dependents.asSequence()

    fun <T : PlanNode> ancestor(clazz: Class<T>): T? {
        var p = parent
        while (p != null) {
            if (clazz.isInstance(p)) {
                return clazz.cast(p)
            }
            p = p.parent
        }
        return null
    }

    fun <T : PlanNode> descendants(clazz: Class<T>): Sequence<T> = sequence {
        val children = children().toList()
        println("${this@PlanNode} has children: $children")
        for (child in children) {
            if (clazz.isInstance(child)) {
                yield(clazz.cast(child))
            }
            yieldAll(child.descendants(clazz))
        }
    }
}

inline fun <reified T : PlanNode> PlanNode.ancestor(): T? = ancestor(T::class.java)
inline fun <reified T : PlanNode> PlanNode.descendants(): Sequence<T> = descendants(T::class.java)