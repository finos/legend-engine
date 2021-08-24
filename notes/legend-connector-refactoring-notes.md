Refactoring
==================

### New module for connection "interfaces"

* legend-engine-executionPlan-execution-store-relational-connection-interfaces contains Java interfaces + classes that are not database specific.
* legend-engine-executionPlan-execution-store-relational-connection will be deleted

### New module per database 

E.g legend-engine-executionPlan-execution-store-relational-connection-datasource-snowflake

### New plugin interface

Every database(type) provides a RelationalConnectionPlugin via the ServiceLoader mechanism.

The plugin is the single entry point for all functionality for this database type. This includes :
* an API to get to a DatabaseManager
* an API to get to build a "specification" class given a datasource and auth description

### Database connection management centralized in a single class

Every database provides a single "specification" class that builds URLs, datasource properties etc. 

See [SnowflakeDataSourceSpecification](../legend-engine-executionPlan-execution-store-relational-connection-datasource-snowflake/src/main/java/org/finos/legend/engine/plan/execution/stores/relational/connection/ds/specifications/SnowflakeDataSourceSpecification.java)

If we want to split datasource code and auth code into different modules, we introduce a dependency cycle. i.e the auth code depends on the datasource and vice versa. 
So we pass in the auth strategy into the "specification" class.

```
  @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties)
    {
        if (this.authenticationStrategy instanceof UserPasswordAuthenticationStrategy) {
            UserPasswordAuthenticationStrategy userPasswordAuthenticationStrategy = (UserPasswordAuthenticationStrategy) authenticationStrategy;
            Properties connectionProperties = new Properties();
            connectionProperties.putAll(properties);
            connectionProperties.put("user", "fred");
            connectionProperties.put("password", userPasswordAuthenticationStrategy.getPassword());
            return Tuples.pair(url, connectionProperties);
        } 
        else if ( ...)
        {
            // do something else
        }
        else if ( ...)
        {
          // do something else
        }        
        return Tuples.pair(url, properties);
    }
```
### New module per authentication strategy 

E.g legend-engine-executionPlan-execution-store-relational-connection-authn-userpass

### Delete RelationalDatabaseCommandsVisitor 

The StreamResultToTempTableVisitor was causing dependency cycles.

Temp table handling etc is now pushed into RelationalDatabaseCommands. 

### Delete DataSourceIdentifiersCaseSensitiveVisitor 

Same idea as temp table handling. Push database specific code into the plugin and related classes.

```
    // RelationalExecutor 
    
    private void buildTransformersAndBuilder(ExecutionNode node, DatabaseConnection databaseConnection) throws SQLException
    {
        // TODO : epsstan : ask the data source about this ??
        RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) databaseConnection;
        RelationalConnectionPlugin relationalConnectionPlugin = new RelationalConnectionPluginLoader().getPlugin(relationalDatabaseConnection.databaseType);
        DataSourceSpecification dataSourceSpecification = relationalConnectionPlugin.buildDatasourceSpecification(relationalDatabaseConnection, new RelationalExecutorInfo());
        boolean isDatabaseIdentifiersCaseSensitive = dataSourceSpecification.isDatabaseIdentifiersCaseSensitive();
       ...
    }
```

Questions / Gotchas
==================
### Can we remove the datasource specification key classes ??

We use the datasource specification key to index user + database specific connections.

Can we introduce a method that returns a key in the protocol classes and remove the key classes ??

### Can we remove the authentication strategy key classes ??

Similarly can we remove the auth strategy key classes

### Overspecified/Long keys

In the example below, "quoteIdentifiers" does not uniquely identify the database

```
    @Override
    public String shortId()
    {
        return "Snowflake_" +
                "account:" + accountName + "_" +
                "region:" + region + "_" +
                "warehouse:" + warehouseName + "_" +
                "db:" + databaseName + "_" +
                "cloudType:" + cloudType + "_" +
                "quoteIdentifiers:" + quoteIdentifiers;
    }
```

### OAuth profiles
TODO 

### Plugin load order
TODO 