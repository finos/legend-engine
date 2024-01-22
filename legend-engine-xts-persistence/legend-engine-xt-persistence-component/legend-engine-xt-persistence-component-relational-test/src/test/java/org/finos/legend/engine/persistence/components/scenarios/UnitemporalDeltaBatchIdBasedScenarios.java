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

package org.finos.legend.engine.persistence.components.scenarios;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.*;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;

import java.util.Arrays;

public class UnitemporalDeltaBatchIdBasedScenarios extends BaseTest
{
    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) transactionMilestoning = BatchId
    2) deleteIndicator : Enabled, Disabled
    3) Deduplication: Allow duplicates, Filter duplicates, Fail on duplicates
    4) Versioning: No Versioning, Max Versioning, All Versioning
    */


    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__NO_VERSION()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DEDUP__ALL_VERSION_WITHOUT_PERFORM()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").dataSplitFieldName(dataSplitField).mergeDataVersionResolver(DigestBasedResolver.INSTANCE).performStageVersioning(false).build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__FILTER_DUPS__NO_VERSIONING()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithDeleteIndicator, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__NO_DEDUP__ALL_VERSION()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .versioningStrategy(AllVersionsStrategy.builder().versioningField("biz_date").mergeDataVersionResolver(DigestBasedResolver.INSTANCE).dataSplitFieldName(dataSplitField).build())
                .build();

        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithDeleteIndicator, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTERS()
    {
        OptimizationFilter filter = OptimizationFilter.of("id", "{ID_LOWER_BOUND}", "{ID_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .addOptimizationFilters(filter)
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTERS__INCLUDES_NULL_VALUES()
    {
        OptimizationFilter filter = OptimizationFilter.of("id", "{ID_LOWER_BOUND}", "{ID_UPPER_BOUND}").withIncludesNullValues(true);
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .addOptimizationFilters(filter)
            .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_MISSING_OPTIMIZATION_FILTER()
    {
        OptimizationFilter filter = OptimizationFilter.of("unknown_column", "{ID_LOWER_BOUND}", "{ID_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .addOptimizationFilters(filter)
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_OPTIMIZATION_FILTER_UNSUPPORTED_DATATYPE()
    {
        OptimizationFilter filter = OptimizationFilter.of("name", "{NAME_LOWER_BOUND}", "{NAME_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .addOptimizationFilters(filter)
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_VERSIONING__WITH_STAGING_FILTER()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(NoVersioningStrategy.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithFilter, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_VERSIONING__WITH_FILTERED_DATASET()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, filteredStagingTable, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__FILTER_DUPS__MAX_VERSION__WITH_STAGING_FILTER()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(MaxVersionStrategy.builder().performStageVersioning(true).versioningField(version.name()).mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN)).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndVersionBasedSchema, stagingTableWithFilterAndVersion, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__FILTER_DUPS__MAX_VERSION__WITH_FILTERED_DATASET()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestField)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInField)
                .batchIdOutName(batchIdOutField)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder().performStageVersioning(true).versioningField(version.name()).mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN)).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();
        return new TestScenario(mainTableWithBatchIdAndVersionBasedSchema, filteredStagingTableWithVersion, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEDUP__MAX_VERSION_WITHOUT_PERFORM__WITH_STAGING_FILTER()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(MaxVersionStrategy.builder().performStageVersioning(false).versioningField(version.name()).mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN)).build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndVersionBasedSchema, stagingTableWithFilterAndVersion, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__FAIL_ON_DUPS__MAX_VERSIONING_WITHOUT_PERFORM__NO_STAGING_FILTER()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(MaxVersionStrategy.builder().performStageVersioning(false).versioningField(version.name()).mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN)).build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndVersionBasedSchema, stagingTableWithVersion, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEDUP__MAX_VERSIONING__NO_STAGING_FILTER()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .versioningStrategy(MaxVersionStrategy.builder().performStageVersioning(true).versioningField(version.name()).mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO)).build())
                .build();
        return new TestScenario(mainTableWithBatchIdAndVersionBasedSchema, stagingTableWithVersion, ingestMode);
    }
}
