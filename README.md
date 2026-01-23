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

| RML module                                           | Coverage                  |
|------------------------------------------------------|---------------------------|
| [RML-Core](https://w3id.org/rml/core/spec)           | ✔️ 99% coverage           |
| [RML-IO](https://w3id.org/rml/io/spec)               | 🚧 Source yes, Target WIP | 
| [RML-IO-Registry](https://w3id.org/rml/io-registry/) | See below                 | 
| [RML-CC](https://w3id.org/rml/cc/spec)               | ✔️ 100% coverage          | 
| [RML-FNML](https://w3id.org/rml/fnml/spec)           | ✔️ 100% coverage          | 
| [RML-Star](https://w3id.org/rml/star/spec)           | ❌ Not implemented         | 
| [RML-LV](https://w3id.org/rml/lv/spec)               | ✔️ 99% coverage           |       
| [RER](https://w3id.org/dre/rer)                      | 🪅 Demo implementation    |

### RML-IO-Registry coverage details

BURP supports natively the following input sources:
- rml:FilePath or rml:RelativePathSource
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

TODO Sai that we can contribute functions and logical sources with additional JARS via ServiceLoader for interfaces in
burp.ls.LogicalSourceProvider and burp.model.fnmlutil.RMLFunction.

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
