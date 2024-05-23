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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestampAbstract;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.LockInfoUtils;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static org.finos.legend.engine.persistence.components.relational.api.RelationalIngestorAbstract.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.transformer.Transformer.TransformOptionsAbstract.DATE_TIME_FORMATTER;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public abstract class RelationalLockProviderAbstract
{
    //---------- FIELDS ----------
    public abstract RelationalSink relationalSink();

    @Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Default
    public SqlLogging sqlLogging()
    {
        return SqlLogging.DISABLED;
    }

    @Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    @Value.Derived
    protected Transformer<SqlGen, SqlPlan> transformer()
    {
        TransformOptions.Builder builder = TransformOptions.builder().executionTimestampClock(executionTimestampClock());
        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);
        TransformOptions transformOptions = builder.build();
        return new RelationalTransformer(relationalSink(), transformOptions);
    }

    // ---------- Private Fields ----------
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalLockProvider.class);

    // ------API-----
    public void createAndInitialize(Executor<SqlGen, TabularData, SqlPlan> executor, LockInfoDataset lockDataset, String tableName)
    {
        executor.setSqlLogging(sqlLogging());
        LOGGER.info(String.format("Creating and initializing Lock dataset: %s, Case Conversion %s", lockDataset.name(), caseConversion()));
        Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
        executor.executePhysicalPlan(createAndInitializeLockSqlPlan(lockDataset, tableName), placeHolderKeyValues);
    }

    public void acquireLock(Executor<SqlGen, TabularData, SqlPlan> executor, LockInfoDataset lockDataset)
    {
        executor.setSqlLogging(sqlLogging());
        LOGGER.info(String.format("Acquiring lock with Lock dataset: %s, Case Conversion %s", lockDataset.name(), caseConversion()));
        Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
        executor.executePhysicalPlan(acquireLockSqlPlan(lockDataset), placeHolderKeyValues);
    }

    // ------ Utility methods -----
    public SqlPlan createAndInitializeLockSqlPlan(LockInfoDataset lockInfoDataset, String tableName)
    {
        LockInfoDataset enrichedLockInfoDataset = ApiUtils.applyCase(lockInfoDataset, caseConversion());
        List<Operation> operations = new ArrayList<>();
        LockInfoUtils lockInfoUtils = new LockInfoUtils(enrichedLockInfoDataset);
        operations.add(Create.of(true, enrichedLockInfoDataset.get()));
        operations.add(lockInfoUtils.initializeLockInfo(tableName, BatchStartTimestampAbstract.INSTANCE));
        LogicalPlan initializeLockLogicalPlan = LogicalPlan.of(operations);
        return transformer().generatePhysicalPlan(initializeLockLogicalPlan);
    }

    public SqlPlan acquireLockSqlPlan(LockInfoDataset lockInfoDataset)
    {
        LockInfoDataset enrichedLockInfoDataset = ApiUtils.applyCase(lockInfoDataset, caseConversion());
        LockInfoUtils lockInfoUtils = new LockInfoUtils(enrichedLockInfoDataset);
        LogicalPlan acquireLockLogicalPlan = LogicalPlan.of(Collections.singleton(lockInfoUtils.updateLockInfo(BatchStartTimestampAbstract.INSTANCE)));
        return transformer().generatePhysicalPlan(acquireLockLogicalPlan);
    }

}
