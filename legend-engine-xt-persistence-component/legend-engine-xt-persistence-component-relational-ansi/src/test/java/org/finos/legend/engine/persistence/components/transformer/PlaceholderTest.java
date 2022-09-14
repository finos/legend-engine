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

package org.finos.legend.engine.persistence.components.transformer;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PlaceholderTest
{

    @Test
    void testTimestampPlaceholder()
    {
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), TransformOptions.builder().batchStartTimestampPattern("{BATCH_START_TIMESTAMP_PLACEHOLDER}").batchEndTimestampPattern("{BATCH_END_TIMESTAMP_PLACEHOLDER}").build());
        MetadataUtils store = new MetadataUtils(MetadataDataset.builder().build());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(
            store.insertMetaData(StringValue.of("main"), BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedSql = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE batch_metadata.\"table_name\" = 'main'),'{BATCH_START_TIMESTAMP_PLACEHOLDER}','{BATCH_END_TIMESTAMP_PLACEHOLDER}','DONE')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    void testTimestampPlaceholderWithUpperCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), TransformOptions.builder().batchStartTimestampPattern("{BATCH_START_TIMESTAMP_PLACEHOLDER}").batchEndTimestampPattern("{BATCH_END_TIMESTAMP_PLACEHOLDER}").addOptimizers(new UpperCaseOptimizer()).build());
        MetadataUtils store = new MetadataUtils(MetadataDataset.builder().build());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(
            store.insertMetaData(StringValue.of("main"), BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedSql = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\") (SELECT 'main',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE BATCH_METADATA.\"TABLE_NAME\" = 'main'),'{BATCH_START_TIMESTAMP_PLACEHOLDER}','{BATCH_END_TIMESTAMP_PLACEHOLDER}','DONE')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    void testBatchIdAndTimestampPlaceholder()
    {
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), TransformOptions.builder().batchIdPattern("{BATCH_ID_PATTERN}").batchStartTimestampPattern("{BATCH_START_TIMESTAMP_PLACEHOLDER}").batchEndTimestampPattern("{BATCH_END_TIMESTAMP_PLACEHOLDER}").build());
        MetadataUtils store = new MetadataUtils(MetadataDataset.builder().build());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(
            store.insertMetaData(StringValue.of("main"), BatchStartTimestamp.INSTANCE, BatchEndTimestamp.INSTANCE)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedSql = "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main',{BATCH_ID_PATTERN},'{BATCH_START_TIMESTAMP_PLACEHOLDER}','{BATCH_END_TIMESTAMP_PLACEHOLDER}','DONE')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

}
