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

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestDetails;
import org.finos.legend.engine.persistence.components.relational.api.DatasetIngestResults;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.finos.legend.engine.persistence.components.RelationalMultiDatasetIngestorTest.configureForTest1;
import static org.finos.legend.engine.persistence.components.TestUtils.digestUDF;

public class MultiDatasetConcurrentRunnerTest extends BaseTest
{

    @Test
    public void testSameIngestMode() throws InterruptedException
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        String ingestRequestId1 = "abcd-pqrs-0001";
        String ingestRequestId2 = "abcd-pqrs-0002";

        Map<String, List<DatasetIngestResults>> results = new HashMap<>();
        Map<String, Long> requestIdBatchIdMap = new HashMap<>();

        // Batch 1
        List<DatasetIngestDetails> datasetIngestDetails1 = configureForTest1("src/test/resources/data/multi-dataset/set1/input/file1_for_dataset1.csv", "src/test/resources/data/multi-dataset/set1/input/file1_for_dataset2.csv");
        List<DatasetIngestDetails> datasetIngestDetails2 = configureForTest1("src/test/resources/data/multi-dataset/set1/input/file2_for_dataset1.csv", "src/test/resources/data/multi-dataset/set1/input/file2_for_dataset2.csv");

        Runnable r1 = new MultiDatasetRunner(datasetIngestDetails1, ingestRequestId1, results, requestIdBatchIdMap, H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL);
        Thread t1 = new Thread(r1);
        t1.start();

        // Thread 2
        Runnable r2 = new MultiDatasetRunner(datasetIngestDetails2, ingestRequestId2, results, requestIdBatchIdMap, H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL);
        Thread t2 = new Thread(r2);
        t2.start();
        t1.join();
        t2.join();

        // Verify Results
        List<Long> batchIds = new ArrayList<Long>(requestIdBatchIdMap.values());
        Assertions.assertEquals(2, batchIds.size());
        Assertions.assertTrue(batchIds.contains(1L));
        Assertions.assertTrue(batchIds.contains(2L));

        long batchIdRequest1 = requestIdBatchIdMap.get(ingestRequestId1);
        long batchIdRequest2 = requestIdBatchIdMap.get(ingestRequestId2);


        Assertions.assertEquals(batchIdRequest1, results.get(ingestRequestId1).get(0).batchId());
        Assertions.assertEquals("DATASET_1", results.get(ingestRequestId1).get(0).dataset());
        Assertions.assertEquals(batchIdRequest1, results.get(ingestRequestId1).get(1).batchId());
        Assertions.assertEquals("DATASET_2", results.get(ingestRequestId1).get(1).dataset());

        Assertions.assertEquals(batchIdRequest2, results.get(ingestRequestId2).get(0).batchId());
        Assertions.assertEquals("DATASET_1", results.get(ingestRequestId2).get(0).dataset());
        Assertions.assertEquals(batchIdRequest2, results.get(ingestRequestId2).get(1).batchId());
        Assertions.assertEquals("DATASET_2", results.get(ingestRequestId2).get(1).dataset());
    }

}
