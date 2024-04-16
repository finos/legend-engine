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
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public abstract class RelationalSchemaEvolutionServiceAbstract
{
    //---------- FIELDS ----------
    public abstract RelationalSink relationalSink();

    public abstract IngestMode ingestMode();

    @Default
    public Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet()
    {
        return Collections.emptySet();
    }

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

    @Value.Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder();
        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);
        return builder.build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalSchemaEvolutionService.class);

    // ------API-----
    public SchemaEvolutionServiceResult evolve(DatasetReference mainDatasetReference, SchemaDefinition stagingSchema, RelationalConnection connection)
    {
        LOGGER.info("Invoked evolve method, will evolve the target dataset");

        // 1. Initialize executor and transformer
        LOGGER.info("Initializing executor and transformer");
        Executor<SqlGen, TabularData, SqlPlan> executor = relationalSink().getRelationalExecutor(connection);
        executor.setSqlLogging(sqlLogging());
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());

        // 2. Handle case conversion
        IngestMode ingestMode = ApiUtils.applyCase(ingestMode(), caseConversion());
        mainDatasetReference = ApiUtils.applyCase(mainDatasetReference, caseConversion());
        stagingSchema = ApiUtils.applyCase(stagingSchema, caseConversion());

        // 3. Check if main dataset exists
        LOGGER.info("Checking if target dataset exists");
        if (!executor.datasetExists(mainDatasetReference))
        {
            return SchemaEvolutionServiceResult.builder()
                .status(SchemaEvolutionStatus.FAILED)
                .message("Dataset is not found: " + mainDatasetReference.datasetReference().name().orElseThrow(IllegalStateException::new))
                .build();
        }

        // 4. Derive main dataset schema
        LOGGER.info("Constructing target dataset schema from database");
        Dataset mainDataset = executor.constructDatasetFromDatabase(mainDatasetReference);

        // 5. Generate schema evolution operations
        LOGGER.info("Generating schema evolution operations");
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink(), ingestMode, schemaEvolutionCapabilitySet());
        SchemaEvolutionResult schemaEvolutionResult = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainDataset, stagingSchema);
        LogicalPlan schemaEvolutionLogicalPlan = schemaEvolutionResult.logicalPlan();

        // 6. Execute schema evolution operations
        LOGGER.info("Starting schema evolution execution");
        List<String> executedSqls = new ArrayList<>();
        if (!schemaEvolutionLogicalPlan.ops().isEmpty())
        {
            for (Operation op : schemaEvolutionLogicalPlan.ops())
            {
                // Recreating a logical plan per operation such that we can keep track of executed SQLs in case of partial failure
                SqlPlan singleOperationAlterSqlPlan = transformer.generatePhysicalPlan(LogicalPlan.of(Collections.singletonList(op)));
                try
                {
                    executor.executePhysicalPlan(singleOperationAlterSqlPlan);
                    executedSqls.addAll(singleOperationAlterSqlPlan.getSqlList());
                }
                catch (Exception e)
                {
                    LOGGER.info("Encountered error in executing schema evolution");
                    return SchemaEvolutionServiceResult.builder()
                        .status(executedSqls.isEmpty() ? SchemaEvolutionStatus.FAILED : SchemaEvolutionStatus.PARTIALLY_SUCCEEDED)
                        .addAllExecutedSchemaEvolutionSqls(executedSqls)
                        .message(e.getMessage())
                        .build();
                }
            }
        }

        LOGGER.info("Schema evolution completed");
        return SchemaEvolutionServiceResult.builder()
            .status(SchemaEvolutionStatus.SUCCEEDED)
            .addAllExecutedSchemaEvolutionSqls(executedSqls)
            .build();
    }
}
