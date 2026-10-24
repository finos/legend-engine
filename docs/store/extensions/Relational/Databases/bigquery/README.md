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
Legend supports BigQuery as a relational store via Google's BigQuery JDBC driver
(`com.google.cloud:google-cloud-bigquery-jdbc`). See https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers 

## Database Authentication
Legend supports the following authentication modes :

__Application Default Credentials__

Legend connects to BigQuery using GCP Application Default Credentials. This means that BigQuery JDBC driver is able to read service account credentials provided by GCP runtimes like GKE.

This authentication mode is usable only when Legend executes in a GCP environment.

See the following docs for details :
* https://cloud.google.com/bigquery/docs/authentication
* https://cloud.google.com/docs/authentication/production 

__Service Account Key__

Legend connects with a GCP service account key held in a vault, supplying the account email and
private key to the driver directly. Unlike Application Default Credentials this needs nothing present
in the ambient environment, so it works wherever Legend runs.

See the following docs for details :
* https://cloud.google.com/iam/docs/service-account-creds

__Workload Identity Federation__

Legend supports Workload Identity Federation using AWS as identity provider to connect to BigQuery. This means that BigQuery JDBC driver will require any valid Service Account Access Token obtained using configured authentication flow during runtime to connect to BigQuery.

See the following docs for details :
* https://cloud.google.com/iam/docs/workload-identity-federation

## BigQuery JDBC Driver

Legend depends on Google's BigQuery JDBC driver, which is Apache-2.0 licensed and published to Maven
Central, so no driver has to be supplied on the CLASSPATH.

This replaces the Simba BigQuery JDBC driver, which Legend previously named but could not depend on:
it is not open source, and its use is subject to commercial license agreements between GCP, Simba and
the customer. Deployments still wanting Simba can set `BigQueryDriver.DRIVER_CLASSNAME` back to
`com.simba.googlebigquery.jdbc.Driver` and put that jar on the CLASSPATH; the connection properties
Legend emits are the same for both.

See the following docs for details :
* https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers 

