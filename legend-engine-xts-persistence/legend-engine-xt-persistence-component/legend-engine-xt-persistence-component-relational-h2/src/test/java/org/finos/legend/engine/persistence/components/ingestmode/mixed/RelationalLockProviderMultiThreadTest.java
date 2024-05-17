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
import org.finos.legend.engine.persistence.components.RelationalSinkCleanerTest;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalLockProvider;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RelationalLockProviderMultiThreadTest extends BaseTest
{

    /*
    initial value of batch_id = 1
    Two threads are trying to read and increment the value of this batch_id
    without lock , there will be race condition and the final value could be 2
    With lock , the final value should be 3
     */
    @Test
    public void testMultiThreads() throws InterruptedException
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        Executor executor = RelationalIngestor.getExecutor(H2Sink.get(), JdbcConnection.of(h2Sink.connection()));
        RelationalSinkCleanerTest.createBatchMetadataTableWithData("batch_metadata", "main");
        lockProvider.createAndInitialize(executor, lockInfoDataset, "main");

        Thread t1 = new Thread(new LockTestRunner(lockProvider, H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL));
        Thread t2 = new Thread(new LockTestRunner(lockProvider, H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        String maxBatchIdSql = "select max(table_batch_id) as batch_id from batch_metadata";
        int maxBatchId = (int) h2Sink.executeQuery(maxBatchIdSql).get(0).get("batch_id");
        Assertions.assertEquals(3, maxBatchId);
    }

}
