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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.*;
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

import java.util.Arrays;
import java.util.List;

public class DatasetVersioningHandlerTest extends IngestModeTest
{
    private final TransformOptions transformOptions = TransformOptions.builder().build();
    Dataset stagingDataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("stage")
            .schema(baseTableSchemaWithVersion)
            .build();

    List<String> primaryKeys = Arrays.asList("id", "name");

    @Test
    public void testVersioningHandlerNoVersioningStrategy()
    {
        Dataset versionedDataset = NoVersioningStrategy.builder().build().accept(new DatasetVersioningHandler(stagingDataset, primaryKeys));
        Assertions.assertTrue(versionedDataset instanceof DatasetDefinition);
        DatasetDefinition versionedDatasetDef = (DatasetDefinition) versionedDataset;
        Assertions.assertEquals(versionedDatasetDef, stagingDataset);
    }

    @Test
    public void testVersioningHandlerMaxVersionStrategy()
    {
        Dataset versionedDataset = MaxVersionStrategy.builder().versioningField("version").build().accept(new DatasetVersioningHandler(stagingDataset, primaryKeys));
        Selection versionedSelection = (Selection) versionedDataset;
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(versionedSelection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\" " +
                "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\",DENSE_RANK() OVER " +
                "(PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" DESC) as \"legend_persistence_rank\" " +
                "FROM \"my_db\".\"my_schema\".\"my_table\" as stage) as stage WHERE stage.\"legend_persistence_rank\" = 1) as stage";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testVersioningHandlerAllVersionsStrategy()
    {
        Dataset versionedDataset = AllVersionsStrategy.builder().versioningField("version").build().accept(new DatasetVersioningHandler(stagingDataset, primaryKeys));
        Selection versionedSelection = (Selection) versionedDataset;
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(versionedSelection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\"," +
                "DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" ASC) as \"legend_persistence_data_split\" " +
                "FROM \"my_db\".\"my_schema\".\"my_table\" as stage) as stage";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testVersioningHandlerWithDeduplicationHandler()
    {
        Dataset dedupedDataset = FailOnDuplicates.builder().build().accept(new DatasetDeduplicationHandler(stagingDataset));
        Dataset versionedDataset = AllVersionsStrategy.builder().versioningField("version").build().accept(new DatasetVersioningHandler(dedupedDataset, primaryKeys));
        Selection versionedSelection = (Selection) versionedDataset;
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(versionedSelection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\"," +
                "stage.\"legend_persistence_count\" as \"legend_persistence_count\"," +
                "DENSE_RANK() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" ASC) as \"legend_persistence_data_split\" " +
                "FROM (SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\"," +
                "COUNT(*) as \"legend_persistence_count\" FROM \"my_db\".\"my_schema\".\"my_table\" as stage " +
                "GROUP BY stage.\"id\", stage.\"name\", stage.\"version\", stage.\"biz_date\") as stage) as stage";
        Assertions.assertEquals(expectedSql, list.get(0));
    }
}
