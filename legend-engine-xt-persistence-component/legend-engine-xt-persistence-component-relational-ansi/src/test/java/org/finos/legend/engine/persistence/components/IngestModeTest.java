// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngestModeTest
{
    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    protected String mainDbName = "mydb";
    protected String mainTableName = "main";
    protected String mainTableAlias = "sink";

    protected String stagingDbName = "mydb";
    protected String stagingTableName = "staging";
    protected String stagingTableAlias = "stage";

    protected String tempDbName = "mydb";
    protected String tempTableName = "temp";
    protected String tempTableAlias = "temp";

    protected String tempWithDeleteIndicatorDbName = "mydb";
    protected String tempWithDeleteIndicatorTableName = "tempWithDeleteIndicator";
    protected String tempWithDeleteIndicatorTableAlias = "tempWithDeleteIndicator";

    protected String digestField = "digest";
    protected String dataSplitField = "data_split";
    protected String batchUpdateTimeField = "batch_update_time";
    protected String batchIdInField = "batch_id_in";
    protected String batchIdOutField = "batch_id_out";
    protected String batchTimeInField = "batch_time_in";
    protected String batchTimeOutField = "batch_time_out";
    protected String deleteIndicatorField = "delete_indicator";
    protected String[] deleteIndicatorValues = new String[]{"yes", "1", "true"};
    protected Boolean[] deleteIndicatorBooleanValues = new Boolean[]{true};
    protected String validityFromReferenceField = "validity_from_reference";
    protected String validityThroughReferenceField = "validity_through_reference";
    protected String validityFromTargetField = "validity_from_target";
    protected String validityThroughTargetField = "validity_through_target";

    protected String[] partitionKeys = new String[]{"biz_date"};
    protected Map<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put("biz_date", new HashSet<>(Arrays.asList("2000-01-01 00:00:00", "2000-01-02 00:00:00")));
    }};
    protected String[] bitemporalPartitionKeys = new String[]{validityFromReferenceField};

    // Base Columns: Primary keys : id, name
    protected Field id = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field tinyIntId = Field.builder().name("id").type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field tinyIntString = Field.builder().name("id").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field nameModified = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, 64, null)).primaryKey(true).build();
    protected Field amount = Field.builder().name("amount").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    protected Field floatAmount = Field.builder().name("amount").type(FieldType.of(DataType.FLOAT, Optional.empty(), Optional.empty())).build();
    protected Field bizDate = Field.builder().name("biz_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();

    // Bitemporal Columns:
    protected Field validityFromReference = Field.builder().name(validityFromReferenceField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field validityThroughReference = Field.builder().name(validityThroughReferenceField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    protected Field validityFromTarget = Field.builder().name(validityFromTargetField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field validityThroughTarget = Field.builder().name(validityThroughTargetField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();

    // Problematic Columns
    protected Field idNonPrimary = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    protected Field nameNonPrimary = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    protected Field batchIdInNonPrimary = Field.builder().name(batchIdInField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    protected Field batchTimeInNonPrimary = Field.builder().name(batchTimeInField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();

    // Milestoning Columns
    protected Field digest = Field.builder().name(digestField).type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
    protected Field dataSplit = Field.builder().name(dataSplitField).type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field batchUpdateTime = Field.builder().name(batchUpdateTimeField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    protected Field batchIdIn = Field.builder().name(batchIdInField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field batchIdOut = Field.builder().name(batchIdOutField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    protected Field batchTimeIn = Field.builder().name(batchTimeInField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    protected Field batchTimeOut = Field.builder().name(batchTimeOutField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    protected Field deleteIndicator = Field.builder().name(deleteIndicatorField).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    protected Field deleteIndicatorBoolean = Field.builder().name(deleteIndicatorField).type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();

    protected List<DataSplitRange> dataSplitRanges = new ArrayList<DataSplitRange>()
    {{
        add(DataSplitRange.of(2, 5));
        add(DataSplitRange.of(6, 7));
        add(DataSplitRange.of(8, 10));
        add(DataSplitRange.of(11, 14));
    }};

    protected SchemaDefinition baseTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    protected SchemaDefinition baseTableShortenedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .build();

    protected SchemaDefinition stagingTableEvolvedSize = SchemaDefinition.builder()
        .addFields(id)
        .addFields(nameModified)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    protected SchemaDefinition stagingTableImplicitDatatypeChange = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(floatAmount)
        .addFields(bizDate)
        .build();

    protected SchemaDefinition stagingTableNonBreakingDatatypeChange = SchemaDefinition.builder()
        .addFields(tinyIntId)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    protected SchemaDefinition stagingTableBreakingDatatypeChange = SchemaDefinition.builder()
        .addFields(tinyIntString)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    protected SchemaDefinition mainTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .addFields(batchTimeIn)
        .addFields(batchTimeOut)
        .build();

    protected SchemaDefinition mainTableSchemaWithBatchIdInNotPrimary = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdInNonPrimary)
        .addFields(batchIdOut)
        .addFields(batchTimeIn)
        .addFields(batchTimeOut)
        .build();

    protected SchemaDefinition mainTableBatchIdBasedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .build();

    protected SchemaDefinition mainTableBatchIdBasedSchemaWithBatchIdInNotPrimary = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdInNonPrimary)
        .addFields(batchIdOut)
        .build();

    protected SchemaDefinition mainTableTimeBasedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchTimeIn)
        .addFields(batchTimeOut)
        .build();

    protected SchemaDefinition mainTableTimeBasedSchemaWithBatchTimeInNotPrimary = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchTimeInNonPrimary)
        .addFields(batchTimeOut)
        .build();

    protected SchemaDefinition baseTableSchemaWithDigest = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .build();

    protected SchemaDefinition baseTableSchemaWithNoPrimaryKeys = SchemaDefinition.builder()
        .addFields(idNonPrimary)
        .addFields(nameNonPrimary)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .build();

    protected SchemaDefinition baseTableSchemaWithDataSplit = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(dataSplit)
        .build();

    protected SchemaDefinition baseTableSchemaWithDigestAndUpdateBatchTimeField = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchUpdateTime)
        .build();

    protected SchemaDefinition stagingTableSchemaWithLimitedColumns = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(digest)
        .build();

    protected SchemaDefinition stagingTableSchemaWithDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(deleteIndicator)
        .build();

    protected SchemaDefinition stagingTableSchemaWithDeleteIndicatorWithDataSplit = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(deleteIndicator)
        .addFields(dataSplit)
        .build();

    protected SchemaDefinition stagingTableSchemaWithBooleanDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(deleteIndicatorBoolean)
        .build();

    protected SchemaDefinition bitemporalMainTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(validityThroughReference)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .addFields(validityFromTarget)
        .addFields(validityThroughTarget)
        .build();

    protected SchemaDefinition bitemporalFromOnlyMainTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .addFields(validityFromTarget)
        .addFields(validityThroughTarget)
        .build();

    protected SchemaDefinition bitemporalStagingTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(validityThroughReference)
        .addFields(digest)
        .build();

    protected SchemaDefinition bitemporalStagingTableSchemaWithDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(validityThroughReference)
        .addFields(digest)
        .addFields(deleteIndicator)
        .build();

    protected SchemaDefinition bitemporalFromOnlyStagingTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .build();

    protected SchemaDefinition bitemporalFromOnlyStagingTableSchemaWithDataSplit = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(dataSplit)
        .build();

    protected SchemaDefinition bitemporalFromOnlyStagingTableSchemaWithDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(deleteIndicator)
        .build();

    protected SchemaDefinition bitemporalFromOnlyStagingTableSchemaWithDeleteIndicatorWithDataSplit = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(deleteIndicator)
        .addFields(dataSplit)
        .build();

    protected SchemaDefinition bitemporalFromOnlyTempTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .addFields(validityFromTarget)
        .addFields(validityThroughTarget)
        .build();

    protected SchemaDefinition bitemporalFromOnlyTempTableWithDeleteIndicatorSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .addFields(validityFromTarget)
        .addFields(validityThroughTarget)
        .addFields(deleteIndicator)
        .build();

    protected String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
        "(\"table_name\" VARCHAR(255)," +
        "\"batch_start_ts_utc\" DATETIME," +
        "\"batch_end_ts_utc\" DATETIME," +
        "\"batch_status\" VARCHAR(32)," +
        "\"table_batch_id\" INTEGER)";

    protected String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
        "(\"TABLE_NAME\" VARCHAR(255)," +
        "\"BATCH_START_TS_UTC\" DATETIME," +
        "\"BATCH_END_TS_UTC\" DATETIME," +
        "\"BATCH_STATUS\" VARCHAR(32)," +
        "\"TABLE_BATCH_ID\" INTEGER)";

    protected String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    protected String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    protected String expectedMetadataTableIngestQueryWithPlaceHolders = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TS_PATTERN}','{BATCH_END_TS_PATTERN}','DONE')";

    protected String expectedStagingCleanupQuery = "DELETE FROM \"mydb\".\"staging\" as stage";

    protected String expectedDropTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"staging\"";

    protected String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
        "(\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER," +
        "\"batch_id_out\" INTEGER," +
        "\"batch_time_in\" DATETIME," +
        "\"batch_time_out\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"batch_id_in\", \"batch_time_in\"))";

    protected String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER," +
        "\"NAME\" VARCHAR," +
        "\"AMOUNT\" DOUBLE," +
        "\"BIZ_DATE\" DATE," +
        "\"DIGEST\" VARCHAR," +
        "\"BATCH_ID_IN\" INTEGER," +
        "\"BATCH_ID_OUT\" INTEGER," +
        "\"BATCH_TIME_IN\" DATETIME," +
        "\"BATCH_TIME_OUT\" DATETIME," +
        "PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\", \"BATCH_TIME_IN\"))";

    protected String expectedMainTableBatchIdBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER,\"name\" VARCHAR,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER,\"batch_id_out\" INTEGER,PRIMARY KEY (\"id\", \"name\", \"batch_id_in\"))";

    protected String expectedMainTableBatchIdBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER,\"NAME\" VARCHAR,\"AMOUNT\" DOUBLE,\"BIZ_DATE\" DATE,\"DIGEST\" VARCHAR," +
        "\"BATCH_ID_IN\" INTEGER,\"BATCH_ID_OUT\" INTEGER,PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_ID_IN\"))";

    protected String expectedMainTableTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER,\"name\" VARCHAR,\"amount\" DOUBLE,\"biz_date\" DATE,\"digest\" VARCHAR," +
        "\"batch_time_in\" DATETIME,\"batch_time_out\" DATETIME,PRIMARY KEY (\"id\", \"name\", \"batch_time_in\"))";

    protected String expectedMainTableTimeBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER,\"NAME\" VARCHAR,\"AMOUNT\" DOUBLE,\"BIZ_DATE\" DATE,\"DIGEST\" VARCHAR," +
        "\"BATCH_TIME_IN\" DATETIME,\"BATCH_TIME_OUT\" DATETIME,PRIMARY KEY (\"ID\", \"NAME\", \"BATCH_TIME_IN\"))";

    protected String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "PRIMARY KEY (\"id\", \"name\"))";

    protected String expectedSchemaEvolutionAddColumn = "ALTER TABLE \"mydb\".\"main\" ADD COLUMN \"biz_date\" DATE";

    protected String expectedSchemaEvolutionAddColumnWithUpperCase = "ALTER TABLE \"MYDB\".\"MAIN\" ADD COLUMN \"BIZ_DATE\" DATE";

    protected String expectedSchemaEvolutionModifySize = "ALTER TABLE \"mydb\".\"main\" ALTER COLUMN \"name\" VARCHAR(64) PRIMARY KEY";

    protected String expectedSchemaEvolutionModifySizeWithUpperCase = "ALTER TABLE \"MYDB\".\"MAIN\" ALTER COLUMN \"NAME\" VARCHAR(64) PRIMARY KEY";

    protected String expectedSchemaNonBreakingChange = "ALTER TABLE \"mydb\".\"main\" ALTER COLUMN \"id\" TINYINT PRIMARY KEY";

    protected String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER," +
        "\"NAME\" VARCHAR," +
        "\"AMOUNT\" DOUBLE," +
        "\"BIZ_DATE\" DATE," +
        "PRIMARY KEY (\"ID\", \"NAME\"))";

    protected String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "PRIMARY KEY (\"id\", \"name\"))";

    protected String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"(" +
        "\"ID\" INTEGER," +
        "\"NAME\" VARCHAR," +
        "\"AMOUNT\" DOUBLE," +
        "\"BIZ_DATE\" DATE," +
        "\"DIGEST\" VARCHAR," +
        "PRIMARY KEY (\"ID\", \"NAME\"))";

    protected String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"(" +
        "\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"biz_date\" DATE," +
        "\"digest\" VARCHAR," +
        "\"batch_update_time\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\"))";

    protected String expectedStagingDigestUpdateQuery = "UPDATE \"mydb\".\"staging\" as stage SET " +
        "stage.\"digest\" = MD5(CONCAT(stage.\"id\",stage.\"name\",stage.\"amount\"," +
        "stage.\"biz_date\",stage.\"digest\"))";

    protected String expectedBitemporalMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
        "(\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"validity_from_reference\" DATETIME," +
        "\"validity_through_reference\" DATETIME," +
        "\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME," +
        "\"validity_through_target\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"batch_id_in\", \"validity_from_target\"))";

    protected String expectedBitemporalFromOnlyMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
        "(\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"validity_from_reference\" DATETIME," +
        "\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME," +
        "\"validity_through_target\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"batch_id_in\", \"validity_from_target\"))";

    protected String expectedBitemporalMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER," +
        "\"NAME\" VARCHAR," +
        "\"AMOUNT\" DOUBLE," +
        "\"VALIDITY_FROM_REFERENCE\" DATETIME," +
        "\"VALIDITY_THROUGH_REFERENCE\" DATETIME," +
        "\"DIGEST\" VARCHAR," +
        "\"BATCH_ID_IN\" INTEGER," +
        "\"BATCH_ID_OUT\" INTEGER," +
        "\"VALIDITY_FROM_TARGET\" DATETIME," +
        "\"VALIDITY_THROUGH_TARGET\" DATETIME," +
        "PRIMARY KEY (\"ID\", \"NAME\", \"VALIDITY_FROM_REFERENCE\", \"BATCH_ID_IN\", \"VALIDITY_FROM_TARGET\"))";

    protected String expectedBitemporalFromOnlyMainTableCreateQueryUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
        "(\"ID\" INTEGER," +
        "\"NAME\" VARCHAR," +
        "\"AMOUNT\" DOUBLE," +
        "\"VALIDITY_FROM_REFERENCE\" DATETIME," +
        "\"DIGEST\" VARCHAR," +
        "\"BATCH_ID_IN\" INTEGER," +
        "\"BATCH_ID_OUT\" INTEGER," +
        "\"VALIDITY_FROM_TARGET\" DATETIME," +
        "\"VALIDITY_THROUGH_TARGET\" DATETIME," +
        "PRIMARY KEY (\"ID\", \"NAME\", \"VALIDITY_FROM_REFERENCE\", \"BATCH_ID_IN\", \"VALIDITY_FROM_TARGET\"))";

    protected String expectedBitemporalFromOnlyTempTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"temp\"" +
        "(\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"validity_from_reference\" DATETIME," +
        "\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME," +
        "\"validity_through_target\" DATETIME," +
        "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"batch_id_in\", \"validity_from_target\"))";

    protected String expectedBitemporalFromOnlyTempTableWithDeleteIndicatorCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"tempWithDeleteIndicator\"" +
        "(\"id\" INTEGER," +
        "\"name\" VARCHAR," +
        "\"amount\" DOUBLE," +
        "\"validity_from_reference\" DATETIME," +
        "\"digest\" VARCHAR," +
        "\"batch_id_in\" INTEGER," +
        "\"batch_id_out\" INTEGER," +
        "\"validity_from_target\" DATETIME," +
        "\"validity_through_target\" DATETIME," +
        "\"delete_indicator\" VARCHAR," +
        "PRIMARY KEY (\"id\", \"name\", \"validity_from_reference\", \"batch_id_in\", \"validity_from_target\"))";

    protected String getExpectedCleanupSql(String fullName, String alias)
    {
        return String.format("DELETE FROM %s as %s", fullName, alias);
    }

    public void assertIfListsAreSameIgnoringOrder(List<String> first, List<String> second)
    {
        assertTrue(first.size() == second.size() &&
            first.stream().sorted().collect(Collectors.toList())
                .equals(second.stream().sorted().collect(Collectors.toList())));
    }
}
