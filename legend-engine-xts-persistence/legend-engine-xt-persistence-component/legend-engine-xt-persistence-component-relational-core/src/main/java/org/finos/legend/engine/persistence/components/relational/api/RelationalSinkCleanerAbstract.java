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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.*;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)


public abstract class RelationalSinkCleanerAbstract
{
    //---------- FIELDS ----------
    public abstract RelationalSink relationalSink();

    public abstract Dataset mainDataset();

    public abstract Optional<LockInfoDataset> lockInfoDataset();

    public abstract String requestedBy();

    @Default
    public SinkCleanupAuditDataset auditDataset()
    {
        return SinkCleanupAuditDataset.builder().build();
    }

    @Default
    public MetadataDataset metadataDataset()
    {
        return MetadataDataset.builder().build();
    }

    @Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    @Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Default
    public boolean enableConcurrentSafety()
    {
        return false;
    }

    @Value.Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder()
                .executionTimestampClock(executionTimestampClock());

        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);

        return builder.build();
    }

    // ---------- Private Fields ----------
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalSinkCleaner.class);

    // ------API-----
    public SinkCleanupGeneratorResult generateOperationsForSinkCleanup()
    {
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());

        // Pre-action SQL
        LogicalPlan preActionsLogicalPlan = buildLogicalPlanForPreActions();
        SqlPlan preActionsSqlPlan = transformer.generatePhysicalPlan(preActionsLogicalPlan);

        // initialize-lock SQL
        LogicalPlan initializeLockLogicalPlan = buildLogicalPlanForInitializeLock();
        Optional<SqlPlan> initializeLockSqlPlan = Optional.empty();
        if (initializeLockLogicalPlan != null)
        {
            initializeLockSqlPlan = Optional.of(transformer.generatePhysicalPlan(initializeLockLogicalPlan));
        }

        // acquire-lock SQL
        LogicalPlan acquireLockLogicalPlan = buildLogicalPlanForAcquireLock();
        Optional<SqlPlan> acquireLockSqlPlan = Optional.empty();
        if (acquireLockLogicalPlan != null)
        {
            acquireLockSqlPlan = Optional.of(transformer.generatePhysicalPlan(acquireLockLogicalPlan));
        }

        //Sink clean-up SQL's
        LogicalPlan cleanupLogicalPlan = buildLogicalPlanForCleanupActions();
        SqlPlan cleanupSqlPlan = transformer.generatePhysicalPlan(cleanupLogicalPlan);
        return SinkCleanupGeneratorResult.builder()
                .preActionsSqlPlan(preActionsSqlPlan)
                .cleanupSqlPlan(cleanupSqlPlan)
                .initializeLockSqlPlan(initializeLockSqlPlan)
                .acquireLockSqlPlan(acquireLockSqlPlan)
                .build();
    }

    //todo : ApiUtils.getLockInfoDataset() ?
    public SinkCleanupIngestorResult executeOperationsForSinkCleanup(RelationalConnection connection)
    {
        SinkCleanupIngestorResult ingestorResult;

        // 1. initialize connection
        initExecutor(connection);

        // 2. Generate sink cleanup Operations
        // todo : need to enrich datastes? (enrichedDatasets = ApiUtils.enrichAndApplyCase(datasets, caseConversion());)
        LOGGER.info("Generating SQL's for sink cleanup");
        SinkCleanupGeneratorResult result = generateOperationsForSinkCleanup();

        //3. Create datasets
        LOGGER.info("Creating the datasets");
        executor.executePhysicalPlan(result.preActionsSqlPlan());
        if (enableConcurrentSafety())
        {
            LOGGER.info("Concurrent safety is enabled, Initializing lock");
            executor.executePhysicalPlan(result.initializeLockSqlPlan().get());
        }

        //4. Execute sink cleanup operations
        try
        {
            executor.begin();
            if (enableConcurrentSafety())
            {
                LOGGER.info("Concurrent safety is enabled, acquiring lock");
                executor.executePhysicalPlan(result.acquireLockSqlPlan().get());
            }
            LOGGER.info("Executing SQL's for sink cleanup");
            executor.executePhysicalPlan(result.cleanupSqlPlan());
            executor.commit();
            ingestorResult = SinkCleanupIngestorResult.builder().status(IngestStatus.SUCCEEDED).build();
        }
        //todo : throw exception vs return failure status?
        catch (Exception e)
        {
            executor.revert();
            throw e;
        }
        finally
        {
            executor.close();
        }
        return ingestorResult;
    }

    // ---------- UTILITY METHODS ----------

    private Executor initExecutor(RelationalConnection connection)
    {
        LOGGER.info("Invoked initExecutor method, will initialize the executor");
        this.executor = relationalSink().getRelationalExecutor(connection);
        return executor;
    }

    private LogicalPlan buildLogicalPlanForPreActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Create.of(true, auditDataset().get()));
        if (enableConcurrentSafety())
        {
            operations.add(Create.of(true, lockInfoDataset().orElseThrow(IllegalStateException::new).get()));
        }
        return LogicalPlan.of(operations);
    }

    private LogicalPlan buildLogicalPlanForCleanupActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Drop.of(true, mainDataset(), true));
        operations.add(buildDeleteCondition());
        operations.add(buildInsertCondition());
        return LogicalPlan.of(operations);
    }

    private Operation buildDeleteCondition()
    {
        StringValue mainTableName = getMainTableName();
                FieldValue tableNameFieldValue = FieldValue.builder().datasetRef(metadataDataset().get().datasetReference()).fieldName(metadataDataset().tableNameField()).build();
        FunctionImpl tableNameInUpperCase = FunctionImpl.builder().functionName(FunctionName.UPPER).addValue(tableNameFieldValue).build();
        StringValue mainTableNameInUpperCase = StringValue.builder().value(mainTableName.value().map(field -> field.toUpperCase()))
                .alias(mainTableName.alias()).build();
        Condition whereCondition = Equals.of(tableNameInUpperCase, mainTableNameInUpperCase);
        return Delete.of(metadataDataset().get(), whereCondition);
    }

    private Operation buildInsertCondition()
    {
        DatasetReference auditTableRef = this.auditDataset().get().datasetReference();
        FieldValue tableName = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().tableNameField()).build();
        FieldValue startTs = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().batchStartTimeField()).build();
        FieldValue endTs = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().batchEndTimeField()).build();
        FieldValue batchStatus = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().batchStatusField()).build();
        FieldValue requestedBy = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().requestedBy()).build();

        List<org.finos.legend.engine.persistence.components.logicalplan.values.Value> fieldsToInsert = new ArrayList<>();
        fieldsToInsert.add(tableName);
        fieldsToInsert.add(startTs);
        fieldsToInsert.add(endTs);
        fieldsToInsert.add(batchStatus);
        fieldsToInsert.add(requestedBy);

        List<org.finos.legend.engine.persistence.components.logicalplan.values.Value> selectFields = new ArrayList<>();
        selectFields.add(getMainTableName());
        selectFields.add(BatchStartTimestamp.INSTANCE);
        selectFields.add(BatchEndTimestamp.INSTANCE);
        selectFields.add(StringValue.of(MetadataUtils.MetaTableStatus.DONE.toString()));
        selectFields.add(StringValue.of(requestedBy()));

        return Insert.of(auditDataset().get(), Selection.builder().addAllFields(selectFields).build(), fieldsToInsert);
    }

    private StringValue getMainTableName()
    {
        return StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new));
    }

    private LogicalPlan buildLogicalPlanForInitializeLock()
    {
        if (enableConcurrentSafety())
        {
            LockInfoUtils lockInfoUtils = new LockInfoUtils(lockInfoDataset().orElseThrow(IllegalStateException::new));
            return LogicalPlan.of(Collections.singleton(lockInfoUtils.initializeLockInfo(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new), BatchStartTimestampAbstract.INSTANCE)));
        }
        return null;
    }

    private LogicalPlan buildLogicalPlanForAcquireLock()
    {
        if (enableConcurrentSafety())
        {
            LockInfoUtils lockInfoUtils = new LockInfoUtils(lockInfoDataset().orElseThrow(IllegalStateException::new));
            return LogicalPlan.of(Collections.singleton(lockInfoUtils.updateLockInfo(BatchStartTimestampAbstract.INSTANCE)));
        }
        return null;
    }
}
