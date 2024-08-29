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

package org.finos.legend.engine.persistence.components.ingestmode.mixed;

import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestDetails;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestResults;
import org.finos.legend.engine.persistence.components.relational.api.RelationalMultiDatasetIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

public class MultiDatasetRunner implements Runnable
{
    private JdbcHelper h2Sink;
    private String ingestRequestId;
    private static final String lockDataset = "LOCK_DATASET";

    private Map<String, List<DatasetIngestResults>> results;

    private List<DatasetIngestDetails> ingestDetails;

    Map<String, Long> requestIdBatchIdMap;

    private static final LockInfoDataset lockInfoDataset = LockInfoDataset.builder()
            .database(testDatabaseName)
            .group(testSchemaName)
            .name(lockDataset)
            .build();

    public MultiDatasetRunner(List<DatasetIngestDetails> ingestDetails, String ingestRequestId,
                              Map<String, List<DatasetIngestResults>> results, Map<String, Long> requestIdBatchIdMap,
                              String h2User, String h2Pwd, String h2JdbcUrl)
    {
        this.ingestDetails = ingestDetails;
        this.ingestRequestId = ingestRequestId;
        this.requestIdBatchIdMap = requestIdBatchIdMap;
        this.results = results;
        this.h2Sink = JdbcHelper.of(H2Sink.createConnection(h2User, h2Pwd, h2JdbcUrl));
    }

    @Override
    public void run()
    {
        try
        {
            RelationalMultiDatasetIngestor ingestor = RelationalMultiDatasetIngestor.builder()
                    .relationalSink(H2Sink.get())
                    .lockInfoDataset(lockInfoDataset)
                    .ingestRequestId(this.ingestRequestId)
                    .enableIdempotencyCheck(true)
                    .build();

            ingestor.init(ingestDetails, JdbcConnection.of(h2Sink.connection()));
            ingestor.create();
            List<DatasetIngestResults> ingestResults = ingestor.ingest();
            results.put(ingestRequestId, ingestResults);
            requestIdBatchIdMap.put(ingestRequestId, ingestResults.get(0).batchId());
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
}