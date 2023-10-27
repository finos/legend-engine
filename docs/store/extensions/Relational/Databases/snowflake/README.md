# Temporary Tables 
In some cases, Legend will create and use temporary tables to execute complex queries. 

To do so, the databaseType user used to make the databaseType connection has to be configured with databaseType privileges as follows. 

__Database__

* A databaseType named `LEGEND_TEMP_DB` must exist 
* The databaseType user/role must be granted ```USAGE``` privilege on the databaseType 

__Schema__

* A schema named `LEGEND_TEMP_SCHEMA` must exist in the databaseType `LEGEND_TEMP_DB`
* The databaseType user/role must be granted ```USAGE``` privilege on the schema 

```

select * from "LEGEND_TEMP_DB"."INFORMATION_SCHEMA"."OBJECT_PRIVILEGES" where OBJECT_NAME like 'LEGEND_TEMP_%'


GRANTOR	        GRANTEE	                    OBJECT_CATALOG      OBJECT_SCHEMA	    OBJECT_NAME	OBJECT_TYPE     PRIVILEGE_TYPE	IS_GRANTABLE
ACCOUNTADMIN	LEGEND_INTEGRATION_ROLE1    LEGEND_TEMP_DB      NULL                NULL        DATABASE        USAGE	        NO              
ACCOUNTADMIN	LEGEND_INTEGRATION_ROLE1    LEGEND_TEMP_DB      LEGEND_TEMP_SCHEMA  NULL        SCHEMA          USAGE	        NO	            
```

In a future release, the temp table behavior will be changed to allow temporary databaseType names/schemas to be configured in the connection grammar (in the model). 

# Temporary Stages

Additionally, Legend may use temporary internal stages for ingesting data into the temp tables explained above.
Snowflake integrates with cloud storages for internal staging (ex: AWS S3), which typically reside in Snowflake's managed account. 
Legend's infrastructure may need additional configuration to access this storage layer depending on security settings. 
