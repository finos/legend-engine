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

public class IngestModeTest
{
    protected final ZonedDateTime fixedZonedDateTime_2000_01_01 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedZonedDateTime_2000_01_01.toInstant(), ZoneOffset.UTC);

    String mainDbName = "mydb";
    String mainTableName = "main";
    String mainTableAlias = "sink";

    String stagingDbName = "mydb";
    String stagingTableName = "staging";
    String stagingTableAlias = "stage";

    String digestField = "digest";
    String batchIdInField = "batch_id_in";
    String batchIdOutField = "batch_id_out";
    String batchTimeInField = "batch_time_in";
    String batchTimeOutField = "batch_time_out";
    String deleteIndicatorField = "delete_indicator";

    String[] partitionKeys = new String[]{"biz_date"};
    HashMap<String, Set<String>> partitionFilter = new HashMap<String, Set<String>>()
    {{
        put("biz_date", new HashSet<>(Arrays.asList("2000-01-01 00:00:00", "2000-01-02 00:00:00")));
    }};

    // Base Columns: Primary keys : id, name
    Field id = Field.builder().name("id").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    Field amount = Field.builder().name("amount").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    Field bizDate = Field.builder().name("biz_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();

    // Milestoning Columns
    Field digest = Field.builder().name(digestField).type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
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

    SchemaDefinition baseTableSchemaWithDigest = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(digest)
        .build();

    String expectedMetadataTableCreateQuery = "CREATE TABLE IF NOT EXISTS batch_metadata" +
        "(\"table_name\" VARCHAR(255)," +
        "\"batch_start_ts_utc\" DATETIME," +
        "\"batch_end_ts_utc\" DATETIME," +
        "\"batch_status\" VARCHAR(32)," +
        "\"table_batch_id\" INTEGER)";

    String expectedMetadataTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS BATCH_METADATA" +
        "(\"TABLE_NAME\" VARCHAR(255)," +
        "\"BATCH_START_TS_UTC\" DATETIME," +
        "\"BATCH_END_TS_UTC\" DATETIME," +
        "\"BATCH_STATUS\" VARCHAR(32)," +
        "\"TABLE_BATCH_ID\" INTEGER)";

    protected String expectedMetadataTableIngestQuery = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),'2000-01-01 00:00:00',SYSDATE(),'DONE')";

    protected String expectedMetadataTableIngestQueryWithUpperCase = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\")" +
        " (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as batch_metadata WHERE batch_metadata.\"TABLE_NAME\" = 'main'),'2000-01-01 00:00:00',SYSDATE(),'DONE')";


    String expectedMainTableCreateQuery = "CREATE TABLE IF NOT EXISTS \"mydb\".\"main\"" +
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

    String expectedMainTableCreateQueryWithUpperCase = "CREATE TABLE IF NOT EXISTS \"MYDB\".\"MAIN\"" +
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
}
