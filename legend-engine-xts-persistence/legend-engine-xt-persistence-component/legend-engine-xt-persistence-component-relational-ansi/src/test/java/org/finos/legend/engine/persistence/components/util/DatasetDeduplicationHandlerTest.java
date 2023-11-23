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

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DatasetDeduplicationHandler;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;

public class DatasetDeduplicationHandlerTest extends IngestModeTest
{
    private final TransformOptions transformOptions = TransformOptions.builder().build();
    Dataset stagingDataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("stage")
            .schema(baseTableSchemaWithVersion)
            .build();

    String expectedSql = "SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\",COUNT(*) as \"legend_persistence_count\" " +
            "FROM \"my_db\".\"my_schema\".\"my_table\" as stage " +
            "GROUP BY stage.\"id\", stage.\"name\", stage.\"version\", stage.\"biz_date\"";

    @Test
    public void testDatasetDeduplicationFailOnDuplicates()
    {
        Dataset dedupedDataset = FailOnDuplicates.builder().build().accept(new DatasetDeduplicationHandler(stagingDataset));
        Selection dedupedSelection = (Selection) dedupedDataset;
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(dedupedSelection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testDatasetDeduplicationFilterDuplicates()
    {
        Dataset dedupedDataset = FilterDuplicates.builder().build().accept(new DatasetDeduplicationHandler(stagingDataset));
        Selection dedupedSelection = (Selection) dedupedDataset;
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(dedupedSelection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testDatasetDeduplicationAllowDuplicates()
    {
        Dataset dedupedDataset = AllowDuplicates.builder().build().accept(new DatasetDeduplicationHandler(stagingDataset));
        Assertions.assertTrue(dedupedDataset instanceof DatasetDefinition);
        DatasetDefinition dedupedDatasetDef = (DatasetDefinition) dedupedDataset;
        Assertions.assertEquals(dedupedDatasetDef, stagingDataset);
    }
}
