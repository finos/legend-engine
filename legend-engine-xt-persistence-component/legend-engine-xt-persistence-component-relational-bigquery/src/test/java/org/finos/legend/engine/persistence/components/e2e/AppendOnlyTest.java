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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.scenarios.AppendOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class AppendOnlyTest extends BigQueryEndToEndTest
{

    //@Test
    public void testAppend() throws IOException, InterruptedException
    {
        TestScenario scenario = new AppendOnlyScenarios().ALLOW_DUPLICATES_WITH_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(BigQuerySink.get())
                .collectStatistics(true)
                .cleanupStagingData(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();

        DatasetDefinition stagingDataset = (DatasetDefinition) scenario.getDatasets().stagingDataset();
        DatasetDefinition mainDataset = (DatasetDefinition) scenario.getDatasets().mainDataset();

        // Drop tables if exists
        delete(mainDataset);
        delete(stagingDataset);

        // Create Staging table
        createTable(stagingDataset);

        // Pass 1
        loadData("src/test/resources/append/input/data_pass1.csv", stagingDataset, 1);
        ingest(preActionsSqlList, milestoningSqlList, metadataIngestSql, postActionsSql);

        // Pass 2
        loadData("src/test/resources/append/input/data_pass2.csv", stagingDataset, 1);
        ingest(preActionsSqlList, milestoningSqlList, metadataIngestSql, postActionsSql);
    }

}
