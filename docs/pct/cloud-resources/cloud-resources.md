# Cloud Resources

Currently, the Snowflake and Databricks PCT tests require connectivity to external cloud resources.

These resources require secrets to be able to establish a connection.

During the GitHub workflows, the jobs get these secrets using AWS Secret Manager.

But how local developers can run these? 

_**The steps below assume developers have personal access to these resources**_

## Create a cloud PCT properties file

The first step is to create a properties file like below, replacing `{}` wit the correct value for your personal access.

```properties
# Snowflake

snowflake.spec.accountName={}
snowflake.spec.warehouseName={}
snowflake.spec.databaseName={}
snowflake.spec.role={}
snowflake.spec.region=us-east-1
snowflake.spec.cloudType=aws

snowflake.auth.publicUserName={}
# These are secrets, and should be careful on sharing
snowflake.auth.privateKey={}
snowflake.auth.passPhrase={}

#Dataricks

databricks.spec.hostname={}
databricks.spec.port=443
databricks.spec.protocol=https
databricks.spec.httpPath={}

# These are secrets, and should be careful on sharing
databricks.auth.apiToken={}
```

## Running on PURE IDE Light

When starting the PURE IDE Light process, pass the following system property with the location of the file created above:

`-Dpct.external.resources.properties={location of file}`

After this, execute any PCT test using the cloud resource PCT adapter, and it should connect and work as expected. 

## Running individual JUnit test cases 

When executing a PCT test for one of these cloud resources, pass the following system property with the location of the file created above:

`-Dpct.external.resources.properties={location of file}`

## Running Maven Surefire

When executing the cloud resource maven module, you will need to set the resource file as an environment variable:

`PCT_EXTERNAL_RESOURCES_PROPERTIES={location of file}`

Then, you will need to enable the `pct-test` maven profile.

After this, the cloud resources PCT test cases should execute using Maven Surefire. 