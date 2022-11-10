# Persistence

**Persistence** is a specification for writing data output by a `Service` to one or more target sinks.

The specification can loosely be thought of as a representation of an Extract, Transform, Load (ETL) pipeline.
In this mental model, `Service` handles the Extract and Transform, while Load details and other concerns that don't
neatly fit the acronym (e.g. triggering execution) are covered by the rest of the specification.

Note: this is only a loose correlation. While execution of a **Persistence** specification may be delegated to a
pipeline-ing platform, it also may be handled by Engine itself or be pushed all the way down to source
(e.g. creation of a materialized view to handle a database-to-database specification).

## Concepts

**Persistence** is overloaded shorthand for two `PackageableElements`: `Persistence` and `PersistenceRuntime`.

`Persistence` is a *template* for a data movement definition. It includes the following components:
* A `Trigger` that specifies how data extraction via `Service` kicks off. For example, a `Trigger` might specify execution on a fixed schedule, e.g. a cron expression.
Or it might specify a dependency on completion of an upstream `Persistence` specification (process dependency) or population of one or more upstream datasets (data dependency).
As a final example, a `Trigger` might specify continuous execution with infinite data as input, e.g. as part of a Change Data Capture (CDC) flow.
* A `Service` that defines how to extract data from source and--optionally--transform it. The `Service` may be parameterized with open variables, but the `Persistence` specification doesn't provide concrete values for those parameters.
* One or more `ServiceDatasetMapping`s that each consist of two parts
  * A `ServiceDataset` that describes a section (or possibly all) of the data output by the service, e.g.
    * Does the data represent a full snapshot of the target dataset or only deltas? If snapshot, is there partitioning?
    * Are some parts of the data actually metadata (e.g. event time, version information, or action directives)?
  * A `Target` that describes the physical sink to which the `ServiceDataset` will be written
    * What is the type of the sink, e.g. a relational database, MongoDB, S3, etc?
    * What is the specific target within the sink, e.g. the database table, or MongoDB collection
    * What capabilities of the target sink should we leverage when writing data, e.g. does the sink support temporality, e.g. a milestoned database table or versioned MongoDB collection? 
* A `Notifier` configuration for channels that should receive significant events e.g. PagerDuty or email distribution lists

`Persistence` is independent of
* Execution platform
* Runtime environment
* Service parameters

**PersistenceRuntime**

Each `PersistenceRuntime` references a `Persistence` and instantiates it by populating the template with concrete values:
* Execution platform includes the backend system that will do the execution and additional parameters that platform requires 
* Runtime environment includes information to connect specific datasources (e.g. a JDBC information)
* Service parameters include concrete values (or instructions on how to get concrete values) to bind to open variables in a `Service`

Many `PersistenceRuntime` instances may point to the same `Persistence` definitions. An example would be to use separate instances
to define non-prod + prod configurations for a given data transfer.
