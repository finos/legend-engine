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
    private final MetadataDataset metadata = MetadataDataset.builder().metadataDatasetName("batch_metadata").metadataDatasetGroupName("mydb").build();
    private final String dropMainTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"main\"";
    private final String dropLockTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"main_legend_persistence_lock\"";
    private final String dropMetadataTableQuery = "DROP TABLE IF EXISTS \"mydb\".\"batch_metadata\"";


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

        Assertions.assertEquals(3, result.dropSql().size());
        Assertions.assertEquals(dropMainTableQuery, result.dropSql().get(0));
        Assertions.assertEquals(dropLockTableQuery, result.dropSql().get(1));
        Assertions.assertEquals(dropMetadataTableQuery, result.dropSql().get(2));
    }

    @Test
    void testGenerateOperationsForSinkCleanupWithLockTable()
    {
        RelationalSinkCleaner sinkCleaner = RelationalSinkCleaner.builder()
                .relationalSink(AnsiSqlSink.get())
                .mainDataset(mainTable)
                .metadataDataset(metadata)
                .executionTimestampClock(fixedClock_2000_01_01)
                .requestedBy("lh_dev")
                .lockDataset(LockInfoDataset.builder().name("lock_table").group("mydb").build())
                .build();
        SinkCleanupGeneratorResult result = sinkCleaner.generateOperationsForSinkCleanup();

        Assertions.assertEquals(3, result.dropSql().size());
        Assertions.assertEquals(dropMainTableQuery, result.dropSql().get(0));
        Assertions.assertEquals("DROP TABLE IF EXISTS \"mydb\".\"lock_table\"", result.dropSql().get(1));
        Assertions.assertEquals(dropMetadataTableQuery, result.dropSql().get(2));
    }
}
