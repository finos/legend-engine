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

package org.finos.legend.engine.persistence.components.ingestmode.mixed;

import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

public class UnitemporalDeltaRunner implements Runnable
{
    private String stagingSuffix;
    private Clock clock;
    private AtomicInteger maxBatchIdCounter;
    private String dataPath;
    private JdbcHelper h2Sink;
    DatasetDefinition mainTable = TestUtils.getDefaultMainTable();

    IngestMode getIngestMode()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();
        return ingestMode;
    }

    public UnitemporalDeltaRunner(String dataPath, String stagingSuffix, String h2User, String h2Pwd, String h2JdbcUrl, Clock clock, AtomicInteger maxBatchIdCounter)
    {
        this.dataPath = dataPath;
        this.stagingSuffix = stagingSuffix;
        this.clock = clock;
        this.maxBatchIdCounter = maxBatchIdCounter;
        this.h2Sink = JdbcHelper.of(H2Sink.createConnection(h2User, h2Pwd, h2JdbcUrl));
    }

    @Override
    public void run()
    {
        try
        {
            DatasetDefinition stagingTable = DatasetDefinition.builder()
                    .group(testSchemaName)
                    .name(stagingTableName + stagingSuffix)
                    .schema(getStagingSchema())
                    .build();

            createStagingTable(stagingTable);
            loadBasicStagingData(dataPath, stagingTableName + stagingSuffix);
            Datasets datasets = Datasets.of(mainTable, stagingTable);
            RelationalIngestor ingestor = RelationalIngestor.builder()
                    .ingestMode(getIngestMode())
                    .relationalSink(H2Sink.get())
                    .cleanupStagingData(true)
                    .collectStatistics(true)
                    .enableConcurrentSafety(true)
                    .executionTimestampClock(clock)
                    .build();

            IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
            if (maxBatchIdCounter.get() < result.batchId().get())
            {
                maxBatchIdCounter.set(result.batchId().get());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            h2Sink.close();
        }
    }

    protected void loadBasicStagingData(String path, String tableName) throws Exception
    {
        String loadSql = String.format("TRUNCATE TABLE \"TEST\".\"%s\";", tableName) +
                String.format("INSERT INTO \"TEST\".\"%s\"(id, name, income, start_time ,expiry_date, digest) ", tableName) +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void createStagingTable(DatasetDefinition stagingTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        h2Sink.executeStatements(tableCreationPhysicalPlan.getSqlList());
    }
}