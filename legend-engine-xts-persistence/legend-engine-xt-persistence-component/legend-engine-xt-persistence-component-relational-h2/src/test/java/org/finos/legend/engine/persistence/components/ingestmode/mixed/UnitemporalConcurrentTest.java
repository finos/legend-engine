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

import org.finos.legend.engine.persistence.components.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class UnitemporalConcurrentTest extends BaseTest
{
    @Disabled
    @Test
    public void test() throws InterruptedException, IOException
    {

        AtomicInteger maxBatchIdCounter = new AtomicInteger();
        maxBatchIdCounter.set(0);

        // Thread 1
        String path1 = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/without_delete_ind/staging_data_pass1.csv";
        Runnable r1 = new UnitemporalDeltaRunner(path1, "_thread1", H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL, fixedClock_2000_01_01, maxBatchIdCounter);
        Thread t1 = new Thread(r1);
        t1.start();

        // Thread 2
        String path2 = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/without_delete_ind/staging_data_pass2.csv";
        Runnable r2 = new UnitemporalDeltaRunner(path2, "_thread2", H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL, fixedClock_2000_01_01, maxBatchIdCounter);
        Thread t2 = new Thread(r2);
        t2.start();

        // Thread 3
        String path3 = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/without_delete_ind/staging_data_pass3.csv";
        Runnable r3 = new UnitemporalDeltaRunner(path3, "_thread3", H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL, fixedClock_2000_01_01, maxBatchIdCounter);
        Thread t3 = new Thread(r3);
        t3.start();

        // Sleep for a while for tests to finish
        Thread.sleep(10000);

        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from \"TEST\".\"%s\"", "main"));
        Assertions.assertEquals(5, tableData.size());
        Assertions.assertEquals(3, maxBatchIdCounter.get());
    }

}