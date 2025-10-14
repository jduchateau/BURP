# BURP: A Basic and Unassuming RML Processor

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.11037712.svg)](https://doi.org/10.5281/zenodo.11037712)

BURP (Basic and Unassuming RML Processor) is a reference implementation for the
new [RML specification](http://w3id.org/rml/portal) which has been written from scratch to have no influence from prior
implementations of RML.
BURP was created to serve as a reference RML implementation for the Knowledge Graph Construction community and to verify
the RML specifications their feasibility and coverage of their test cases.

## Coverage matrix

<div style="display: flex; flex-direction: row; align-items: start; flex-wrap: wrap;">

| Module                                        | Coverage                  |
|:----------------------------------------------|:--------------------------|
| [**RML-Core**](http://w3id.org/rml/core/spec) | ✔️ 100% coverage          |
| [**RML-IO**](http://w3id.org/rml/io/spec)     | 🚧 Source yes, Target WIP |
| [**RML-CC**](http://w3id.org/rml/cc/spec)     | ✔️ 100% coverage          |
| [**RML-FNML**](http://w3id.org/rml/fnml/spec) | ✔️ 100% coverage          |
| [**RML-Star**](http://w3id.org/rml/star/spec) | 🚧 WIP                    |
| [**RML-LV**](https://w3id.org/rml/lv/spec/)   | 🚧 WIP                    |

| [RML-IO-Registry](https://w3id.org/rml/io-registry) Logical Sources           | Support                 | With Source                        |
|:------------------------------------------------------------------------------|:------------------------|------------------------------------|
| [JSONPath](http://w3id.org/rml/io-registry/json-path/spec)                    | ✔️ Supported, RFC9535   | FilePath                           |
| [XPath](http://w3id.org/rml/io-registry/xpath/spec)                           | ✔️ Supported, XPath 1.0 | FilePath                           |
| [CSV](http://w3id.org/rml/io-registry/csv/spec)                               | ✔️ Supported, RFC4180   | FilePath                           |
| [CSV on the Web (CSVW)](http://w3id.org/rml/io-registry/csvw/spec)            | 🚧 Partial              | FilePath                           |
| [SQL](http://w3id.org/rml/io-registry/sql/spec)                               | ✔️ Supported            | D2RQ:Database                      |
| SPARQL                                                                        | ✔️ Supported            | FilePath, VOID:Dataset, SD:Service |
| [Logical View](https://kg-construct.github.io/rml-lv/spec/docs/#logicalviews) | 🚧 WIP                  |                                    |

| [RML-IO-Registry](https://w3id.org/rml/io-registry) Sources          | Support                                                                               |
|:---------------------------------------------------------------------|:--------------------------------------------------------------------------------------|
| [D2RQ](http://w3id.org/rml/io-registry/d2rq/spec)                    | ✔️ Supported (by default, PostgreSQL, MySQL, MS SQL, SQLite, jdbc drivers are loaded) |
| [FilePath](http://w3id.org/rml/io-registry/file-path/spec)           | ✔️ Supported                                                                          |
| [W3C Web of Things (WOT)](http://w3id.org/rml/io-registry/wot/spec)  | 🚧 Not planned                                                                        |
| [W3C Data Catalog (DCAT)](http://w3id.org/rml/io-registry/dcat/spec) | 🚧 Not planned                                                                        |

</div>

## Building BURP

To build the project and copy its dependencies, execute

```bash
$ mvn package 
$ mvn dependency:copy-dependencies
```

You can add `-DskipTests` after `mvn package` to skip the unit tests. The tests do rely on Docker for testing mappings
on top of MySQL and PostgreSQL.

## Using BURP

The run the RML processor, execute the following command:

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

## Citation

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
