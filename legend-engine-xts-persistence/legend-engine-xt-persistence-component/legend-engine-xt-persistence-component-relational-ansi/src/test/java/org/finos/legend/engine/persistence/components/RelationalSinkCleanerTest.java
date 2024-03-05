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
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSinkCleaner;
import org.finos.legend.engine.persistence.components.relational.api.SinkCleanupGeneratorResult;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RelationalSinkCleanerTest extends IngestModeTest
{
    private static DatasetDefinition mainTable;
    private final static LockInfoDataset lockTable = LockInfoDataset.builder().name("lock_info").build();
    protected SchemaDefinition mainTableSchema = SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(batchIdIn)
            .addFields(batchIdOut)
            .build();
    private final MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").build();
    private final String auditTableCreationQuery = "CREATE TABLE IF NOT EXISTS sink_cleanup_audit(\"table_name\" VARCHAR(255),\"batch_start_ts_utc\" DATETIME,\"batch_end_ts_utc\" DATETIME,\"batch_status\" VARCHAR(32),\"requested_by\" VARCHAR(32))";
    private final String dropMainTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"main\" CASCADE";
    private final String deleteFromMetadataTableQuery = "DELETE FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'";
    private final String insertToAuditTableQuery = "INSERT INTO sink_cleanup_audit (\"table_name\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"requested_by\") (SELECT 'main','2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'SUCCEEDED','lh_dev')";


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
                .metadataDataset(metadata)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .build();
        SinkCleanupGeneratorResult result = sinkCleaner.generateOperationsForSinkCleanup();

        List<String> preActionsSql = result.preActionsSql();

        Assertions.assertEquals(auditTableCreationQuery, preActionsSql.get(0));

        List<String> cleanupSql = result.cleanupSql();
        Assertions.assertEquals(dropMainTableQuery, cleanupSql.get(0));
        Assertions.assertEquals(deleteFromMetadataTableQuery, cleanupSql.get(1));
        Assertions.assertEquals(insertToAuditTableQuery, cleanupSql.get(2));
    }

    @Test
    void testGenerateOperationsForSinkCleanupWithConcurrencyFlag()
    {
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(AnsiSqlSink.get())
                .mainDataset(mainTable)
                .executionTimestampClock(fixedClock_2000_01_01)
                .enableConcurrentSafety(true)
                .lockInfoDataset(lockTable)
                .metadataDataset(metadata)
                .requestedBy("lh_dev")
                .build();
        SinkCleanupGeneratorResult result = sinkCleaner.generateOperationsForSinkCleanup();

        List<String> preActionsSql = result.preActionsSql();
        String lockTableCreationQuery = "CREATE TABLE IF NOT EXISTS lock_info(\"insert_ts_utc\" DATETIME,\"last_used_ts_utc\" DATETIME,\"table_name\" VARCHAR UNIQUE)";
        Assertions.assertEquals(auditTableCreationQuery, preActionsSql.get(0));
        Assertions.assertEquals(lockTableCreationQuery, preActionsSql.get(1));

        String initializeLockQuery = "INSERT INTO lock_info (\"insert_ts_utc\", \"table_name\") (SELECT '2000-01-01 00:00:00.000000','main' WHERE NOT (EXISTS (SELECT * FROM lock_info as lock_info)))";
        String acquireLockQuery = "UPDATE lock_info as lock_info SET lock_info.\"last_used_ts_utc\" = '2000-01-01 00:00:00.000000'";
        Assertions.assertEquals(initializeLockQuery, result.initializeLockSql().get(0));
        Assertions.assertEquals(acquireLockQuery, result.acquireLockSql().get(0));

        List<String> cleanupSql = result.cleanupSql();
        Assertions.assertEquals(dropMainTableQuery, cleanupSql.get(0));
        Assertions.assertEquals(deleteFromMetadataTableQuery, cleanupSql.get(1));
        Assertions.assertEquals(insertToAuditTableQuery, cleanupSql.get(2));
    }
}
