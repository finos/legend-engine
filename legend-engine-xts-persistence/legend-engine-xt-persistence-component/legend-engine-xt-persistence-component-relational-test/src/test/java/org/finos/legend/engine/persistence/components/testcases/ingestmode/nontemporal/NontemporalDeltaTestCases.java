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

import java.util.Collections;
import java.util.List;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.NonTemporalDeltaScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class NontemporalDeltaTestCases extends BaseTest
{
    NonTemporalDeltaScenarios scenarios = new NonTemporalDeltaScenarios();

    @Test
    void testNontemporalDeltaNoAuditingNoDedupNoVersioning()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .enableConcurrentSafety(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaNoAuditingNoDedupNoVersioning(operations);
    }

    public abstract void verifyNontemporalDeltaNoAuditingNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__WITH_DELETE_INDICATOR__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning(operations);
    }

    public abstract void verifyNontemporalDeltaNoAuditingWithDeleteIndicatorNoDedupNoVersioning(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithAuditingFilterDupsNoVersioning()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__FILTER_DUPLICATES__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .collectStatistics(true)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithAuditingFilterDupsNoVersioning(operations);
    }

    public abstract void verifyNontemporalDeltaWithAuditingFilterDupsNoVersioning(GeneratorResult operations);

    @Test
    void testNonTemporalDeltaNoAuditingNoDedupAllVersion()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__ALL_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId("075605e3-bada-47d7-9ae9-7138f392fe22")
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(testScenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyNonTemporalDeltaNoAuditingNoDedupAllVersion(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyNonTemporalDeltaNoAuditingNoDedupAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__ALL_VERSION_WITHOUT_PERFORM();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(testScenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyNonTemporalDeltaNoAuditingNoDedupAllVersionWithoutPerform(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion()
    {
        TestScenario testScenario = scenarios.WITH_AUDTING__FAIL_ON_DUPS__ALL_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .collectStatistics(true)
                .ingestRunId(ingestRunId)
                .build();

        List<GeneratorResult> operations = generator.generateOperationsWithDataSplits(testScenario.getDatasets(), dataSplitRangesOneToTwo);
        verifyNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion(operations, dataSplitRangesOneToTwo);
    }

    public abstract void verifyNonTemporalDeltaWithWithAuditingFailOnDupsAllVersion(List<GeneratorResult> operations, List<DataSplitRange> dataSplitRanges);

    @Test
    void testNontemporalDeltaWithUpperCaseOptimizer()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .caseConversion(CaseConversion.TO_UPPER)
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithUpperCaseOptimizer(operations);
    }

    public abstract void verifyNontemporalDeltaWithUpperCaseOptimizer(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithLessColumnsInStaging()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        Dataset stagingTable = testScenario.getStagingTable().withSchema(stagingTableSchemaWithLimitedColumns);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);

        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

        GeneratorResult operations = generator.generateOperations(datasets);
        verifyNontemporalDeltaWithLessColumnsInStaging(operations);
    }

    public abstract void verifyNontemporalDeltaWithLessColumnsInStaging(GeneratorResult operations);

    @Test
    void testNontemporalDeltaValidationPkFieldsMissing()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        // Staging table has no pks
        Dataset stagingTable = testScenario.getStagingTable().withSchema(stagingTableSchemaWithNoPrimaryKeys);
        Datasets datasets = Datasets.of(testScenario.getMainTable(), stagingTable);
        try
        {
            RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .executionTimestampClock(fixedClock_2000_01_01)
                .build();

            GeneratorResult operations = generator.generateOperations(datasets);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Primary key list must not be empty", e.getMessage());
        }
    }

    @Test
    void testNontemporalDeltaValidationDateTimeFieldMissing()
    {
        try
        {
            NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestField)
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
    public void testNontemporalDeltaPostActionSqlAndCleanStagingData()
    {
        TestScenario testScenario = scenarios.NO_AUDTING__NO_DEDUP__NO_VERSIONING();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .cleanupStagingData(true)
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();
        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaPostActionSqlAndCleanStagingData(operations);
    }

    public abstract void verifyNontemporalDeltaPostActionSqlAndCleanStagingData(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithNoVersionAndStagingFilter()
    {
        TestScenario testScenario = scenarios.NO_VERSIONING__WITH_STAGING_FILTER();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .cleanupStagingData(false)
                .collectStatistics(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithNoVersionAndStagingFilter(operations);
    }

    public abstract void verifyNontemporalDeltaWithNoVersionAndStagingFilter(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithNoVersionAndFilteredDataset()
    {
        TestScenario testScenario = scenarios.NO_VERSIONING__WITH_FILTERED_DATASET();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .cleanupStagingData(false)
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .putAllAdditionalMetadata(Collections.singletonMap("watermark", "my_watermark_value"))
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithNoVersionAndFilteredDataset(operations);
    }

    public abstract void verifyNontemporalDeltaWithNoVersionAndFilteredDataset(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithFilterDupsMaxVersionWithStagingFilters()
    {
        TestScenario testScenario = scenarios.FILTER_DUPS__MAX_VERSION__WITH_STAGING_FILTER();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .executionTimestampClock(fixedClock_2000_01_01)
            .cleanupStagingData(false)
            .collectStatistics(true)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithFilterDupsMaxVersionWithStagingFilters(operations);
    }

    public abstract void verifyNontemporalDeltaWithFilterDupsMaxVersionWithStagingFilters(GeneratorResult operations);

    @Test
    void testNontemporalDeltaWithNoDedupMaxVersioningWithoutPerformWithStagingFilters()
    {
        TestScenario testScenario = scenarios.NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM__WITH_STAGING_FILTER();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .cleanupStagingData(false)
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaWithNoDedupMaxVersioningWithoutPerformWithStagingFilters(operations);
    }

    public abstract void verifyNontemporalDeltaWithNoDedupMaxVersioningWithoutPerformWithStagingFilters(GeneratorResult operations);

    @Test
    void testNontemporalDeltaNoDedupMaxVersionWithoutPerform()
    {
        TestScenario testScenario = scenarios.NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaNoDedupMaxVersionWithoutPerform(operations);
    }

    public abstract void verifyNontemporalDeltaNoDedupMaxVersionWithoutPerform(GeneratorResult operations);

    @Test
    void testNontemporalDeltaAllowDuplicatesMaxVersionWithUpperCase()
    {
        TestScenario testScenario = scenarios.NO_DEDUP__MAX_VERSION();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .sampleRowCount(10)
            .ingestRunId(ingestRunId)
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNontemporalDeltaAllowDuplicatesMaxVersionWithUpperCase(operations);
    }

    public abstract void verifyNontemporalDeltaAllowDuplicatesMaxVersionWithUpperCase(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}
