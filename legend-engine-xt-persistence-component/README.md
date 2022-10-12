# Legend Write Component

Component library for persisting data to a target.

[[_TOC_]]

## Getting started

### Prerequisites

1. Install Java 8+
2. Install maven 3.8.1

### Development Setup

1. Open *legend-engine-xt-persistence-component/pom.xml* as a project
2. Configure the project as a Maven project
3. Configure SDK - File > Project Structure > Project Settings > Project

## Test suite

The following tests are available for verifying the logic :

* Tests to verify the SQL's generated in ANSI format (module : *legend-engine-xt-persistence-component-relational-ansi*)
* Tests to verify the execution of the generated SQL's in an H2 executor (module : *legend-engine-xt-persistence-component-relational-h2*)
* Tests to verify the SQL's generated for Snowflake (module : *legend-engine-xt-persistence-component-relational-snowflake*)
* Tests to verify the SQL's generated for Memsql (module : *legend-engine-xt-persistence-component-relational-memsql*)

## Using it as a library

**Step 1:** Add the appropriate maven dependency.
Please [pick](https://mvnrepository.com/search?q=legend-engine-xt-persistence-component-)
the latest version from Maven Central.

#### Using the snowflake executor

    <dependency>
      <groupId>org.finos.legend.engine</groupId>
      <artifactId>legend-engine-xt-persistence-component-relational-snowflake</artifactId>
      <version>[PICK THE LATEST VERSION FROM MAVEN CENTRAL]</version>
    </dependency>

#### Using the H2 executor

    <dependency>
      <groupId>org.finos.legend.engine</groupId>
      <artifactId>legend-engine-xt-persistence-component-relational-h2</artifactId>
      <version>[PICK THE LATEST VERSION FROM MAVEN CENTRAL]</version>
    </dependency>

#### Using the Memsql executor

    <dependency>
      <groupId>org.finos.legend.engine</groupId>
      <artifactId>legend-engine-xt-persistence-component-relational-memsql</artifactId>
      <version>[PICK THE LATEST VERSION FROM MAVEN CENTRAL]</version>
    </dependency>

**Step 2:** Create the Ingest mode object based on ingestion scheme (Details and examples for each scheme [below](#ingest-modes))

    UnitemporalDelta ingestMode = UnitemporalDelta.builder()
       .digestField(digestField)
       .transactionMilestoning(BatchIdAndDateTime.builder()
            .batchIdInName(batchIdInField)
            .batchIdOutName(batchIdOutField)
            .dateTimeInName(batchTimeInField)
            .dateTimeOutName(batchTimeOutField)
            .build())
        .build();

**Step 3:** Provide the datasets to be used for ingestion

    // Provide the main and staging dataset 
    Datasets datasets = Datasets.of(mainTable, stagingTable);

    // Or provide main, staging and metadata dataset
    Datasets datasets = Datasets.of(mainTable, stagingTable).withMetadataDataset(metadataDataset);

    // Or provide main, staging and temp dataset (used only for bitemporal delta ingest mode)
    Datasets datasets = Datasets.of(mainTable, stagingTable).withTempDataset(tempDataset);

    // Or provide main, staging, temp and tempWithDeleteIndicator dataset (used only for bitemporal delta ingest mode)
    Datasets datasets = Datasets.of(mainTable, stagingTable).withTempDataset(tempDataset).withTempDatasetWithDeleteIndicator(tempDatasetWithDeleteIndicator);

**Step 4:** To extract the SQLs needed for ingestion, follow steps 4.1 and 4.2. 
If you do not need the SQL and want to use the executor to execute the SQLs for you, skip this step and jump to Step 5.

**Step 4.1:** Define a RelationalGenerator

    RelationalGenerator generator = RelationalGenerator.builder()
        .ingestMode(ingestMode)
        .relationalSink(SnowflakeSink.get())
        .cleanupStagingData(true)
        .build();

Mandatory Params:

| parameters          | Description                                                         |
|--------------------|---------------------------------------------------------------------|
| ingestMode | Ingest mode object defined in Step 2  |
| relationalSink | Choose the appropriate Sink where the data will be written to  |

Optional Params:

| parameters          | Description                                                         | Default Value |
|--------------------|---------------------------------------------------------------------|----------------|
| cleanupStagingData | clean staging table after completion of ingestion  | true |
| collectStatistics | Collect Statistics from ingestion  | false |
| enableSchemaEvolution | Enable Schema Evolution to happen  | false |
| caseConversion | Convert SQL objects like table, db, column names to upper or lower case.<br> Values supported - TO_UPPER, TO_LOWER, NONE  | NONE |
| executionTimestampClock | Clock to use to derive the time  | Clock.systemUTC() |
| batchStartTimestampPattern | Pattern for batchStartTimestamp. If this pattern is provided, it will replace the batchStartTimestamp values | None |
| batchEndTimestampPattern | Pattern for batchEndTimestamp. If this pattern is provided, it will replace the batchEndTimestamp values  | None |
| batchIdPattern | Pattern for batch id. If this pattern is provided, it will replace the next batch id | None |

**Step 4.2:** Use the generator object to extract the queries

    GeneratorResult operations = generator.generateOperations(datasets);
    List<String> preActionsSql = operations.preActionsSql(); // Pre actions: create tables
    Map<StatisticName, String> preIngestStatisticsSql = operations.preIngestStatisticsSql(); // Pre Ingest stats
    List<String> ingestSql = operations.ingestSql(); // milestoning sql
    Map<StatisticName, String> postIngestStatisticsSql = operations.postIngestStatisticsSql(); // post Ingest stats
    List<String> metadataIngestSql = operations.metadataIngestSql(); // insert batch into metadata table
    List<String> postActionsSql = operations.postActionsSql(); // post actions cleanup

NOTE 1: These queries must be strictly run in the order shown below.
1. preActionsSql - Creates tables
2. preIngestStatisticsSql - Collects pre ingest stats
3. ingestSql - Performs ingest/milestoning
4. postIngestStatisticsSql - Collects post ingest stats
5. metadataIngestSql - Inserts batch Id into metadata table
6. postActionsSql - Does clean up

NOTE 2: Statistics provided:     
1) INCOMING_RECORD_COUNT
2) ROWS_TERMINATED
3) ROWS_INSERTED
4) ROWS_UPDATED
5) ROWS_DELETED


**Step 5:** To use the executor to perform the ingestion for you, follow the steps in step 5. Skip this step if you just want the SQLs.

**Step 5.1:** Define a RelationalIngestor

    RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .enableSchemaEvolution(true)
            .build();

Mandatory Params:

| parameters          | Description                                                         |
|--------------------|---------------------------------------------------------------------|
| ingestMode | Ingest mode object defined in Step 2  |
| relationalSink | Choose the appropriate Sink where the data will be written to  |

Optional Params:

| parameters          | Description                                                         | Default Value |
|--------------------|---------------------------------------------------------------------|----------------|
| cleanupStagingData | clean staging table after completion of ingestion  | true |
| collectStatistics | Collect Statistics from ingestion  | true |
| enableSchemaEvolution | Enable Schema Evolution to happen  | false |
| caseConversion | Convert SQL objects like table, db, column names to upper or lower case.<br> Values supported - TO_UPPER, TO_LOWER, NONE  | NONE |
| executionTimestampClock | Clock to use to derive the time  | Clock.systemUTC() |

**Step 5.2:** Invoke the ingestion and get the stats

    IngestorResult result = ingestor.ingest(h2Sink.connection(), datasets);
    Map<StatisticName, Object> stats = result.statisticByName();

## Ingest Modes

### Non-temporal Snapshot

This is used to load data that is a complete refresh with each load. All rows from older batches are removed.

#### Object creation

    NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
            .auditing(NoAuditing.builder().build())
            .build();

#### Parameter information

| Field Name     | Description                                            | Mandatory? |
|----------------|--------------------------------------------------------|------------|
| auditing | Choose either one of these two: <br> 1. NoAuditing: no auditing <br> 2. DateTimeAuditing: A dateTimeField will be added to each row     | Yes        |

### Append-only

This is used to load data incrementally. No rows are invalidated, new rows are simply appended.

#### Object creation

    AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();


#### Parameter information

| Field Name     | Description                                                              | Mandatory?                          |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| digestField | Name of the digest field                                                    | No. Mandatory if deduplicationStrategy =  FilterDuplicates                                 | 
| dataSplitField | Name of the field which contains the data split number                                                    | No                               |
| deduplicationStrategy | Choose either one of these three: <br> 1. AllowDuplicates: allows duplicates to be appended <br> 2. FailOnDuplicates: pipeline to fail on duplicates <br> 3. FilterDuplicates: Filter out duplicates   | Yes                                  | 
| auditing | Choose either one of these two: <br> 1. NoAuditing: no auditing <br> 2. DateTimeAuditing: A dateTimeField will be added to each row     | Yes        |

### Nontemporal Delta

This is used to load data incrementally. New rows are simply appended, where-as the updated rows overwritten.

#### Object creation

    NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestName)
            .auditing(NoAuditing.builder().build())
            .build();

#### Parameter information

| Field Name     | Description                                                              | Mandatory?                          |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| digestField | Name of the digest field                                                    | Yes                                 | 
| auditing | Choose either one of these two: <br> 1. NoAuditing: no auditing <br> 2. DateTimeAuditing: A dateTimeField will be added to each row     | Yes        |

### Unitemporal Snapshot

This is used to load data that is a complete refresh with each load. It does not repeat data that does not change from
batch to batch, but it will repeat data that appears, disappears and then reappears between batches. No rows are ever
removed, but rows become invalid if they no longer exist in the loaded data.

There are two variants of this scheme:

- With Partition: With this option, only the rows in the main dataset matching the values of the partition columns in
  the incoming batch is affected by the milestoning. To use this option, you need to pass partitionFields.
- Without Partition: This is the default mode.

#### Object creation

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();


#### Parameter information

| Field Name     | Description                                                              | Mandatory?                          |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| digestField | Name of the digest field                                                    | Yes                                 | 
| transactionMilestoning | Choose either one of these three: <br>1. BatchId : Batch id based milestoning. It will populate batch_id_in and batch_id_out fields <br>2. TransactionDateTime: Transaction time based milestoning. It will populate batch_time_in and batch_time_out fields <br>3. BatchIdAndDateTime:  Batch id and time based milestoning. It will populate batch_id_in, batch_id_out, batch_time_in and batch_time_out fields | Yes |
| partitionFields | List of data partitioning columns. If provided, only the rows in the main dataset matching the values of the partitionFields in the incoming batch is affected by the milestoning                              | No                                                          |
| partitionValuesByField | This is an optional parameter the can be used for boosting performance. If the end user knows the values of the partition keys, they can provide a map of partition key and their values. This map will be used to efficiently filter in only necessary records while milestoning                                                | No                             |

### Unitemporal Delta

This scheme is used to load data incrementally. Each batch can append new data to the dataset. No rows are ever removed,
but rows that reappear in newer batches will invalidate those brought in by older batches.

There are two variants of this scheme:

- Without delete indicator: This mode does not support the concept of row deletion. This is the default mode.
- With delete indicator: This mode enables a row to be marked as deleted based on a "delete indicator flag". To use this mode, set the mergeStrategy as DeleteIndicatorMergeStrategy.
  A deleted row is identified by a delete indicator flag. 
  To use this flag, you need to provide deleteField (field name containing delete indicator flag) and deleteValues (delete indicator flag values) parameters
  
#### Object creation

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();


#### Parameter information

| Field Name     | Description                                                              | Mandatory?                          |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| digestField | Name of the digest field                                                    | Yes                                 | 
| dataSplitField | Name of the field which contains the data split number                                                    | No                               |
| transactionMilestoning | Choose either one of these three: <br>1. BatchId : Batch id based milestoning. It will populate batch_id_in and batch_id_out fields <br>2. TransactionDateTime: Transaction time based milestoning. It will populate batch_time_in and batch_time_out fields <br>3. BatchIdAndDateTime:  Batch id and time based milestoning. It will populate batch_id_in, batch_id_out, batch_time_in and batch_time_out fields | Yes |
| mergeStrategy | Choose either one of these two: <br>1. NoDeletesMergeStrategy: Default mode, this is without delete indicator mode. <br>2 DeleteIndicatorMergeStrategy: This is the delete indicator mode. User must provide deleteField and deleteValues in this case. | No                                                          |

### Bitemporal Snapshot

    WIP

#### Object creation

    WIP

### Bitemporal Delta

This scheme is used to load data incrementally, where the source data contains a validity time dimension. A validity time dimension consist of valid_from and valid_through fields.
The source can either contain both these fields or only contain valid_from field. In case source only provides valid_from field, the milestoning process will derive the valid_through field.
Each batch can append new data to the dataset. No rows are ever removed, but rows that reappear in newer batches will invalidate those brought in by older batches.

There are two variants of this scheme:
- Without delete indicator: This mode does not support the concept of row deletion. This is the default mode.
- With delete indicator: This mode enables a row to be marked as deleted based on a "delete indicator flag". To use this mode, set the mergeStrategy as DeleteIndicatorMergeStrategy.
  A deleted row is identified by a delete indicator flag. 
  To use this flag, you need to provide deleteField (field name containing delete indicator flag) and deleteValues (delete indicator flag values) parameters

#### Object creation

    BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .sourceDateTimeThruField(validityThroughReferenceField)
                                .build())
                        .build())
                .build();

#### Parameter information

| Field Name     | Description                                                              | Mandatory?                          |
|----------------|--------------------------------------------------------------------------|-------------------------------------|
| digestField | Name of the digest field                                                    | Yes                                 | 
| dataSplitField | Name of the field which contains the data split number                                                    | No                               |
| transactionMilestoning | Choose either one of these three: <br>1. BatchId : Batch id based milestoning. It will populate batch_id_in and batch_id_out fields <br>2. TransactionDateTime: Transaction time based milestoning. It will populate batch_time_in and batch_time_out fields <br>3. BatchIdAndDateTime:  Batch id and time based milestoning. It will populate batch_id_in, batch_id_out, batch_time_in and batch_time_out fields | Yes |
| validityMilestoning | For the ValidityDerivation object, choose either one of these two: <br>1. SourceSpecifiesFromAndThruDateTime : choose this when source data contains both validity from field and validity through field. <br>2. SourceSpecifiesFromDateTime: choose this when source data contains only validity from field. The dateTimeFromName species the column in the main dataset which will be populated with the validity from time; the dateTimeThruName species the column in the main dataset which will be populated with the validity through time.   | Yes |
| mergeStrategy | Choose either one of these two: <br>1. NoDeletesMergeStrategy: Default mode, this is without delete indicator mode. <br>2 DeleteIndicatorMergeStrategy: This is the delete indicator mode. User must provide deleteField and deleteValues in this case. | No                                                          |


