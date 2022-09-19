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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class MetadataUtilsWithCustomTableNameTest extends MetadataUtilsTest
{

    @Override
    protected MetadataDataset metadataDataset()
    {
        return MetadataDataset.builder()
            .metadataDatasetName("custom_batch_metadata_1")
            .build();
    }

    @Override
    protected String lowerCaseTableName()
    {
        return "custom_batch_metadata_1";
    }

    @Test
    public void testMetadataDatasetWithCustomDatabaseAndGroupName()
    {
        MetadataDataset metadataDataset = MetadataDataset.builder()
            .metadataDatasetName("custom_batch_metadata_1")
            .metadataDatasetDatabaseName("custom_db")
            .metadataDatasetGroupName("custom_schema")
            .build();

        LogicalPlan logicalPlan = LogicalPlanFactory.getDatasetCreationPlan(metadataDataset.get(), true);
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "CREATE TABLE IF NOT EXISTS \"custom_db\".\"custom_schema\".\"" + lowerCaseTableName() +
            "\"(\"table_name\" VARCHAR(255),\"batch_start_ts_utc\" DATETIME,\"batch_end_ts_utc\" DATETIME,\"batch_status\" VARCHAR(32),\"table_batch_id\" INTEGER)";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

}
