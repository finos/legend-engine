# ElasticSearch Legend Extension

## Introduction

This module includes the Pure metamodels, and functions to generate execution plans against an ElasticSearch store

## ElasticSearch Specification

The ElasticSearch specification it's used to generate the required protocol to generate an execution plan.  

More information on the specification can be found [here](https://github.com/elastic/elasticsearch-specification).

## How to add support for a new specification version?

1. Create a new directory (i.e `v2`) under `core_elasticsearch/protocols/elasticsearch`
2. Create a file for the generated code inside this directory: `generated_metamodel.pure`
3. Run the following code on the IDE light

```legend
    let esVersion = '8.3'; // version to generate
    let pureCode = $esVersion->meta::external::store::elasticsearch::metamodel::spec::toPure::generatePureCode(false);
    println($pureCode->joinStrings('\n\n'));
```

4. Copy the output into the filed created on step #2
5. Copy test cases from `v7` and modify accordingly
6. Incorporate into plan execution generation

## Todo

While responses are modeled, they are not use.  The current JSON deserialization functions cannot handle properly the response's model complexities.

