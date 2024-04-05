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

package org.finos.legend.engine.persistence.components.testcases.ingestmode.nontemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.AppendOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public abstract class AppendOnlyTestCases extends BaseTest
{
    AppendOnlyScenarios scenarios = new AppendOnlyScenarios();

    @Test
    void testAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecords()
    {
        TestScenario scenario = scenarios.NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .createStagingDataset(true)
                .enableConcurrentSafety(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecordsDeriveMainSchema(operations);
    }

    @Test
    void testAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecordsDeriveMainSchema()
    {
        TestScenario scenario = scenarios.NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS__DERIVE_MAIN_SCHEMA();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .createStagingDataset(true)
                .enableConcurrentSafety(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecordsDeriveMainSchema(operations);
    }

    public abstract void verifyAppendOnlyNoAuditingNoDedupNoVersioningNoFilterExistingRecordsDeriveMainSchema(GeneratorResult operations);

    @Test
    void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecords()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .ingestRunId(ingestRunId)
                .build();
        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecords(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecords(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges);

    @Test
    void testAppendOnlyWithAuditingFilterDuplicatesNoVersioningWithFilterExistingRecords()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FILTER_DUPS__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .cleanupStagingData(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();

        GeneratorResult queries = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyWithAuditingFilterDuplicatesNoVersioningWithFilterExistingRecords(queries);
    }

    public abstract void verifyAppendOnlyWithAuditingFilterDuplicatesNoVersioningWithFilterExistingRecords(GeneratorResult queries);

    @Test
    void testAppendOnlyNoAuditingValidation()
    {
        TestScenario scenario = scenarios.NO_AUDITING__FILTER_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();
        try
        {
            List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        }
        catch (Exception e)
        {
            Assertions.assertEquals("NoAuditing not allowed when there are primary keys", e.getMessage());
        }
    }

    @Test
    public void testAppendOnlyWithAuditingFilterDuplicatesAllVersionWithFilterExistingRecords()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FILTER_DUPS__ALL_VERSION__WITH_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .ingestRunId(ingestRunId)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFilterDuplicatesAllVersionWithFilterExistingRecords(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyAppendOnlyWithAuditingFilterDuplicatesAllVersionWithFilterExistingRecords(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testAppendOnlyWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FILTER_DUPS__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .caseConversion(CaseConversion.TO_UPPER)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyAppendOnlyWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testAppendOnlyWithLessColumnsInStaging()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FILTER_DUPS__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS();
        Dataset stagingTable = scenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(scenario.getMainTable(), stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyAppendOnlyWithLessColumnsInStaging(operations);
    }

    public abstract void verifyAppendOnlyWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testAppendOnlyWithAuditingFailOnDuplicatesMaxVersionWithFilterExistingRecords()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FAIL_ON_DUPS__MAX_VERSION__WITH_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyWithAuditingFailOnDuplicatesMaxVersionWithFilterExistingRecords(operations);
    }

    public abstract void verifyAppendOnlyWithAuditingFailOnDuplicatesMaxVersionWithFilterExistingRecords(GeneratorResult operations);

    @Test
    void testAppendOnlyWithAuditingFilterDupsMaxVersionNoFilterExistingRecords()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FILTER_DUPS__MAX_VERSION__NO_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyWithAuditingFilterDupsMaxVersionNoFilterExistingRecords(operations);
    }

    public abstract void verifyAppendOnlyWithAuditingFilterDupsMaxVersionNoFilterExistingRecords(GeneratorResult operations);

    @Test
    void testAppendOnlyValidationDateTimeFieldMissing()
    {
        try
        {
            AppendOnly ingestMode = AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestField).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().build())
                .build();
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build DateTimeAuditing, some of required attributes are not set [dateTimeField]", e.getMessage());
        }
    }

    @Test
    void testAppendOnlyNoAuditingFilterExistingRecords()
    {
        TestScenario scenario = scenarios.NO_AUDITING__NO_DEDUP__NO_VERSIONING__WITH_FILTER_EXISTING_RECORDS();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .build();
        try
        {
            List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Primary keys and digest are mandatory for filterExistingRecords", e.getMessage());
        }
    }

    @Test
    public void testAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration()
    {
        TestScenario scenario = scenarios.NO_AUDITING__NO_DEDUP__NO_VERSIONING__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration(operations);
    }

    public abstract void verifyAppendOnlyNoAuditingAllowDuplicatesNoVersioningNoFilterExistingRecordsUdfDigestGeneration(GeneratorResult operations);

    @Test
    public void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGeneration(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges);

    @Test
    public void testAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf()
    {
        TestScenario scenario = scenarios.WITH_AUDITING__FAIL_ON_DUPS__ALL_VERSION__NO_FILTER_EXISTING_RECORDS__UDF_DIGEST_GENERATION__TYPE_CONVERSION_UDF();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId(ingestRunId)
            .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyAppendOnlyWithAuditingFailOnDuplicatesAllVersionNoFilterExistingRecordsUdfDigestGenerationTypeConversionUdf(List<GeneratorResult> generatorResults, List<DataSplitRange> dataSplitRanges);

    public abstract RelationalSink getRelationalSink();
}
