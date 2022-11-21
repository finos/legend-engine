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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class RelationalGeneratorAbstract
{
    //---------- FLAGS ----------

    @Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Default
    public boolean collectStatistics()
    {
        return false;
    }

    @Default
    public boolean enableSchemaEvolution()
    {
        return false;
    }

    @Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    public abstract Optional<String> batchStartTimestampPattern();

    public abstract Optional<String> batchEndTimestampPattern();

    public abstract Optional<String> batchIdPattern();

    //---------- FIELDS ----------

    public abstract IngestMode ingestMode();

    public abstract RelationalSink relationalSink();

    @Derived
    protected PlannerOptions plannerOptions()
    {
        return PlannerOptions.builder()
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .enableSchemaEvolution(enableSchemaEvolution())
            .build();
    }

    @Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder()
            .executionTimestampClock(executionTimestampClock())
            .batchStartTimestampPattern(batchStartTimestampPattern())
            .batchEndTimestampPattern(batchEndTimestampPattern())
            .batchIdPattern(batchIdPattern());

        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);

        return builder.build();
    }

    // ---------- API ----------

    public GeneratorResult generateOperations(Datasets datasets)
    {
        return generateOperations(datasets, Resources.builder().build());
    }

    public GeneratorResult generateOperationsForEmptyBatch(Datasets datasets)
    {
        return generateOperations(datasets, Resources.builder().stagingDataSetEmpty(true).build());
    }

    public List<GeneratorResult> generateOperationsWithDataSplits(Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        GeneratorResult generatorResult = generateOperations(datasets);
        return dataSplitRanges.stream()
            .map(generatorResult::withIngestDataSplitRange)
            .collect(Collectors.toList());
    }

    //TODO: generateOperationsWithDataSplitsForEmptyBatch


    // ---------- UTILITY METHODS ----------

    GeneratorResult generateOperations(Datasets datasets, Resources resources)
    {
        Planner planner = Planners.get(datasets, ingestMode(), plannerOptions());
        return generateOperations(datasets, resources, planner);
    }

    GeneratorResult generateOperations(Datasets datasets, Resources resources, Planner planner)
    {
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());

        // pre-run statistics
        Map<StatisticName, LogicalPlan> preIngestStatisticsLogicalPlan = planner.buildLogicalPlanForPreRunStatistics(resources);
        Map<StatisticName, SqlPlan> preIngestStatisticsSqlPlan = new HashMap<>();
        for (StatisticName statistic : preIngestStatisticsLogicalPlan.keySet())
        {
            preIngestStatisticsSqlPlan.put(statistic, transformer.generatePhysicalPlan(preIngestStatisticsLogicalPlan.get(statistic)));
        }

        // pre-actions
        LogicalPlan preActionsLogicalPlan = planner.buildLogicalPlanForPreActions(resources);
        SqlPlan preActionsSqlPlan = transformer.generatePhysicalPlan(preActionsLogicalPlan);

        // schema evolution
        Optional<SqlPlan> schemaEvolutionSqlPlan = Optional.empty();
        Optional<Dataset> schemaEvolutionDataset = Optional.empty();
        if (enableSchemaEvolution())
        {
            // Get logical plan and physical plan for schema evolution and update datasets
            SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink(), ingestMode());
            SchemaEvolutionResult schemaEvolutionResult = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasets.mainDataset(), datasets.stagingDataset());
            LogicalPlan schemaEvolutionLogicalPlan = schemaEvolutionResult.logicalPlan();

            schemaEvolutionSqlPlan = Optional.of(transformer.generatePhysicalPlan(schemaEvolutionLogicalPlan));
            schemaEvolutionDataset = Optional.of(schemaEvolutionResult.evolvedDataset());

            // update main dataset with evolved schema and re-initialize planner
            planner = Planners.get(datasets.withMainDataset(schemaEvolutionDataset.get()), ingestMode(), plannerOptions());
        }

        // ingest
        LogicalPlan ingestLogicalPlan = planner.buildLogicalPlanForIngest(resources, relationalSink().capabilities());
        SqlPlan ingestSqlPlan = transformer.generatePhysicalPlan(ingestLogicalPlan);

        // metadata-ingest
        LogicalPlan metaDataIngestLogicalPlan = planner.buildLogicalPlanForMetadataIngest(resources);
        Optional<SqlPlan> metaDataIngestSqlPlan = Optional.empty();
        if (metaDataIngestLogicalPlan != null)
        {
            metaDataIngestSqlPlan = Optional.of(transformer.generatePhysicalPlan(metaDataIngestLogicalPlan));
        }
        // post-actions
        LogicalPlan postActionsLogicalPlan = planner.buildLogicalPlanForPostActions(resources);
        SqlPlan postActionsSqlPlan = transformer.generatePhysicalPlan(postActionsLogicalPlan);

        // post-run statistics
        Map<StatisticName, LogicalPlan> postIngestStatisticsLogicalPlan = planner.buildLogicalPlanForPostRunStatistics(resources);
        Map<StatisticName, SqlPlan> postIngestStatisticsSqlPlan = new HashMap<>();
        for (StatisticName statistic : postIngestStatisticsLogicalPlan.keySet())
        {
            postIngestStatisticsSqlPlan.put(statistic, transformer.generatePhysicalPlan(postIngestStatisticsLogicalPlan.get(statistic)));
        }

        return GeneratorResult.builder()
            .preActionsSqlPlan(preActionsSqlPlan)
            .schemaEvolutionSqlPlan(schemaEvolutionSqlPlan)
            .schemaEvolutionDataset(schemaEvolutionDataset)
            .ingestSqlPlan(ingestSqlPlan)
            .postActionsSqlPlan(postActionsSqlPlan)
            .metadataIngestSqlPlan(metaDataIngestSqlPlan)
            .putAllPreIngestStatisticsSqlPlan(preIngestStatisticsSqlPlan)
            .putAllPostIngestStatisticsSqlPlan(postIngestStatisticsSqlPlan)
            .build();
    }
}
