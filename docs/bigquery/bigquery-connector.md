# BigQuery Connector 

## SQL Considerations
__Dialect__

Big Query supports two dialects : Legacy and standard. Standard SQL is supposed to comply with the SQL 2011 standard. https://cloud.google.com/bigquery/docs/reference/standard-sql/migrating-from-legacy-sql 

__Object naming conventions__
 
* Dataset Name - Dataset names cannot contain spaces and other special chars (expect underscores)
* Table Name - Table names can contain spaces and other special chars
* Column Name - Column names cannot contain spaces and other special chars
 
__Quoted Identifiers__

To support the use of tables whose names contain special chars, we quote identifier names using backticks.

See https://cloud.google.com/bigquery/docs/reference/standard-sql/lexical

## Client Library
Legend supports BigQuery as a relational store via the BigQuery Simba JDBC Driver. See https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers 

## Database Authentication
Legend supports the following authentication modes :

__Application Default Credentials__

Legend connects to BigQuery using GCP Application Default Credentials. This means that BigQuery JDBC driver is able to read service account credentials provided by GCP runtimes like GKE.

This authentication mode is usable only when Legend executes in a GCP environment.

See the following docs for details :
* https://cloud.google.com/bigquery/docs/authentication
* https://cloud.google.com/docs/authentication/production 

__Workload Identity Federation__

Legend can connect to BigQuery using Workload Identity Federation. This means that BigQuery JDBC driver will require any valid Service Account Access Token obtained using configured authentication flow during runtime to connect to BigQuery.

This authentication mode is usable in any environment.

See the following docs for details :
* https://cloud.google.com/iam/docs/workload-identity-federation

## Simba BiqQuery Driver 

Legend uses the Simba BigQuery JDBC Driver. However, this driver is not open source software.

Use of the Simba driver is subject to commercial license agreements between GCP, Simba and the customer using the JDBC driver.

For this reason, Legend does not have a compile time dependency of the Simba driver. Legend users will have to make the Simba JDBC driver available in the CLASSPATH.

See the following docs for details :
* https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers 

