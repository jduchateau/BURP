# BURP-Error: A Basic and Unassuming RML Processor with RML Execution Report

[![DOI](https://zenodo.org/badge/DOI//zenodo..svg)](https://doi.org//zenodo.)

BURP (Basic and Unassuming RML Processor) is a reference implementation for the
new [RML specification](http://w3id.org/rml/portal) which has been written from scratch to have no influence from prior
implementations of RML.
BURP was created to serve as a reference RML implementation for the Knowledge Graph Construction community and to verify
the RML specifications, their feasibility, and coverage of their test cases.

BURP-Error is a fork of BURP that adds error handling to the RML processor, according to
the [RML Execution Report](https://w3id.org/dre/rer). The project is a proof of concept, no maintenance garanteed, but
if you have any questions or issues, feel free
to [open an issue on GitHub](https://github.com/jduchateau/BURP/issues/new).

## Coverage matrix

| RML module                                           | Test Cases: Pass / Fail / Total       |
|------------------------------------------------------|---------------------------------------|
| [RML-Core](https://w3id.org/rml/core/spec)           | ✔️ 55 / 9 / 64                        |
| [RML-IO](https://w3id.org/rml/io/spec)               | 🚧 18 / 55 / 73 Source yes, Target no | 
| [RML-IO-Registry](https://w3id.org/rml/io-registry/) | ✔️ Details below                      | 
| [RML-CC](https://w3id.org/rml/cc/spec)               | ✅ 35 / 0 / 35                         | 
| [RML-FNML](https://w3id.org/rml/fnml/spec)           | 🚧️ 10 / 10 / 20                      | 
| [RML-Star](https://w3id.org/rml/star/spec)           | ❌ Not implemented                     | 
| [RML-LV](https://w3id.org/rml/lv/spec)               | ✅️ 41 / 0 / 41                        |       
| [RER](https://w3id.org/dre/rer)                      | 🪅 Demo implementation                |

### RML-IO-Registry coverage details

BURP supports natively the following input sources:

- rml:FilePath or rml:RelativePathSource — local files (supports rml:root and rml:path)
- rml:CSV — CSV files (including CSVW tables and their dialects: encoding, delimiter, header, nulls)
- rml:JSONPath — JSON sources ([RFC 9535](https://www.rfc-editor.org/rfc/rfc9535) JSONPath iterator)
- rml:XPath — XML sources (XPath 1.0 iterator; supports namespace/prefix mappings for XPath reference formulations)
- rml:SPARQL Results (CSV/TSV/XML/JSON) — SPARQL result files, SPARQL endpoints/services and data dumps (VOID/SD)
- rml:SQL2008Query and rml:SQL2008Table — relational database sources (via D2RQ properties such as d2rq:jdbcDSN, d2rq:
  jdbcDriver, username, password)
- DCAT Distribution / CSVW Table — remote files via DCAT downloadURL or CSVW url

- Extensions possible [see extending BURP](#extending-burp)

## Building BURP

To build the project, you will need Maven, Java, and Kotlin.
To package the project as a Fat-JAR (or Über-JAR) that includes all the dependencies, execute:

```shell
mvn package
```

The resulting JAR `burp.jar` will be located in the `target` folder.

To skip the tests, execute instead:

```shell
mvn package -DskipTests
```

The tests do rely on Docker for testing mappings on top of MySQL, PostgreSQL, and MSSQL.

### Updating shapes and test-cases

To update shapes and test cases from the specifications, execute the `FetchTestCases` command:

```
mvn -Dexec.mainClass=burp.tools.FetchTestCases exec:java
```

## Using BURP

The run the R2RML processor, execute the following command:

```bash
$ java -jar burp.jar [-h] [-b=<baseIRI>] -m=<mappingFile> [-o=<outputFile>]
```

A fat jar is also provided with the [Apache Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/).
It does not depend on the `dependency` folder.

```
Usage: burp [-h] [-b=<baseIRI>] -m=<mappingFile> [-o=<outputFile>]
  -b, --baseIRI=<baseIRI>   Used in resolving relative IRIs produced by the RML mapping
  -h, --help                Display a help message
  -m, --mappingFile=<mappingFile>
                            The RML mapping file
  -o, --outputFile=<outputFile>
                            The output file
```

If no outputFile is provided and the RML mapping does not rely on RML-IO for targets, then the output is written to the
standard output.

## Extending BURP

BURP can be extended by providing additional logical source providers (for new input sources) and custom RML-FNML
functions.
BURP discovers extensions on the classpath using Java's ServiceLoader mechanism.

**For complete documentation, see [ExtensionPoints.md](./ExtensionPoints.md)**

What you can extend:

- **Logical source providers** (`burp.ls.LogicalSourceProvider`) — add support for new reference formulations or custom
  source types
- **RML functions** (`burp.model.fnmlutil.RMLFunction`) — provide custom function behavior for FNML mappings

Quick example:

```bash
# Run BURP with your extension JAR on the classpath
java -cp "burp.jar:your-extension.jar" burp.Main -m mapping.ttl -o output.ttl
```

## Citation

If you use BURP-Error, please cite our paper:

```
@inproceedings{duchateau2026rml-execution-report,
  author       = {Jakub Duchateau and Dylan {Van Assche} and Christophe Debruyne},
  editor       = {},
  title        = {Beyond Exit Code 1: A Vocabulary for Execution Report of RML Processors (RER)},
  booktitle    = {Proceedings of the 7th International Workshop on Knowledge Graph Construction
                  co-located with 23rd Extended Semantic Web Conference ({ESWC} 2026),
                  Dubrovnik, Croatia, May 10, 2026},
  series       = {{CEUR} Workshop Proceedings},
  volume       = {X},
  publisher    = {CEUR-WS.org},
  year         = {2026},
  url          = {https://ceur-ws.org/Vol-X/paperX.pdf}
}
```

If you use BURP, please cite our paper:

```
@inproceedings{DBLP:conf/kgcw/AsscheD24,
  author       = {Dylan {Van Assche} and Christophe Debruyne},
  editor       = {David Chaves{-}Fraga and Anastasia Dimou and
                  Ana Iglesias{-}Molina and Umutcan Serles and
                  Dylan {Van Assche}},
  title        = {BURPing Through {RML} Test Cases},
  booktitle    = {Proceedings of the 5th International Workshop on Knowledge Graph Construction
                  co-located with 21th Extended Semantic Web Conference ({ESWC} 2024),
                  Hersonissos, Greece, May 27, 2024},
  series       = {{CEUR} Workshop Proceedings},
  volume       = {3718},
  publisher    = {CEUR-WS.org},
  year         = {2024},
  url          = {https://ceur-ws.org/Vol-3718/paper4.pdf}
}
```

## License

BURP is released under the [MIT license](./LICENSE).


