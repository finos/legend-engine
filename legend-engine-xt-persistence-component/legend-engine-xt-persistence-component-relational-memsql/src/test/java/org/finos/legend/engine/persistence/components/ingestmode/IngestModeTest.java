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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class IngestModeTest
{
    protected final ZonedDateTime fixedExecutionZonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedExecutionZonedDateTime.toInstant(), ZoneOffset.UTC);

    String mainDbName = "mydb";
    String mainTableName = "main";
    String mainTableAlias = "sink";

    String stagingDbName = "mydb";
    String stagingTableName = "staging";
    String stagingTableAlias = "stage";

    String digestField = "digest";
    String batchUpdateTimeField = "batch_update_time";
    String batchIdInField = "batch_id_in";
    String batchIdOutField = "batch_id_out";
    String batchTimeInField = "batch_time_in";
    String batchTimeOutField = "batch_time_out";
    String deleteIndicatorField = "delete_indicator";
    String[] deleteIndicatorValues = new String[]{"yes", "1", "true"};
    String validityFromReferenceField = "validity_from_reference";
    String validityThroughReferenceField = "validity_through_reference";
    String validityFromTargetField = "validity_from_target";
    String validityThroughTargetField = "validity_through_target";

    String[] primaryKeys = new String[]{"id", "name"};
    List<String> primaryKeysList = Arrays.asList(primaryKeys);
    String[] partitionKeys = new String[]{"biz_date"};
    HashMap<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put("biz_date", new HashSet<>(Arrays.asList("2000-01-01 00:00:00", "2000-01-02 00:00:00")));
    }};
    String[] bitemporalPrimaryKeys = new String[]{"id", "name", validityFromReferenceField, validityThroughReferenceField};
    String[] bitemporalFromTimeOnlyPrimaryKeys = new String[]{"id", "name", validityFromReferenceField};
    String[] bitemporalPartitionKeys = new String[]{validityFromReferenceField};

    // Base Columns: Primary keys : id, name
    Field id = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field tinyIntId = Field.builder().name("id").type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field tinyIntString = Field.builder().name("id").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field nameModified = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, 64, null)).primaryKey(true).build();
    Field amount = Field.builder().name("amount").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    Field floatAmount = Field.builder().name("amount").type(FieldType.of(DataType.FLOAT, Optional.empty(), Optional.empty())).build();
    Field bizDate = Field.builder().name("biz_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();

    // Bitemporal Columns:
    Field validityFromReference = Field.builder().name(validityFromReferenceField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field validityThroughReference = Field.builder().name(validityThroughReferenceField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field validityFromTarget = Field.builder().name(validityFromTargetField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    Field validityThroughTarget = Field.builder().name(validityThroughTargetField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();

    // Problematic Columns
    Field batchIdInNonPrimary = Field.builder().name(batchIdInField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    Field batchTimeInNonPrimary = Field.builder().name(batchTimeInField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();

    // Milestoning Columns
    Field digest = Field.builder().name(digestField).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    Field batchUpdateTime = Field.builder().name(batchUpdateTimeField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    Field batchIdIn = Field.builder().name(batchIdInField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field batchIdOut = Field.builder().name(batchIdOutField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    Field batchTimeIn = Field.builder().name(batchTimeInField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field batchTimeOut = Field.builder().name(batchTimeOutField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    Field deleteIndicator = Field.builder().name(deleteIndicatorField).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();

    SchemaDefinition baseTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    SchemaDefinition baseTableShortenedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .build();

    SchemaDefinition stagingTableEvolvedSize = SchemaDefinition.builder()
        .addFields(id)
        .addFields(nameModified)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    SchemaDefinition stagingTableImplicitDatatypeChange = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(floatAmount)
        .addFields(bizDate)
        .build();

    SchemaDefinition stagingTableNonBreakingDatatypeChange = SchemaDefinition.builder()
        .addFields(tinyIntId)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    SchemaDefinition stagingTableBreakingDatatypeChange = SchemaDefinition.builder()
        .addFields(tinyIntString)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .build();

    SchemaDefinition mainTableSchema = SchemaDefinition.builder()
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

    SchemaDefinition mainTableSchemaWithBatchIdInNotPrimary = SchemaDefinition.builder()
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

    SchemaDefinition mainTableBatchIdBasedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdIn)
        .addFields(batchIdOut)
        .build();

    SchemaDefinition mainTableBatchIdBasedSchemaWithBatchIdInNotPrimary = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchIdInNonPrimary)
        .addFields(batchIdOut)
        .build();

    SchemaDefinition mainTableTimeBasedSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchTimeIn)
        .addFields(batchTimeOut)
        .build();

    SchemaDefinition mainTableTimeBasedSchemaWithBatchTimeInNotPrimary = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchTimeInNonPrimary)
        .addFields(batchTimeOut)
        .build();

    SchemaDefinition baseTableSchemaWithDigest = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .build();

    SchemaDefinition baseTableSchemaWithDigestAndUpdateBatchTimeField = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(batchUpdateTime)
        .build();

    SchemaDefinition stagingTableSchemaWithLimitedColumns = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(digest)
        .build();

    SchemaDefinition stagingTableSchemaWithDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .addFields(deleteIndicator)
        .build();

    SchemaDefinition bitemporalMainTableSchema = SchemaDefinition.builder()
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

    SchemaDefinition bitemporalFromTimeOnlyMainTablSchema = SchemaDefinition.builder()
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

    SchemaDefinition bitemporalStagingTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(validityThroughReference)
        .addFields(digest)
        .build();

    SchemaDefinition bitemporalStagingTableSchemaWithDeleteIndicator = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(validityThroughReference)
        .addFields(digest)
        .addFields(deleteIndicator)
        .build();

    SchemaDefinition bitemporalFromTimeOnlyStagingTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(validityFromReference)
        .addFields(digest)
        .build();

    String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
        "(`table_name` VARCHAR(255)," +
        "`batch_start_ts_utc` DATETIME," +
        "`batch_end_ts_utc` DATETIME," +
        "`batch_status` VARCHAR(32)," +
        "`table_batch_id` INTEGER)";

    String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
        "(`TABLE_NAME` VARCHAR(255)," +
        "`BATCH_START_TS_UTC` DATETIME," +
        "`BATCH_END_TS_UTC` DATETIME," +
        "`BATCH_STATUS` VARCHAR(32)," +
        "`TABLE_BATCH_ID` INTEGER)";

    protected String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (`table_name`, `table_batch_id`, `batch_start_ts_utc`, `batch_end_ts_utc`, `batch_status`) (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`table_batch_id`),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.`table_name` = 'main'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    protected String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (`TABLE_NAME`, `TABLE_BATCH_ID`, `BATCH_START_TS_UTC`, `BATCH_END_TS_UTC`, `BATCH_STATUS`) (SELECT 'main',(SELECT COALESCE(MAX(BATCH_METADATA.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE BATCH_METADATA.`TABLE_NAME` = 'main'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    String expectedTruncateTableQuery = "TRUNCATE TABLE `mydb`.`staging`";
    String expectedDropTableQuery = "DROP TABLE IF EXISTS `mydb`.`staging`";

    String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
        "(`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`biz_date` DATE," +
        "`digest` VARCHAR(256)," +
        "`batch_id_in` INTEGER," +
        "`batch_id_out` INTEGER," +
        "`batch_time_in` DATETIME," +
        "`batch_time_out` DATETIME," +
        "PRIMARY KEY (`id`, `name`, `batch_id_in`, `batch_time_in`))";

    String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
        "(`ID` INTEGER," +
        "`NAME` VARCHAR(256)," +
        "`AMOUNT` DOUBLE," +
        "`BIZ_DATE` DATE," +
        "`DIGEST` VARCHAR(256)," +
        "`BATCH_ID_IN` INTEGER," +
        "`BATCH_ID_OUT` INTEGER," +
        "`BATCH_TIME_IN` DATETIME," +
        "`BATCH_TIME_OUT` DATETIME," +
        "PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`, `BATCH_TIME_IN`))";

    String expectedMainTableBatchIdBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER,`name` VARCHAR(256),`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256)," +
        "`batch_id_in` INTEGER,`batch_id_out` INTEGER,PRIMARY KEY (`id`, `name`, `batch_id_in`))";

    String expectedMainTableBatchIdBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
        "(`ID` INTEGER,`NAME` VARCHAR(256),`AMOUNT` DOUBLE,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256)," +
        "`BATCH_ID_IN` INTEGER,`BATCH_ID_OUT` INTEGER,PRIMARY KEY (`ID`, `NAME`, `BATCH_ID_IN`))";

    String expectedMainTableTimeBasedCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER,`name` VARCHAR(256),`amount` DOUBLE,`biz_date` DATE,`digest` VARCHAR(256)," +
        "`batch_time_in` DATETIME,`batch_time_out` DATETIME,PRIMARY KEY (`id`, `name`, `batch_time_in`))";

    String expectedMainTableTimeBasedCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
        "(`ID` INTEGER,`NAME` VARCHAR(256),`AMOUNT` DOUBLE,`BIZ_DATE` DATE,`DIGEST` VARCHAR(256)," +
        "`BATCH_TIME_IN` DATETIME,`BATCH_TIME_OUT` DATETIME,PRIMARY KEY (`ID`, `NAME`, `BATCH_TIME_IN`))";

    String expectedBaseTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`biz_date` DATE," +
        "PRIMARY KEY (`id`, `name`))";

    String expectedSchemaEvolutionAddColumn = "ALTER TABLE `mydb`.`main` ADD COLUMN `biz_date` DATE";

    String expectedSchemaEvolutionModifySize = "ALTER TABLE `mydb`.`main` ALTER COLUMN `name` VARCHAR(64) PRIMARY KEY";

    String expectedSchemaNonBreakingChange = "ALTER TABLE `mydb`.`main` ALTER COLUMN `id` TINYINT PRIMARY KEY";

    String expectedBaseTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`" +
        "(`ID` INTEGER," +
        "`NAME` VARCHAR(256)," +
        "`AMOUNT` DOUBLE," +
        "`BIZ_DATE` DATE," +
        "PRIMARY KEY (`ID`, `NAME`))";

    String expectedBaseTablePlusDigestCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`biz_date` DATE," +
        "`digest` VARCHAR(256)," +
        "PRIMARY KEY (`id`, `name`))";

    String expectedBaseTablePlusDigestCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS `MYDB`.`MAIN`(" +
        "`ID` INTEGER," +
        "`NAME` VARCHAR(256)," +
        "`AMOUNT` DOUBLE," +
        "`BIZ_DATE` DATE," +
        "`DIGEST` VARCHAR(256)," +
        "PRIMARY KEY (`ID`, `NAME`))";

    String expectedBaseTablePlusDigestPlusUpdateTimestampCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`(" +
        "`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`biz_date` DATE," +
        "`digest` VARCHAR(256)," +
        "`batch_update_time` DATETIME," +
        "PRIMARY KEY (`id`, `name`))";

    String expectedStagingDigestUpdateQuery = "UPDATE `mydb`.`staging` as stage SET " +
        "stage.`digest` = MD5(CONCAT(stage.`id`,stage.`name`,stage.`amount`," +
        "stage.`biz_date`,stage.`digest`))";

    String expectedBitemporalMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
        "(`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`validity_from_reference` DATETIME," +
        "`validity_through_reference` DATETIME," +
        "`digest` VARCHAR(256)," +
        "`batch_id_in` INTEGER," +
        "`batch_id_out` INTEGER," +
        "`validity_from_target` DATETIME," +
        "`validity_through_target` DATETIME," +
        "PRIMARY KEY (`id`, `name`, `validity_from_reference`, `validity_through_reference`, `batch_id_in`))";

    String expectedBitemporalFromTimeOnlyMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS `mydb`.`main`" +
        "(`id` INTEGER," +
        "`name` VARCHAR(256)," +
        "`amount` DOUBLE," +
        "`validity_from_reference` DATETIME," +
        "`digest` VARCHAR(256)," +
        "`batch_id_in` INTEGER," +
        "`batch_id_out` INTEGER," +
        "`validity_from_target` DATETIME," +
        "`validity_through_target` DATETIME," +
        "PRIMARY KEY (`id`, `name`, `validity_from_reference`, `batch_id_in`))";

    public void assertIfListsAreSameIgnoringOrder(List<String> first, List<String> second)
    {
        assertTrue(first.size() == second.size() &&
            first.stream().sorted().collect(Collectors.toList())
                .equals(second.stream().sorted().collect(Collectors.toList())));
    }
}
