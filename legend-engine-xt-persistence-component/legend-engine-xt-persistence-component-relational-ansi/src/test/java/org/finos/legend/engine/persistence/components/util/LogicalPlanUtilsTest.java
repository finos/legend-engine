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
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class LogicalPlanUtilsTest extends IngestModeTest
{

    @Test
    public void testDeduplicateByMaxVersion()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("stage")
                .schema(baseTableSchemaWithVersion)
                .build();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());

        List<String> primaryKeys = Arrays.asList("id", "name");
        Selection selection = LogicalPlanUtils.deduplicateByMaxVersionAndFilterDataset(dataset, primaryKeys, "version", Optional.empty());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedSelectQuery = "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\" FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\"," +
                "ROW_NUMBER() OVER (PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" DESC) as \"legend_persistence_row_num\" " +
                "FROM \"my_db\".\"my_schema\".\"my_table\" as stage) as X " +
                "WHERE X.\"legend_persistence_row_num\" = 1) as stage";
        Assertions.assertEquals(expectedSelectQuery, list.get(0));
    }

    @Test
    public void testDeduplicateByMaxVersionAndFilterDataset()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("stage")
                .schema(baseTableSchemaWithVersion)
                .build();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());
        List<String> primaryKeys = Arrays.asList("id", "name");

        Condition filterCondition1 = GreaterThan
                .builder()
                .leftValue(FieldValue.builder().fieldName("biz_date").datasetRefAlias("stage").build())
                .rightValue(StringValue.of("2020-01-01"))
                .build();

        Condition filterCondition2 = LessThan
                .builder()
                .leftValue(FieldValue.builder().fieldName("biz_date").datasetRefAlias("stage").build())
                .rightValue(StringValue.of("2020-01-03"))
                .build();

        Optional<Condition> filterCondition = Optional.of(And.of(Arrays.asList(filterCondition1, filterCondition2)));
        Selection selection = LogicalPlanUtils.deduplicateByMaxVersionAndFilterDataset(dataset, primaryKeys, "version", filterCondition);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedSelectQuery = "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\" FROM " +
                "(SELECT stage.\"id\",stage.\"name\",stage.\"version\",stage.\"biz_date\",ROW_NUMBER() OVER " +
                "(PARTITION BY stage.\"id\",stage.\"name\" ORDER BY stage.\"version\" DESC) as \"legend_persistence_row_num\" " +
                "FROM \"my_db\".\"my_schema\".\"my_table\" as stage " +
                "WHERE (stage.\"biz_date\" > '2020-01-01') AND (stage.\"biz_date\" < '2020-01-03')) as X " +
                "WHERE X.\"legend_persistence_row_num\" = 1) as stage";
        Assertions.assertEquals(expectedSelectQuery, list.get(0));
    }


}
