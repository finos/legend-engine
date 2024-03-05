// Copyright 2024 Goldman Sachs
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
//

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSinkCleaner;
import org.finos.legend.engine.persistence.components.relational.api.SinkCleanupGeneratorResult;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RelationalSinkCleanerTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private static LockInfoDataset lockTable = LockInfoDataset.builder().name("lock_info").build();
    protected SchemaDefinition mainTableSchema = SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(batchIdIn)
            .addFields(batchIdOut)
            .build();

    @BeforeEach
    void initializeTables()
    {
        mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(mainTableSchema)
                .build();
    }

    @Test
    void testGenerateOperationsForSinkCleanup()
    {
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(AnsiSqlSink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        SinkCleanupGeneratorResult result = sinkCleaner.generateOperationsForSinkCleanup();

        List<String> preActionsSql = result.preActionsSql();
        String creationQuery = "CREATE TABLE IF NOT EXISTS sink_cleanup_audit(\"table_name\" VARCHAR(255),\"batch_start_ts_utc\" DATETIME,\"batch_end_ts_utc\" DATETIME,\"batch_status\" VARCHAR(32))";
        Assertions.assertEquals(creationQuery, preActionsSql.get(0));

        String dropMainTable = "DROP TABLE IF EXISTS \"mydb\".\"main\" CASCADE";
        String deleteFromMetadataTable = "DELETE FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'";
        String insertToAuditTable = "INSERT INTO sink_cleanup_audit (\"table_name\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main','2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";
        List<String> cleanupSql = result.cleanupSql();
        Assertions.assertEquals(dropMainTable, cleanupSql.get(0));
        Assertions.assertEquals(deleteFromMetadataTable, cleanupSql.get(1));
        Assertions.assertEquals(insertToAuditTable, cleanupSql.get(2));
    }

    @Test
    void testGenerateOperationsForSinkCleanupWithConcurrencyFlagAndUpperCase()
    {
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(AnsiSqlSink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .enableConcurrentSafety(true)
                .lockInfoDataset(lockTable)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        SinkCleanupGeneratorResult result = sinkCleaner.generateOperationsForSinkCleanup();

        List<String> preActionsSql = result.preActionsSql();
        String auditTableCreationQuery = "CREATE TABLE IF NOT EXISTS SINK_CLEANUP_AUDIT(\"TABLE_NAME\" VARCHAR(255),\"BATCH_START_TS_UTC\" DATETIME,\"BATCH_END_TS_UTC\" DATETIME,\"BATCH_STATUS\" VARCHAR(32))";
        String lockTableCreationQuery = "CREATE TABLE IF NOT EXISTS LOCK_INFO(\"INSERT_TS_UTC\" DATETIME,\"LAST_USED_TS_UTC\" DATETIME,\"TABLE_NAME\" VARCHAR UNIQUE)";
        Assertions.assertEquals(auditTableCreationQuery, preActionsSql.get(0));
        Assertions.assertEquals(lockTableCreationQuery, preActionsSql.get(1));

        //todo : table name (main) in small ?
        String initializeLockQuery = "INSERT INTO LOCK_INFO (\"INSERT_TS_UTC\", \"TABLE_NAME\") (SELECT '2000-01-01 00:00:00.000000','main' WHERE NOT (EXISTS (SELECT * FROM LOCK_INFO as lock_info)))";
        String acquireLockQuery = "UPDATE LOCK_INFO as lock_info SET lock_info.\"LAST_USED_TS_UTC\" = '2000-01-01 00:00:00.000000'";
        Assertions.assertEquals(initializeLockQuery, result.initializeLockSql().get(0));
        Assertions.assertEquals(acquireLockQuery, result.acquireLockSql().get(0));

        String dropMainTableQuery = "DROP TABLE IF EXISTS \"MYDB\".\"MAIN\" CASCADE";
        String deleteFromMetadataTableQuery = "DELETE FROM BATCH_METADATA as batch_metadata WHERE UPPER(batch_metadata.\"TABLE_NAME\") = 'MAIN'";
        String insertToAuditTableQuery = "INSERT INTO SINK_CLEANUP_AUDIT (\"TABLE_NAME\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\") (SELECT 'main','2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE')";
        List<String> cleanupSql = result.cleanupSql();
        Assertions.assertEquals(dropMainTableQuery, cleanupSql.get(0));
        Assertions.assertEquals(deleteFromMetadataTableQuery, cleanupSql.get(1));
        Assertions.assertEquals(insertToAuditTableQuery, cleanupSql.get(2));
    }
}
