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

    String[] partitionKeys = new String[]{"biz_date"};
    HashMap<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put("biz_date", new HashSet<>(Arrays.asList("2000-01-01 00:00:00", "2000-01-02 00:00:00")));
    }};

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
    Field idNonPrimary = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    Field nameNonPrimary = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();

    // Milestoning Columns
    Field digest = Field.builder().name(digestField).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    Field batchUpdateTime = Field.builder().name(batchUpdateTimeField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    Field batchIdIn = Field.builder().name(batchIdInField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field batchIdOut = Field.builder().name(batchIdOutField).type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).build();
    Field batchTimeIn = Field.builder().name(batchTimeInField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field batchTimeOut = Field.builder().name(batchTimeOutField).type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    Field deleteIndicator = Field.builder().name(deleteIndicatorField).type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();

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

    protected String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (`TABLE_NAME`, `TABLE_BATCH_ID`, `BATCH_START_TS_UTC`, `BATCH_END_TS_UTC`, `BATCH_STATUS`) (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.`TABLE_BATCH_ID`),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.`TABLE_NAME` = 'main'),'2000-01-01 00:00:00',CURRENT_TIMESTAMP(),'DONE')";

    String expectedStagingCleanupQuery = "DELETE FROM `mydb`.`staging` as stage";

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

    public void assertIfListsAreSameIgnoringOrder(List<String> first, List<String> second)
    {
        assertTrue(first.size() == second.size() &&
            first.stream().sorted().collect(Collectors.toList())
                .equals(second.stream().sorted().collect(Collectors.toList())));
    }
}
