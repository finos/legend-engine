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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.scenarios.UnitemporalDeltaBatchIdBasedScenarios;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

public class UnitempDeltaTest extends BigQueryEndToEndTest
{

    //@Test
    public void testAppend() throws IOException, InterruptedException
    {
        TestScenario scenario = new UnitemporalDeltaBatchIdBasedScenarios().BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(BigQuerySink.get())
                .collectStatistics(true)
                .cleanupStagingData(true)
                .build();

        DatasetDefinition stagingDataset = (DatasetDefinition) scenario.getDatasets().stagingDataset();
        DatasetDefinition mainDataset = (DatasetDefinition) scenario.getDatasets().mainDataset();
        MetadataDataset metadataDataset = MetadataDataset.builder().metadataDatasetGroupName("mydb").metadataDatasetName("batch_metadata").build();
        Datasets datasets = Datasets.builder().mainDataset(mainDataset).stagingDataset(stagingDataset).metadataDataset(metadataDataset).build();

        GeneratorResult operations = generator.generateOperations(datasets);
        List<String> preActionsSqlList = operations.preActionsSql();
        List<String> milestoningSqlList = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> postActionsSql = operations.postActionsSql();

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
