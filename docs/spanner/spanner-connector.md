# Spanner Connector

## SQL Considerations
__Dialect__

Spanner by itself supports two dialects : [Google Standard SQL and PostgreSQL](https://cloud.google.com/spanner/docs/postgresql-interface#choose).
<br>This connector so far uses PostgreSQL compatability, so, on database creation **PostgreSQL dialect** should be chosen. 
<br>

__Object naming conventions__

* [ProjectId](https://cloud.google.com/resource-manager/docs/creating-managing-projects#before_you_begin) - A globally unique identifier for your project
* [InstanceId]((https://cloud.google.com/spanner/docs/create-query-database-console#create-instance)) - When you first use Cloud Spanner, you must create an instance, which is an allocation of resources that are used by Cloud Spanner databases in that instance.
* [DatabaseId](https://cloud.google.com/spanner/docs/create-query-database-console#create-database) - Database name

## Client Library
Legend supports Spanner as a relational store via the [Google Google Cloud Spanner JDBC  Client for Java](https://cloud.google.com/spanner/docs/use-oss-jdbc)

## Database Authentication

Google Cloud Auth same as [Big Query](../bigquery/bigquery-connector.md#database-authentication)

