# TurtleProv

`TurtleProv` is a Kotlin Multiplatform library that parses RDF Turtle into quads and keeps provenance information (source node positions for subject, predicate, and object).

## Features

- Parse Turtle from string, stream, or file.
- Produce `ProvStore` containing:
    - `quads`: RDF quads wrapped as `ProvQuad`
    - `prefixes`: resolved namespace prefixes
- Keep provenance metadata (`NodeInfo`) for each triple component.
- Targets include JVM and JS (plus WASM in this project setup).

## Jena conversion

Conversion from `ProvStore` to Jena `Dataset` currently exists in `BURP-Error`.
If this is useful for your integration, please open an issue and it can be moved into this repository.

## Minimum requirements

- JDK 17 for JVM build/publish.

## Quick usage

```kotlin
import turtleprov.parseTurtleFromString

val turtle = """
	@prefix ex: <http://example.com/> .
	ex:alice ex:knows ex:bob .
""".trimIndent()

val store = parseTurtleFromString(turtle)
println(store.prefixes) // {ex=http://example.com/}

val first = store.quads.first()
println(first.quad)
println(first.subjectInfo)
println(first.predicateInfo)
println(first.objectInfo)
```

## Example input/output

Input Turtle:

```turtle
@prefix ex: <http://example.com/> .
ex:alice ex:age 42 .
```

Output (conceptual):

```text
quad: <http://example.com/alice> <http://example.com/age> "42"^^http://www.w3.org/2001/XMLSchema#integer DefaultGraph
subjectInfo: NodeInfo(kind=PREFIXED_NAME, start=Point(...), end=Point(...))
predicateInfo: NodeInfo(kind=PREFIXED_NAME, start=Point(...), end=Point(...))
objectInfo: NodeInfo(kind=INTEGER_LITERAL, start=Point(...), end=Point(...))
```
