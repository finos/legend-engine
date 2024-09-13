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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.ErrorCategory;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class BatchErrorDatasetUtilsTest
{

    private final ZonedDateTime executionZonedDateTime = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private final TransformOptions transformOptions = TransformOptions.builder().executionTimestampClock(Clock.fixed(executionZonedDateTime.toInstant(), ZoneOffset.UTC)).build();

    private BatchErrorDataset batchErrorDataset = BatchErrorDataset.builder().name("batch_error_table").build();


    @Test
    public void testInsertBatchError()
    {
        BatchErrorDatasetUtils store = new BatchErrorDatasetUtils(batchErrorDataset, batchErrorDataset.get());
        Insert operation = store.insertBatchError("12345", "dataset_1", ErrorCategory.PARSING_ERROR.getDefaultErrorMessage(), ErrorCategory.PARSING_ERROR.name(), BatchStartTimestamp.INSTANCE);
        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get(), transformOptions);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(operation).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expectedSql = "INSERT INTO batch_error_table (\"ingest_request_id\", \"table_name\", \"error_message\", \"error_category\", \"created_on\") " +
                "(SELECT '12345','dataset_1','Unable to parse file','PARSING_ERROR','2000-01-01 00:00:00.000000')";
        Assertions.assertEquals(expectedSql, list.get(0));
    }
}
