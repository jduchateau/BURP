package turtleprov

import burp.vocabularies.BURP


enum class TurtleNodeKind(val uri: String) {
    // IRI kinds
    IRIREF("${BURP.base_uri}IRIREF"),
    PREFIXED_NAME("${BURP.base_uri}PrefixedName"),

    // Blank Node kinds
    BLANK_NODE_LABEL("${BURP.base_uri}BlankNodeLabel"),
    ANONYMOUS_BLANK_NODE("${BURP.base_uri}AnonymousBlankNode"),
    BLANK_NODE_PROPERTY_LIST("${BURP.base_uri}BlankNodePropertyList"),

    // Collection
    COLLECTION("${BURP.base_uri}Collection"),

    // be.uliege.rmldevtools.Literal kinds
    STRING_LITERAL_QUOTE("${BURP.base_uri}StringLiteralQuote"),
    STRING_LITERAL_SINGLE_QUOTE("${BURP.base_uri}StringLiteralSingleQuote"),
    STRING_LITERAL_LONG_QUOTE("${BURP.base_uri}StringLiteralLongQuote"),
    STRING_LITERAL_LONG_SINGLE_QUOTE("${BURP.base_uri}StringLiteralLongSingleQuote"),
    INTEGER_LITERAL("${BURP.base_uri}IntegerLiteral"),
    DECIMAL_LITERAL("${BURP.base_uri}DecimalLiteral"),
    DOUBLE_LITERAL("${BURP.base_uri}DoubleLiteral"),
    BOOLEAN_LITERAL("${BURP.base_uri}BooleanLiteral"),

    // Special verb
    TYPE_VERB("${BURP.base_uri}TypeVerb"),

    // be.uliege.rmldevtools.RDF-Star kinds
    REIFIED_TRIPLE("${BURP.base_uri}ReifiedTriple"),
    TRIPLE_TERM("${BURP.base_uri}TripleTerm")
}