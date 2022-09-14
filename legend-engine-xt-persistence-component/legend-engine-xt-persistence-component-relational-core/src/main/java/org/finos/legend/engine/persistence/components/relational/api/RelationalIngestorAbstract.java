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
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.importer.Importer;
import org.finos.legend.engine.persistence.components.importer.Importers;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.sql.Connection;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.TABLE_IS_NON_EMPTY;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class RelationalIngestorAbstract
{
    private static final String STAGING = "staging";
    private static final String UNDERSCORE = "_";
    private static final String BATCH_ID_PATTERN = "{NEXT_BATCH_ID_PATTERN}";

    //---------- FLAGS ----------

    @Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Default
    public boolean collectStatistics()
    {
        return true;
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
            .batchIdPattern(BATCH_ID_PATTERN);

        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);

        return builder.build();
    }

    // ---------- API ----------

    public IngestorResult ingest(Connection connection, Datasets datasets)
    {
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());
        Executor<SqlGen, TabularData, SqlPlan> executor = new RelationalExecutor(relationalSink(), JdbcHelper.of(connection));

        Resources.Builder resourcesBuilder = Resources.builder();
        Datasets updatedDatasets = datasets;

        // import external dataset reference
        if (updatedDatasets.stagingDataset() instanceof ExternalDatasetReference)
        {
            // update staging dataset reference to imported dataset
            updatedDatasets = importExternalDataset(ingestMode(), updatedDatasets, transformer, executor);
            resourcesBuilder.externalDatasetImported(true);
        }

        // check status of staging table
        if (executor.datasetExists(updatedDatasets.stagingDataset()))
        {
            resourcesBuilder
                .stagingDatasetExists(true)
                .stagingDataSetEmpty(datasetEmpty(updatedDatasets.stagingDataset(), transformer, executor));
        }
        else
        {
            resourcesBuilder.stagingDatasetExists(false);
        }

        // check status of main table
        if (executor.datasetExists(updatedDatasets.mainDataset()))
        {
            // validate whether user-provided schema for main dataset matches schema in db
            validateMainDatasetSchema(updatedDatasets.mainDataset(), executor);
            resourcesBuilder.mainDataSetExists(true);
        }
        else
        {
            resourcesBuilder.mainDataSetExists(false);
        }

        // generate sql plans
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode())
            .relationalSink(relationalSink())
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .enableSchemaEvolution(enableSchemaEvolution())
            .caseConversion(caseConversion())
            .executionTimestampClock(executionTimestampClock())
            .batchIdPattern(BATCH_ID_PATTERN)
            .build();

        GeneratorResult generatorResult = generator.generateOperations(updatedDatasets, resourcesBuilder.build());
        if (generatorResult.schemaEvolutionDataset().isPresent())
        {
            updatedDatasets = updatedDatasets.withMainDataset(generatorResult.schemaEvolutionDataset().get());
        }

        try
        {
            executor.begin();
            // 1. preActionsSqlPlan
            executor.executePhysicalPlan(generatorResult.preActionsSqlPlan());
            // 2. schemaEvolutionSqlPlan
            generatorResult.schemaEvolutionSqlPlan().ifPresent(executor::executePhysicalPlan);
            //  Find the Placeholder
            Map<String, String> placeHolderKeyValues = extractPlaceHolderKeyValues(datasets, executor, transformer, ingestMode());
            // 3. preIngestStatisticsSqlPlan
            Map<StatisticName, Object> statisticsResultMap = new HashMap<>(
                executeStatisticsPhysicalPlan(executor, generatorResult.preIngestStatisticsSqlPlan(), placeHolderKeyValues));
            // 4.ingestSqlPlan
            executor.executePhysicalPlan(generatorResult.ingestSqlPlan(), placeHolderKeyValues);
            // 5. postIngestStatisticsSqlPlan
            statisticsResultMap.putAll(
                executeStatisticsPhysicalPlan(executor, generatorResult.postIngestStatisticsSqlPlan(), placeHolderKeyValues));
            // 6. metadataIngestSqlPlan
            if (generatorResult.metadataIngestSqlPlan().isPresent())
            {
                executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan().get(), placeHolderKeyValues);
            }
            // 7. postActionsSqlPlan
            executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());
            executor.commit();

            return IngestorResult.builder()
                .putAllStatisticByName(statisticsResultMap)
                .updatedDatasets(updatedDatasets)
                .build();
        }
        catch (Exception e)
        {
            executor.revert();
            throw e;
        }
        finally
        {
            executor.close();
        }
    }

    // ---------- UTILITY METHODS ----------

    private Datasets importExternalDataset(IngestMode ingestMode, Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor)
    {
        ExternalDatasetReference externalDatasetReference = (ExternalDatasetReference) datasets.stagingDataset();
        DatasetReference mainDataSetReference = datasets.mainDataset().datasetReference();

        externalDatasetReference = externalDatasetReference
            .withName(externalDatasetReference.name().isPresent() ? externalDatasetReference.name().get() : generateStagingTableName(mainDataSetReference.name().orElseThrow(IllegalStateException::new)))
            .withDatabase(externalDatasetReference.database().isPresent() ? externalDatasetReference.database().get() : mainDataSetReference.database().orElse(null))
            .withGroup(externalDatasetReference.group().isPresent() ? externalDatasetReference.group().get() : mainDataSetReference.group().orElse(null))
            .withAlias(externalDatasetReference.alias().isPresent() ? externalDatasetReference.alias().get() : mainDataSetReference.alias().orElseThrow(RuntimeException::new) + UNDERSCORE + STAGING);

        // TODO : Auto infer schema in future

        // Prepare DigestInfo
        boolean hasDigestField = ingestMode.accept(IngestModeVisitors.DIGEST_REQUIRED);
        Optional<String> digestFieldOptional = ingestMode.accept(IngestModeVisitors.EXTRACT_DIGEST_FIELD);
        boolean populateDigest = hasDigestField && externalDatasetReference.schema().fields().stream().noneMatch(field -> field.name().equalsIgnoreCase(digestFieldOptional.orElseThrow(IllegalStateException::new)));

        if (populateDigest)
        {
            Field digestField = datasets.mainDataset().schema().fields()
                .stream()
                .filter(field -> field.name().equals(digestFieldOptional.orElseThrow(IllegalStateException::new)))
                .findFirst()
                .get();

            List<Field> fields = new ArrayList<>(externalDatasetReference.schema().fields());
            fields.add(digestField);
            externalDatasetReference = externalDatasetReference.withSchema(externalDatasetReference.schema().withFields(fields));
        }

        Dataset extractedStagingDatasetDefinition = externalDatasetReference.getDatasetDefinition();
        Datasets updatedDatasets = datasets.withStagingDataset(extractedStagingDatasetDefinition);

        // Create staging table
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(extractedStagingDatasetDefinition, false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        // Load staging data
        Set<String> metaFields = ingestMode.accept(IngestModeVisitors.META_FIELDS_TO_EXCLUDE_FROM_DIGEST);
        DigestInfo digestInfo = DigestInfo.builder().populateDigest(populateDigest).digestField(digestFieldOptional.orElse(null)).addAllMetaFields(metaFields).build();
        Importer importer = Importers.forExternalDatasetReference(externalDatasetReference, transformer, executor);
        importer.importData(externalDatasetReference, digestInfo);

        return updatedDatasets;
    }

    private boolean datasetEmpty(Dataset dataset, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor)
    {
        LogicalPlan checkIsDatasetEmptyLogicalPlan = LogicalPlanFactory.getLogicalPlanForIsDatasetEmpty(dataset);
        SqlPlan physicalPlanForCheckIsDataSetEmpty = transformer.generatePhysicalPlan(checkIsDatasetEmptyLogicalPlan);
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlanForCheckIsDataSetEmpty);

        String value = String.valueOf(results.stream()
            .findFirst()
            .map(TabularData::getData)
            .flatMap(t -> t.stream().findFirst())
            .map(Map::values)
            .flatMap(t -> t.stream().findFirst())
            .orElseThrow(IllegalStateException::new));
        return !value.equals(TABLE_IS_NON_EMPTY);
    }

    private void validateMainDatasetSchema(Dataset dataset, Executor<SqlGen, TabularData, SqlPlan> executor)
    {
        executor.validateMainDatasetSchema(dataset);
    }

    private String generateStagingTableName(String mainTableName)
    {
        UUID uuid = UUID.randomUUID();
        return mainTableName + UNDERSCORE + STAGING + UNDERSCORE + uuid;
    }

    private Map<StatisticName, Object> executeStatisticsPhysicalPlan(Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                     Map<StatisticName, SqlPlan> statisticsSqlPlan,
                                                                     Map<String, String> placeHolderKeyValues)
    {
        return statisticsSqlPlan.keySet()
            .stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> executor.executePhysicalPlanAndGetResults(statisticsSqlPlan.get(k), placeHolderKeyValues)
                    .stream()
                    .findFirst()
                    .map(TabularData::getData)
                    .flatMap(t -> t.stream().findFirst())
                    .map(Map::values)
                    .flatMap(t -> t.stream().findFirst())
                    .orElseThrow(IllegalStateException::new)));
    }

    private Map<String, String> extractPlaceHolderKeyValues(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                            Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        Map<String, String> placeHolderKeyValues = new HashMap<>();
        Optional<Integer> nextBatchId = getNextBatchId(datasets, executor, transformer, ingestMode);
        if (nextBatchId.isPresent())
        {
            placeHolderKeyValues.put(BATCH_ID_PATTERN, nextBatchId.get().toString());
        }
        return placeHolderKeyValues;
    }

    private Optional<Integer> getNextBatchId(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                             Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        Optional<Integer> nextBatchId = Optional.empty();
        if (ingestMode.accept(IngestModeVisitors.IS_INGEST_MODE_TEMPORAL))
        {
            LogicalPlan logicalPlanForNextBatchId = LogicalPlanFactory.getLogicalPlanForNextBatchId(datasets);
            List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForNextBatchId));
            nextBatchId = Optional.ofNullable((Integer) tabularData.stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .map(Map::values)
                .flatMap(t -> t.stream().findFirst())
                .orElseThrow(IllegalStateException::new));
        }
        return nextBatchId;
    }
}
