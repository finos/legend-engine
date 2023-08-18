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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class LockInfoUtilsTest
{

    private final ZonedDateTime executionZonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final TransformOptions transformOptions = TransformOptions.builder().executionTimestampClock(Clock.fixed(executionZonedDateTime.toInstant(), ZoneOffset.UTC)).build();

    private LockInfoDataset lockInfoDataset = LockInfoDataset.builder().name("main_table_lock").build();


    @Test
    public void testInitializeLockInfo()
    {
        LockInfoUtils store = new LockInfoUtils(lockInfoDataset);
        Insert operation = store.initializeLockInfo("main", BatchStartTimestamp.INSTANCE);
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO main_table_lock (\"insert_ts_utc\", \"table_name\") " +
                "(SELECT '2000-01-01 00:00:00','main' WHERE NOT (EXISTS (SELECT * FROM main_table_lock as main_table_lock)))";
        Assertions.assertEquals(expectedSql, list.get(0));
    }

    @Test
    public void testUpdateMetaStore()
    {
        LockInfoUtils store = new LockInfoUtils(lockInfoDataset);
        Update operation = store.updateLockInfo(BatchStartTimestamp.INSTANCE);
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "UPDATE main_table_lock as main_table_lock SET main_table_lock.\"last_used_ts_utc\" = '2000-01-01 00:00:00'";
        Assertions.assertEquals(expectedSql, list.get(0));
    }
}
