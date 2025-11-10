package turtleprov.kotlin

import rdf.RDEV


enum class TurtleNodeKind(val uri: String) {
    // IRI kinds
    IRIREF("${RDEV.base_uri}IRIREF"),
    PREFIXED_NAME("${RDEV.base_uri}PrefixedName"),

    // Blank Node kinds
    BLANK_NODE_LABEL("${RDEV.base_uri}BlankNodeLabel"),
    ANONYMOUS_BLANK_NODE("${RDEV.base_uri}AnonymousBlankNode"),
    BLANK_NODE_PROPERTY_LIST("${RDEV.base_uri}BlankNodePropertyList"),

    // Collection
    COLLECTION("${RDEV.base_uri}Collection"),

    // be.uliege.rmldevtools.Literal kinds
    STRING_LITERAL_QUOTE("${RDEV.base_uri}StringLiteralQuote"),
    STRING_LITERAL_SINGLE_QUOTE("${RDEV.base_uri}StringLiteralSingleQuote"),
    STRING_LITERAL_LONG_QUOTE("${RDEV.base_uri}StringLiteralLongQuote"),
    STRING_LITERAL_LONG_SINGLE_QUOTE("${RDEV.base_uri}StringLiteralLongSingleQuote"),
    INTEGER_LITERAL("${RDEV.base_uri}IntegerLiteral"),
    DECIMAL_LITERAL("${RDEV.base_uri}DecimalLiteral"),
    DOUBLE_LITERAL("${RDEV.base_uri}DoubleLiteral"),
    BOOLEAN_LITERAL("${RDEV.base_uri}BooleanLiteral"),

    // Special verb
    TYPE_VERB("${RDEV.base_uri}TypeVerb"),

    // be.uliege.rmldevtools.RDF-Star kinds
    REIFIED_TRIPLE("${RDEV.base_uri}ReifiedTriple"),
    TRIPLE_TERM("${RDEV.base_uri}TripleTerm")
}