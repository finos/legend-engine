# Temporary Tables 
In some cases, Legend will create and use temporary tables to execute complex queries. 

To do so, the database user used to make the database connection has to be configured with database privileges as follows. 

__Database__

* A database named `LEGEND_TEMP_DB` must exist 
* The database user/role must be granted ```USAGE``` privilege on the database 

__Schema__

* A schema named `LEGEND_TEMP_SCHEMA` must exist in the database `LEGEND_TEMP_DB`
* The database user/role must be granted ```USAGE``` privilege on the schema 

```

select * from "LEGEND_TEMP_DB"."INFORMATION_SCHEMA"."OBJECT_PRIVILEGES" where OBJECT_NAME like 'LEGEND_TEMP_%'


GRANTOR	        GRANTEE	                    OBJECT_CATALOG      OBJECT_SCHEMA	    OBJECT_NAME	OBJECT_TYPE     PRIVILEGE_TYPE	IS_GRANTABLE
ACCOUNTADMIN	LEGEND_INTEGRATION_ROLE1    LEGEND_TEMP_DB      NULL                NULL        DATABASE        USAGE	        NO              
ACCOUNTADMIN	LEGEND_INTEGRATION_ROLE1    LEGEND_TEMP_DB      LEGEND_TEMP_SCHEMA  NULL        SCHEMA          USAGE	        NO	            
```

In a future release, the temp table behavior will be changed to allow temporary database names/schemas to be configured in the connection grammar (in the model). 

