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

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.api.RelationalLockProvider;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;

public class LockTestRunner implements Runnable
{
    private RelationalLockProvider lockProvider;
    private JdbcHelper h2Sink;

    public LockTestRunner(RelationalLockProvider lockProvider, String h2User, String h2Pwd, String h2JdbcUrl)
    {
        this.lockProvider = lockProvider;
        this.h2Sink = JdbcHelper.of(H2Sink.createConnection(h2User, h2Pwd, h2JdbcUrl));
    }

    @Override
    public void run()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().name("main_legend_persistence_lock").build();
        Executor executor = RelationalIngestor.getExecutor(H2Sink.get(), JdbcConnection.of(h2Sink.connection()));

        executor.begin();
        lockProvider.acquireLock(executor, lockInfoDataset);
        String sql = "select max(table_batch_id) as batch_id from batch_metadata";
        int nextBatchId = ((int) h2Sink.executeQuery(sql).get(0).get("batch_id")) + 1;
        String insertNextBatch = "INSERT INTO batch_metadata" +
            " (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\")" +
            " (SELECT 'main'," + nextBatchId + ",'2000-01-01 00:00:00.000000','2000-01-01 00:00:00.000000','DONE')";
        h2Sink.executeStatement(insertNextBatch);
        executor.commit();
    }
}
