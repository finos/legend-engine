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

import java.util.List;

public abstract class AppendOnlyTestCases extends BaseTest
{
    AppendOnlyScenarios scenarios = new AppendOnlyScenarios();

    @Test
    void testAppendOnlyAllowDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.ALLOW_DUPLICATES_NO_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyAllowDuplicatesNoAuditing(operations);
    }

    public abstract void verifyAppendOnlyAllowDuplicatesNoAuditing(GeneratorResult operations);

    @Test
    void testAppendOnlyAllowDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.ALLOW_DUPLICATES_WITH_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyAllowDuplicatesWithAuditing(operations);
    }

    public abstract void verifyAppendOnlyAllowDuplicatesWithAuditing(GeneratorResult operations);

    @Test
    void testAppendOnlyFailOnDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.FAIL_ON_DUPLICATES_NO_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyFailOnDuplicatesNoAuditing(operations);
    }

    public abstract void verifyAppendOnlyFailOnDuplicatesNoAuditing(GeneratorResult operations);

    @Test
    void testAppendOnlyFailOnDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.FAIL_ON_DUPLICATES_WITH_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyFailOnDuplicatesWithAuditing(operations);
    }

    public abstract void verifyAppendOnlyFailOnDuplicatesWithAuditing(GeneratorResult operations);

    @Test
    void testAppendOnlyFilterDuplicatesNoAuditing()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyFilterDuplicatesNoAuditing(operations);
    }

    public abstract void verifyAppendOnlyFilterDuplicatesNoAuditing(GeneratorResult operations);

    @Test
    void testAppendOnlyFilterDuplicatesWithAuditing()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_WITH_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .cleanupStagingData(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .build();

        GeneratorResult queries = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyFilterDuplicatesWithAuditing(queries);
    }

    public abstract void verifyAppendOnlyFilterDuplicatesWithAuditing(GeneratorResult queries);

    @Test
    void testAppendOnlyFilterDuplicatesNoAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING_WITH_DATA_SPLIT();
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
            Assertions.assertEquals("DataSplits not supported for NoAuditing mode", e.getMessage());
        }
    }

    @Test
    public void testAppendOnlyFilterDuplicatesWithAuditingWithDataSplit()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_WITH_AUDITING_WITH_DATA_SPLIT();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(scenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(scenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyAppendOnlyFilterDuplicatesWithAuditingWithDataSplit(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyAppendOnlyFilterDuplicatesWithAuditingWithDataSplit(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testAppendOnlyWithUpperCaseOptimizer()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(scenario.getDatasets());
        verifyAppendOnlyWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyAppendOnlyWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testAppendOnlyWithLessColumnsInStaging()
    {
        TestScenario scenario = scenarios.FILTER_DUPLICATES_NO_AUDITING();
        Dataset stagingTable = scenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(scenario.getMainTable(), stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(scenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyAppendOnlyWithLessColumnsInStaging(operations);
    }

    public abstract void verifyAppendOnlyWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testAppendOnlyValidationPkFieldsMissing()
    {
        TestScenario testScenario = scenarios.FILTER_DUPLICATES_NO_AUDITING();
        // Staging table has no pks
        Dataset stagingTable = testScenario.getStagingTable().withSchema(baseTableSchemaWithNoPrimaryKeys);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);
        try
        {
            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();
            GeneratorResult queries = generator.generateOperations(datasets);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Primary key list must not be empty", e.getMessage());
        }
    }

    @Test
    void testAppendOnlyValidationDateTimeFieldMissing()
    {
        try
        {
            AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestField)
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

    public abstract RelationalSink getRelationalSink();
}
