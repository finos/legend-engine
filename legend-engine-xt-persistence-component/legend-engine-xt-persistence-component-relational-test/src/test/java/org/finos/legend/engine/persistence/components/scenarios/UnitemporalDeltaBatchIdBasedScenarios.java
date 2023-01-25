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
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;

import java.util.Arrays;
import java.util.Optional;

public class UnitemporalDeltaBatchIdBasedScenarios extends BaseTest
{

    /*
    Test Scenarios for Non-temporal Delta
    Variables:
    1) transactionMilestoning = BatchId
    2) deleteIndicator : Enabled, Disabled
    3) DataSplit: Enabled, Disabled

    Valid Combinations:
    1) No Delete Ind, No Data Splits
    2) No Delete Ind, With Data Splits
    3) With Delete Ind, No Data Splits
    4) With Delete Ind, With Data Splits
    5) No Delete Ind, No Data Splits, With Filter Predicates
    */

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS()
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

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .build();

        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaHavingDigestAndDataSplit, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS()
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
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithDeleteIndicator, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS()
    {
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();

        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithDeleteIndicatorWithDataSplit, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_OPTIMIZATION_FILTERS()
    {
        OptimizationFilter filter = OptimizationFilter.of("id", "{ID_LOWER_BOUND}", "{ID_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .optimizationFilters(Arrays.asList(filter))
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_MISSING_OPTIMIZATION_FILTER()
    {
        OptimizationFilter filter = OptimizationFilter.of("unknown_column", "{ID_LOWER_BOUND}", "{ID_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .optimizationFilters(Arrays.asList(filter))
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__WITH_OPTIMIZATION_FILTER_UNSUPPORTED_DATATYPE()
    {
        OptimizationFilter filter = OptimizationFilter.of("name", "{NAME_LOWER_BOUND}", "{NAME_UPPER_BOUND}");
        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .optimizationFilters(Arrays.asList(filter))
                .build();
        return new TestScenario(mainTableWithBatchIdBasedSchema, stagingTableWithBaseSchemaAndDigest, ingestMode);
    }
}
