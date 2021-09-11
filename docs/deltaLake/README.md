## Databricks Integration

*Combining best of open data standards with open source technologies*

___

Part of the Linux foundation, [Delta Lake](https://delta.io/) is an open source storage layer that brings reliability to data lakes. 
Delta Lake provides ACID transactions, scalable metadata handling, and unifies streaming and batch data processing. 
Running on top of your existing data lake and fully compatible with the Apache Spark APIs, Delta brings the best of both 
data warehousing and data lakes functionalities into one unified platform. 

### Usage

Create a new database source of type `DeltaLake`, providing both a Datasource specification and authentication.
For JDBC connection details, please refer to your Databricks environment and create a personal access token as per below screenshot.

![endpoint](images/endpoint_jdbc.png)

The same can be reported back on the legend studio as follows

![legend](images/database.png)

See [pure](deltaLakeModel.pure) model for reference. Finally, users can query data from Delta Lake directly through 
the comfort of the legend studio interface.

![legend](images/query_builder.png)

### Configuration

Spark JDBC driver is not OSS and as such not available through maven central. Please download JDBC driver from Databricks
[website](https://databricks.com/spark/jdbc-drivers-download) and extract jar file to a specific location.
Start the legend engine by appending classpath with `/path/to/SparkJDBC42.jar`.

```shell script
java -cp \
  /path/to/SparkJDBC42.jar:legend-engine-server/target/legend-engine-server-2.35.2-SNAPSHOT-shaded.jar \
  org.finos.legend.engine.server.Server \
  server \
  config.json
```

