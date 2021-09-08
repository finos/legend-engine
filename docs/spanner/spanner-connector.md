# BigQuery Connector 

## SQL Considerations
__Dialect__

Spanner supports a GCP dialect called Standard SQL.
```
From "Spanner : Becoming a SQL system - https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46103.pdf"

The Spanner query processor implements a dialect of SQL,
called Standard SQL, that is shared by several query subsystems
within Google (e.g., Dremel/BigQuery OLAP systems2
). Standard
SQL is based on standard ANSI SQL, fully using standard features
such as ARRAY and row type (called STRUCT) to support nested
data as a first class citizen (see Section 6 for more details).
```

Though this dialect is supposed to be the same across GCP services like BigQuery and Spanner, it is not the same.

For e.g BigQuery [1] supports `SUBSTR` and `SUBSTRING` functions while Spanner supports only `SUBSTR` [2]

[1] https://cloud.google.com/bigquery/docs/reference/standard-sql/string_functions#substring 
[2] https://cloud.google.com/spanner/docs/string_functions#substr

__Database schema__

Spanner does not have a notion of "database schema". All tables in a database exist in an unnamed schema[3].

[3] https://stackoverflow.com/questions/42537046/named-schema-in-google-cloud-spanner

Legend asserts that a 'database' specification when used with Spanner cannot use a non default schema.

```
# The following is valid 

Database meta::relational::spanner::tests::db::spannerDb
(
   Table "persontable" (ID INT PRIMARY KEY, firstName VARCHAR(200), lastName VARCHAR(200), age INT, key INT)
)

# The following is invalid

Database meta::relational::spanner::tests::db::spannerDb
(
   Schema database1
   (
      Table "thingtable" (ID INT PRIMARY KEY, NAME VARCHAR(200))
   ) 
)
```

__Object naming conventions__
  
* Table Name - Table names cannot contain spaces. Backticks used for quouting special chars. See https://cloud.google.com/spanner/docs/data-definition-language#naming_conventions 
* Column Name - Column names cannot contain spaces and other special chars
 
__Quoted Identifiers__

To support the use of tables whose names contain special chars, we quote identifier names using backticks. See https://cloud.google.com/spanner/docs/lexical 

## Client Library
Legend supports BigQuery as a relational store via the BigQuery Simba JDBC Driver. See https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers 

## Database Authentication
Legend supports the following authentication modes :

__Application Default Credentials__

Legend connects to Spanner using GCP Application Default Credentials. This means that Spanner JDBC driver is able to read service account credentials provided by GCP runtimes like GKE.

This authentication mode is usable only when Legend executes in a GCP environment.

See the following docs for details :
* https://cloud.google.com/spanner/docs/getting-started/set-up#set_up_authentication_and_authorization
* https://cloud.google.com/docs/authentication/production 
* https://github.com/googleapis/google-cloud-java#authentication

## Spanner JDBC Driver 

Legend uses the Spanner OSS JDBC Driver.

See the following docs for details :
* https://cloud.google.com/spanner/docs/jdbc-drivers
* https://cloud.google.com/spanner/docs/open-source-jdbc

