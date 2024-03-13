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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.SinkCleanupAuditDataset;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

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

    @Default
    public SinkCleanupAuditDataset auditDataset()
    {
        return SinkCleanupAuditDataset.builder().build();
    }

    public abstract MetadataDataset metadataDataset();

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

        // Pre-action SQL
        LogicalPlan preActionsLogicalPlan = buildLogicalPlanForPreActions();
        SqlPlan preActionsSqlPlan = transformer.generatePhysicalPlan(preActionsLogicalPlan);

        //Sink clean-up SQL's
        LogicalPlan dropLogicalPlan = buildLogicalPlanForDropActions();
        SqlPlan dropSqlPlan = transformer.generatePhysicalPlan(dropLogicalPlan);
        LogicalPlan cleanupLogicalPlan = buildLogicalPlanForCleanupAndAuditActions();
        SqlPlan cleanupSqlPlan = transformer.generatePhysicalPlan(cleanupLogicalPlan);
        return SinkCleanupGeneratorResult.builder()
                .preActionsSqlPlan(preActionsSqlPlan)
                .cleanupSqlPlan(cleanupSqlPlan)
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

        //3. Create datasets
        LOGGER.info("Creating the datasets");
        executor.executePhysicalPlan(result.preActionsSqlPlan());

        //4. Execute sink cleanup operations
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

    private LogicalPlan buildLogicalPlanForPreActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Create.of(true, auditDataset().get()));
        return LogicalPlan.of(operations);
    }

    private LogicalPlan buildLogicalPlanForDropActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Drop.of(true, mainDataset(), false));
        return LogicalPlan.of(operations);
    }

    private LogicalPlan buildLogicalPlanForCleanupAndAuditActions()
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(buildDeleteCondition());
        operations.add(buildInsertCondition(AuditTableStatus.SUCCEEDED));
        return LogicalPlan.of(operations);
    }

    private Operation buildDeleteCondition()
    {
        StringValue mainTableName = getMainTable();
        FieldValue tableNameFieldValue = FieldValue.builder().datasetRef(metadataDataset().get().datasetReference()).fieldName(metadataDataset().tableNameField()).build();
        FunctionImpl tableNameInUpperCase = FunctionImpl.builder().functionName(FunctionName.UPPER).addValue(tableNameFieldValue).build();
        StringValue mainTableNameInUpperCase = StringValue.builder()
                .value(mainTableName.value().map(field -> field.toUpperCase()))
                .alias(mainTableName.alias()).build();
        Condition whereCondition = Equals.of(tableNameInUpperCase, mainTableNameInUpperCase);
        return Delete.of(metadataDataset().get(), whereCondition);
    }

    private Operation buildInsertCondition(AuditTableStatus status)
    {
        DatasetReference auditTableRef = this.auditDataset().get().datasetReference();
        FieldValue tableName = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().tableNameField()).build();
        FieldValue executionTs = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().executionTimeField()).build();
        FieldValue auditStatus = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().statusField()).build();
        FieldValue requestedBy = FieldValue.builder().datasetRef(auditTableRef).fieldName(auditDataset().requestedBy()).build();

        List<org.finos.legend.engine.persistence.components.logicalplan.values.Value> fieldsToInsert = new ArrayList<>();
        fieldsToInsert.add(tableName);
        fieldsToInsert.add(executionTs);
        fieldsToInsert.add(auditStatus);
        fieldsToInsert.add(requestedBy);

        List<org.finos.legend.engine.persistence.components.logicalplan.values.Value> selectFields = new ArrayList<>();
        selectFields.add(getMainTable());
        selectFields.add(BatchStartTimestamp.INSTANCE);
        selectFields.add(StringValue.of(status.name()));
        selectFields.add(StringValue.of(requestedBy()));

        return Insert.of(auditDataset().get(), Selection.builder().addAllFields(selectFields).build(), fieldsToInsert);
    }

    private StringValue getMainTable()
    {
        return StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new));
    }

    private SinkCleanupIngestorResult executeSinkCleanup(SinkCleanupGeneratorResult result)
    {
        SinkCleanupIngestorResult ingestorResult;
        try
        {
            executor.executePhysicalPlan(result.dropSqlPlan());
                try
                {
                    executor.begin();
                    LOGGER.info("Executing SQL's for sink cleanup");
                    executor.executePhysicalPlan(result.cleanupSqlPlan());
                    executor.commit();
                    ingestorResult = SinkCleanupIngestorResult.builder().status(IngestStatus.SUCCEEDED).build();
                }
                catch (Exception e)
                {
                    executor.revert();
                    ingestorResult = SinkCleanupIngestorResult.builder()
                            .status(IngestStatus.FAILED)
                            .message(e.toString())
                            .build();
                }
            }
        catch (Exception e)
        {
            ingestorResult = SinkCleanupIngestorResult.builder()
                    .status(IngestStatus.FAILED)
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
