
# Setup

## Start Postgres

```
docker run -p 5432:5432 -e POSTGRES_PASSWORD=postgres -d postgres

# Check connectivity

psql --host=localhost --port=5432 --user=postgres
```

## Start MinIO

```
 docker run -p 9000:9000 -p 9001:9001 minio/minio server /data --console-address ":9001"
```

```
Create bucket 'iceberg' in MinIO
```

# Java

Run IcerbergDemo.java with these System envs

```
AWS_ACCESS_KEY_ID=minioadmin;AWS_SECRET_ACCESS_KEY=minioadmin
```

# Postgres

```

postgres=# \dt
                    List of relations
 Schema |             Name             | Type  |  Owner
--------+------------------------------+-------+----------
 public | iceberg_namespace_properties | table | postgres
 public | iceberg_tables               | table | postgres
(2 rows)

postgres=# select * from iceberg_namespace_properties;
 catalog_name | namespace | property_key | property_value
--------------+-----------+--------------+----------------
(0 rows)

postgres=# select * from iceberg_tables;

 catalog_name | table_namespace | table_name |                                     metadata_location                                      |                                 previous_metadata_location
--------------+-----------------+------------+--------------------------------------------------------------------------------------------+--------------------------------------------------------------------------------------------
 demo         | webapp          | logs       | s3://iceberg/webapp/logs/metadata/00002-d6649fc8-8857-43c5-981b-0d4438a19ded.metadata.json | s3://iceberg/webapp/logs/metadata/00001-3f5cd19c-f079-4439-a7b6-aa8f58e5c666.metadata.json
(1 row)

postgres=# SELECT  table_name, column_name, data_type FROM information_schema.columns WHERE table_name = 'iceberg_tables';
   table_name   |        column_name         |     data_type
----------------+----------------------------+-------------------
 iceberg_tables | catalog_name               | character varying
 iceberg_tables | table_namespace            | character varying
 iceberg_tables | table_name                 | character varying
 iceberg_tables | metadata_location          | character varying
 iceberg_tables | previous_metadata_location | character varying
(5 rows)

```

# Trino

```
docker run -p 8080:8080 -e AWS_ACCESS_KEY_ID=minioadmin -e AWS_SECRET_KEY=minioadmin --network host --volume $PWD/trino/core/docker/default/etc:/etc/trino --name trino trinodb/trino
```

```
stanle@DESKTOP-2LFD8N8:~$ docker exec -it trino trino
trino> show catalogs;
 Catalog
---------
 jmx
 memory
 sandbox
 system
 tpcds
 tpch
(6 rows)

Query 20230513_174332_00004_kdg9a, FINISHED, 1 node
Splits: 11 total, 11 done (100.00%)
0.23 [0 rows, 0B] [0 rows/s, 0B/s]

trino> show schemas in sandbox;
       Schema
--------------------
 information_schema
 webapp
(2 rows)

Query 20230513_174349_00005_kdg9a, FINISHED, 1 node
Splits: 11 total, 11 done (100.00%)
0.17 [2 rows, 34B] [11 rows/s, 204B/s]

trino> show tables in sandbox.webapp;
 Table
-------
 logs
(1 row)

Query 20230513_174404_00006_kdg9a, FINISHED, 1 node
Splits: 11 total, 11 done (100.00%)
0.13 [1 rows, 20B] [7 rows/s, 156B/s]

trino> select * from sandbox.webapp.logs;
  level  |           event_time           |               message               |                          call_stack
---------+--------------------------------+-------------------------------------+--------------------------------------------------------------
 warning | 2023-05-13 04:00:00.000000 UTC | You probably should not do this!    | [stack trace line 1, stack trace line 2, stack trace line 3]
 error   | 2023-05-13 04:00:00.000000 UTC | This was a fatal application error! | [stack trace line 1, stack trace line 2, stack trace line 3]
 info    | 2023-05-13 04:00:00.000000 UTC | Just letting you know!              | [stack trace line 1, stack trace line 2, stack trace line 3]
(3 rows)

Query 20230513_174414_00007_kdg9a, FINISHED, 1 node
Splits: 3 total, 3 done (100.00%)
0.21 [3 rows, 5.29KB] [14 rows/s, 24.8KB/s]

```

# REST Catalog

```
curl http://localhost:8181/v1/namespaces/
curl http://localhost:8181/v1/namespaces/webapp
curl http://localhost:8181/v1/namespaces/webapp/tables
curl http://localhost:8181/v1/namespaces/webapp/tables/logs 
```