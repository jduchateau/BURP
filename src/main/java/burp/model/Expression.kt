package burp.model

abstract class Expression : IPlanNode {
    override var parent: IPlanNode? = null
    override var origin: RmlOrigin? = null
}

