# Snowflake "Local Connection"

A "Local Connection" is a connection whose details are provided by the "runtime" environment.

Normally, a connection is expressed as follows :

```
RelationalDatabaseConnection connection::snowflake
{
  store: database::snowflake;
  type: Snowflake;
  specification: Snowflake
  {
    name: 'SUMMIT_DEV';
    account: 'ki79827';
    warehouse: 'SUMMIT_DEV';
    region: 'us-east-2';
    cloudType: 'aws';
    role: 'SUMMIT_DEV';
  };
  auth: SnowflakePublic
  {
    publicUserName: 'SUMMIT_DEV1';
    privateKeyVaultReference: 'snowflakePkRef';
    passPhraseVaultReference: 'snowflakePkPassphraseRef';
  };
}
```

In the "Local Connection" mode, the connection is expressed as follows :

```
RelationalDatabaseConnection connection::snowflake
{
  store: database::snowflake;
  type: Snowflake;
  mode: local;
}
```
In a local connection, the details about the database to connect to are provided by the runtime. 

# Runtime Environment

When the connection is marked as local, the connection details are obtained from the environment during JDBC connection creation time. 

The ```DatabaseManager``` provides the connection details. The  ```SnowflakeManager``` provides the details by reading a configuration file. 


