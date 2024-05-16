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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalLockProvider;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class RelationalLockProviderTest extends BaseTest
{

    @Test
    public void testInitializeLockInfo()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        Executor executor = RelationalIngestor.getExecutor(H2Sink.get(), JdbcConnection.of(h2Sink.connection()));
        lockProvider.createAndInitialize(executor, lockInfoDataset, "main");
        List<Map<String, Object>> result = h2Sink.executeQuery("select * from main_legend_persistence_lock");
        Assertions.assertEquals("2000-01-01 00:00:00.0", result.get(0).get("insert_ts_utc").toString());
        Assertions.assertEquals(null, result.get(0).get("last_used_ts_utc"));
        Assertions.assertEquals("main", result.get(0).get("table_name"));
    }

    @Test
    public void testAcquireLockInfo()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        Executor executor = RelationalIngestor.getExecutor(H2Sink.get(), JdbcConnection.of(h2Sink.connection()));
        lockProvider.createAndInitialize(executor, lockInfoDataset, "main");
        lockProvider.lock(executor, lockInfoDataset);

        List<Map<String, Object>> result = h2Sink.executeQuery("select * from main_legend_persistence_lock");
        Assertions.assertEquals("2000-01-01 00:00:00.0", result.get(0).get("insert_ts_utc").toString());
        Assertions.assertEquals("2000-01-01 00:00:00.0", result.get(0).get("last_used_ts_utc").toString());
        Assertions.assertEquals("main", result.get(0).get("table_name"));
    }
}
