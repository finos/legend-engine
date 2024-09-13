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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.operations.*;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.relational.api.utils.ApiUtils.LOCK_INFO_DATASET_SUFFIX;

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

    public abstract String requestedBy();

    public abstract MetadataDataset metadataDataset();

    public abstract Optional<LockInfoDataset> lockDataset();

    @Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    @Value.Derived
    protected TransformOptions transformOptions()
    {
        return TransformOptions.builder().executionTimestampClock(executionTimestampClock()).build();
    }

    // ---------- Private Fields ----------
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalSinkCleaner.class);

    // ------API-----
    public SinkCleanupGeneratorResult generateOperationsForSinkCleanup()
    {
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());

        //Sink clean-up SQL's
        LogicalPlan dropLogicalPlan = buildLogicalPlanForDropActions();
        SqlPlan dropSqlPlan = transformer.generatePhysicalPlan(dropLogicalPlan);
        return SinkCleanupGeneratorResult.builder()
                .dropSqlPlan(dropSqlPlan)
                .build();
    }

    public SinkCleanupIngestorResult executeOperationsForSinkCleanup(RelationalConnection connection)
    {
        // 1. initialize connection
        initExecutor(connection);

        // 2. Generate sink cleanup Operations
        LOGGER.info("Generating SQL's for sink cleanup");
        SinkCleanupGeneratorResult result = generateOperationsForSinkCleanup();

        //3. Execute sink cleanup operations
        LOGGER.info("Executing SQL's for sink cleanup");
        return executeSinkCleanup(result);
    }

    /*
   - Get Executor
   - @return : The methods returns the Executor to the caller enabling them to handle their own transaction
   */
    public static Executor getExecutor(RelationalSink relationalSink, RelationalConnection connection)
    {
        LOGGER.info("Invoked getExecutor method");
        return relationalSink.getRelationalExecutor(connection);
    }

    /*
   - Initializes executor
   - @return : The methods returns the Executor to the caller enabling them to handle their own transaction
   */
    public Executor initExecutor(RelationalConnection connection)
    {
        LOGGER.info("Invoked initExecutor method, will initialize the executor");
        this.executor = relationalSink().getRelationalExecutor(connection);
        return executor;
    }

    // ---------- UTILITY METHODS ----------

    private LogicalPlan buildLogicalPlanForDropActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Drop.of(true, mainDataset(), false));
        operations.add(buildDropPlanForLockTable());
        operations.add(Drop.of(true, metadataDataset().get(), false));
        return LogicalPlan.of(operations);
    }

    private Operation buildDropPlanForLockTable()
    {
        LockInfoDataset lockInfoDataset;
        if (lockDataset().isPresent())
        {
            lockInfoDataset = lockDataset().get();
        }
        else
        {
            String datasetName = mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new);
            String lockDatasetName = datasetName + LOCK_INFO_DATASET_SUFFIX;
            lockInfoDataset = LockInfoDataset.builder()
                    .database(mainDataset().datasetReference().database())
                    .group(mainDataset().datasetReference().group())
                    .name(lockDatasetName)
                    .build();
        }
        return Drop.of(true, lockInfoDataset.get(), false);
    }

    private SinkCleanupIngestorResult executeSinkCleanup(SinkCleanupGeneratorResult result)
    {
        SinkCleanupIngestorResult ingestorResult;
        try
        {
            executor.executePhysicalPlan(result.dropSqlPlan());
            ingestorResult = SinkCleanupIngestorResult.builder()
                    .status(SinkCleanupStatus.SUCCEEDED).build();
        }
        catch (Exception e)
        {
            ingestorResult = SinkCleanupIngestorResult.builder()
                    .status(SinkCleanupStatus.FAILED)
                    .message(e.toString())
                    .build();
        }
        finally
        {
            executor.close();
        }
        return ingestorResult;
    }
}
