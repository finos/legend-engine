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

import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.RelationalLockProvider;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RelationalLockProviderTest extends IngestModeTest
{

    @Test
    public void testInitializeLockInfo()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().database("mydb").name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(AnsiSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        SqlPlan physicalPlan = lockProvider.createAndInitializeLockSqlPlan(lockInfoDataset, "main");
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO \"mydb\".\"main_legend_persistence_lock\" (\"insert_ts_utc\", \"table_name\") " +
                "(SELECT '2000-01-01 00:00:00.000000','main' WHERE NOT (EXISTS (SELECT * FROM \"mydb\".\"main_legend_persistence_lock\" as main_legend_persistence_lock)))";
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableCreateQuery, list.get(0));
        Assertions.assertEquals(expectedSql, list.get(1));
    }

    @Test
    public void testInitializeLockInfoUpperCase()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().database("mydb").name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(AnsiSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        SqlPlan physicalPlan = lockProvider.createAndInitializeLockSqlPlan(lockInfoDataset, "main");
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" " +
                "(\"INSERT_TS_UTC\", \"TABLE_NAME\") " +
                "(SELECT '2000-01-01 00:00:00.000000','main' " +
                "WHERE NOT (EXISTS (SELECT * FROM \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" as MAIN_LEGEND_PERSISTENCE_LOCK)))";
        Assertions.assertEquals(AnsiTestArtifacts.expectedLockInfoTableUpperCaseCreateQuery, list.get(0));
        Assertions.assertEquals(expectedSql, list.get(1));
    }

    @Test
    public void testAcquireLock()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().database("mydb").name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(AnsiSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        SqlPlan physicalPlan = lockProvider.acquireLockSqlPlan(lockInfoDataset);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "UPDATE \"mydb\".\"main_legend_persistence_lock\" as main_legend_persistence_lock SET main_legend_persistence_lock.\"last_used_ts_utc\" = '2000-01-01 00:00:00.000000'";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testAcquireLockUpperCase()
    {
        LockInfoDataset lockInfoDataset = LockInfoDataset.builder().database("mydb").name("main_legend_persistence_lock").build();
        RelationalLockProvider lockProvider = RelationalLockProvider.builder()
                .relationalSink(AnsiSqlSink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .caseConversion(CaseConversion.TO_UPPER)
                .build();
        SqlPlan physicalPlan = lockProvider.acquireLockSqlPlan(lockInfoDataset);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "UPDATE \"MYDB\".\"MAIN_LEGEND_PERSISTENCE_LOCK\" as MAIN_LEGEND_PERSISTENCE_LOCK SET MAIN_LEGEND_PERSISTENCE_LOCK.\"LAST_USED_TS_UTC\" = '2000-01-01 00:00:00.000000'";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

}
