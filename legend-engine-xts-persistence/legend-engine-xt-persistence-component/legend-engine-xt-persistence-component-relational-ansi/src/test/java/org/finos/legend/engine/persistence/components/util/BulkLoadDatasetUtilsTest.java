// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public abstract class BulkLoadDatasetUtilsTest
{

    private final ZonedDateTime executionZonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final TransformOptions transformOptions = TransformOptions
            .builder()
            .executionTimestampClock(Clock.fixed(executionZonedDateTime.toInstant(), ZoneOffset.UTC))
            .bulkLoadBatchStatusPattern("<BATCH_STATUS_PATTERN>")
            .build();

    private BulkLoadMetadataDataset bulkLoadMetadataDataset = BulkLoadMetadataDataset.builder().build();

    @Test
    public void testInsertMetadata()
    {
        BulkLoadMetadataUtils bulkLoadMetadataUtils = new BulkLoadMetadataUtils(bulkLoadMetadataDataset);
        StringValue bulkLoadTableName = StringValue.of("appeng_log_table_name");
        StringValue batchLineageValue = StringValue.of("my_lineage_value");
        Insert operation = bulkLoadMetadataUtils.insertMetaData(bulkLoadTableName, batchLineageValue);

        RelationalTransformer transformer = new RelationalTransformer(getRelationalSink(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = getExpectedSqlForMetadata();
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    public abstract String getExpectedSqlForMetadata();

    @Test
    public void testInsertMetadataInUpperCase()
    {
        BulkLoadMetadataUtils bulkLoadMetadataUtils = new BulkLoadMetadataUtils(bulkLoadMetadataDataset);
        StringValue bulkLoadTableName = StringValue.of("BULK_LOAD_TABLE_NAME");
        StringValue batchLineageValue = StringValue.of("my_lineage_value");

        Insert operation = bulkLoadMetadataUtils.insertMetaData(bulkLoadTableName, batchLineageValue);

        RelationalTransformer transformer = new RelationalTransformer(getRelationalSink(), transformOptions.withOptimizers(new UpperCaseOptimizer()));
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = getExpectedSqlForMetadataUpperCase();
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    public abstract String getExpectedSqlForMetadataUpperCase();

    public abstract RelationalSink getRelationalSink();
}
