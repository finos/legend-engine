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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.AnsiTestArtifacts;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.testcases.ingestmode.NoOpTestCases;
import org.junit.jupiter.api.Assertions;

import java.util.List;

import static org.finos.legend.engine.persistence.components.AnsiTestArtifacts.*;

public class NoOpTest extends NoOpTestCases
{
    @Override
    public void verifyNoOp(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();

        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQuery, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableCreateQuery, preActionsSql.get(1));

        Assertions.assertEquals(0, milestoningSql.size());

        Assertions.assertEquals(getExpectedMetadataTableIngestQuery(), metadataIngestSql.get(0));
        Assertions.assertEquals(lockInitializedQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredQuery, acquireLockSql.get(0));

        verifyStats(operations, null, null, null, null, null);
    }

    protected String getExpectedMetadataTableIngestQuery()
    {
        return "INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\", \"ingest_request_id\") (SELECT 'main',(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE','123456789')";
    }

    @Override
    public void verifyNoOpUpperCase(GeneratorResult operations)
    {
        List<String> preActionsSql = operations.preActionsSql();
        List<String> milestoningSql = operations.ingestSql();
        List<String> metadataIngestSql = operations.metadataIngestSql();
        List<String> initializeLockSql = operations.initializeLockSql();
        List<String> acquireLockSql = operations.acquireLockSql();

        Assertions.assertEquals(AnsiTestArtifacts.expectedMetadataTableCreateQueryWithUpperCase, preActionsSql.get(0));
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableUpperCaseCreateQuery, preActionsSql.get(1));
        Assertions.assertEquals(0, milestoningSql.size());
        String expectedMetadataTableIngestQuery = "INSERT INTO BATCH_METADATA (\"TABLE_NAME\", \"TABLE_BATCH_ID\", \"BATCH_START_TS_UTC\", \"BATCH_END_TS_UTC\", \"BATCH_STATUS\", \"INGEST_REQUEST_ID\") (SELECT 'MAIN',(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN'),'2000-01-01 00:00:00.000000',CURRENT_TIMESTAMP(),'DONE','123456789')";
        Assertions.assertEquals(expectedMetadataTableIngestQuery, metadataIngestSql.get(0));
        Assertions.assertEquals(lockInitializedUpperCaseQuery, initializeLockSql.get(0));
        Assertions.assertEquals(lockAcquiredUpperCaseQuery, acquireLockSql.get(0));

        verifyStats(operations, null, null, null, null, null);
    }

    @Override
    public RelationalSink getRelationalSink()
    {
        return AnsiSqlSink.get();
    }
}
