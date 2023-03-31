# Lineage

## Background

Exposing and understanding data lineage can be important for several reasons:
- Regulatory or compliance requirements, to show traceability of where data is originating from
- Operational visibility, to establish the blast radius of incidents or quality issues on source datasets
- Change management activities, that require identifying of downstream datasets that may be impacted by a proposed change

## Legend Lineage Features

The same strong modelling capabilities that data model owners/providers use to define/create queryable/executable data 
models against a given store/source can be equally be leveraged to provide inferred/automated lineage traceability.

Legend has two main forms of lineage 
* [Model lineage](#model-lineage) 
* [Store lineage](#store-lineage)

Lineage calculations generally require `Service` or query function, a `Mapping` and a `Runtime`

### Model Lineage

Provides details on the model classes & properties used within the query / service.    

### Store Lineage

Provides details on the `Store` features used in the query/mapping.  For example, if this is a
relational / database `Store`, then this provides the link between the output columns and the 
columns used from the specific database tables.  This can provide "dataset level lineage" or "attribute level lineage" 
between the data model / service and the physical database. 

N.B. When calculating Store lineage a `Runtime` is required for accurate results as the `Connection` can for example 
contain things like `SchemaMapper`s (which can redirect / re-map the logical table name to a real world physical table 
name) 

## Implementation 

The main code modules are 
* [legend-engine-xt-analytics-lineage-pure](../../../legend-engine-xt-analytics-lineage-pure)
* [legend-engine-xt-analytics-lineage-generation](../../../legend-engine-xt-analytics-lineage-generation)
* [legend-engine-xt-analytics-lineage-api](../../../legend-engine-xt-analytics-lineage-api)

The main Pure method for the analysis / calculation is:
```pure
function meta::analytics::lineage::computeLineage(f:FunctionDefinition<Any>[1], m:Mapping[1], r:Runtime[0..1], extensions:meta::pure::extension::Extension[*]):FunctionAnalytics[1]
```
N.B. This returns a `meta::analytics::lineage::FunctionAnalytics` instance which contains each of the different
lineage analytics available.  There are other more specific methods available to calculate specific types individually. 
## APIs

The lineage methods are exposed on the Legend Engine service at `pure/v1/analytics/lineage`

| Method | Lineage Type | Description |
| --- | ----- | ----------- |
| /model/propertyPathTree | Model Lineage | Analyze the function to get property path tree |
| /model/class | Model Lineage | Analyze the function to get referenced model classes |
| /store/relational/database | Store Lineage | Analyze the function and mapping to get referenced databases and tables |
| /store/relational/reportColumn | Store Lineage |  Analyze the function and mapping to get referenced database columns for projected columns |
| /store/relational/relationTree | Store Lineage |  Analyze the function and mapping to get relation join tree |

## Usages / Examples

The Lineage methods are used in the [Search Analtyics](../search/search.md) module 