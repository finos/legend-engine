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
    protected SchemaDefinition mainTableSchema = SchemaDefinition.builder()
            .addFields(id)
            .addFields(name)
            .addFields(batchIdIn)
            .addFields(batchIdOut)
            .build();
    private final MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").build();
    private final String auditTableCreationQuery = "CREATE TABLE IF NOT EXISTS sink_cleanup_audit(\"table_name\" VARCHAR(255),\"execution_ts_utc\" DATETIME,\"status\" VARCHAR(32),\"requested_by\" VARCHAR(32))";
    private final String dropMainTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"main\"";
    private final String deleteFromMetadataTableQuery = "DELETE FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'";
    private final String insertToAuditTableQuery = "INSERT INTO sink_cleanup_audit (\"table_name\", \"execution_ts_utc\", \"status\", \"requested_by\") (SELECT 'main','2000-01-01 00:00:00.000000','SUCCEEDED','lh_dev')";


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
        Assertions.assertEquals(dropMainTableQuery, result.dropSql().get(0));
        Assertions.assertEquals(deleteFromMetadataTableQuery, cleanupSql.get(0));
        Assertions.assertEquals(insertToAuditTableQuery, cleanupSql.get(1));
    }
}
