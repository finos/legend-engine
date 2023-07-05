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
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;

import java.util.Arrays;
import java.util.Optional;

public class BitemporalDeltaSourceSpecifiesFromOnlyScenarios extends BaseTest
{

    /*
    Test Scenarios for Bitemporal Delta
    Variables:
    1) transactionMilestoning: BatchId, BatchIdTime, Time
    2) Delete Indicator: Enabled, Disabled
    3) DataSplit: Enabled, Disabled
    4) dedup: AllowDuplicates, FilterDuplicates

    Valid Combinations:
    1) BatchId Based, No Delete Ind, No Data Splits, Allow Duplicates
    2) BatchId Based, No Delete Ind, With Data Splits, Allow Duplicates
    3) BatchId Based, With Delete Ind, No Data Splits, Allow Duplicates
    4) BatchId Based, With Delete Ind, With Data Splits, Allow Duplicates, Default temp table

    5) BatchId Based, No Delete Ind, No Data Splits, Filter Duplicates
    6) BatchId Based, No Delete Ind, With Data Splits, Filter Duplicates
    3) BatchId Based, With Delete Ind, No Data Splits, Filter Duplicates
    4) BatchId Based, With Delete Ind, With Data Splits, Filter Duplicates, Default temp table

    9) BatchIdDateTime Based, No Delete Ind, No Data Splits, Allow Duplicates
    10) DateTime Based, No Delete Ind, No Data Splits, Allow Duplicates
    */

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .build();
        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchema)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .build();

        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchemaWithDataSplit)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();
        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchemaWithDeleteInd)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS__USING_DEFAULT_TEMP_TABLE()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();
        return new TestScenario(mainTableWithBitemporalFromOnlySchema, stagingTableWithBitemporalFromOnlySchemaWithDeleteIndWithDataSplit, ingestMode);
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS__FILTER_DUPLICATES()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();
        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchema)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .stagingDatasetWithoutDuplicates(stagingTableBitemporalWithoutDuplicates)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__NO_DEL_IND__WITH_DATA_SPLITS__FILTER_DUPLICATES()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        TestScenario testScenario = new TestScenario(ingestMode);
        DatasetDefinition stagingTableWithoutDuplicates = DatasetDefinition.builder()
                .database(stagingWithoutDuplicatesDbName)
                .name(stagingTableWithoutDuplicatesName)
                .alias(stagingTableWithoutDuplicatesAlias)
                .schema(bitemporalFromOnlyStagingTableSchemaWithDataSplit)
                .build();

        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchemaWithDataSplit)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__NO_DATA_SPLITS__FILTER_DUPLICATES()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        TestScenario testScenario = new TestScenario(ingestMode);
        DatasetDefinition stagingTableWithoutDuplicates = DatasetDefinition.builder()
                .database(stagingWithoutDuplicatesDbName)
                .name(stagingTableWithoutDuplicatesName)
                .alias(stagingTableWithoutDuplicatesAlias)
                .schema(bitemporalFromOnlyStagingTableSchemaWithDeleteIndicator)
                .build();
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlySchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchemaWithDeleteInd)
                .tempDataset(tempTableWithBitemporalFromOnlySchema)
                .tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator)
                .stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates)
                .build());
        return testScenario;
    }

    public TestScenario BATCH_ID_BASED__WITH_DEL_IND__WITH_DATA_SPLITS__FILTER_DUPLICATES()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        return new TestScenario(mainTableWithBitemporalFromOnlySchema, stagingTableWithBitemporalFromOnlySchemaWithDeleteIndWithDataSplit, ingestMode);
    }


    public TestScenario BATCH_ID_AND_TIME_BASED__NO_DEL_IND__NO_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .build();
        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlyWithBatchIdAndTimeBasedSchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchema)
                .tempDataset(tempTableWithBitemporalFromOnlyWithBatchIdAndTimeBasedSchema)
                .build());
        return testScenario;
    }

    public TestScenario DATETIME_BASED__NO_DEL_IND__NO_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .build())
                        .build())
                .build();
        TestScenario testScenario = new TestScenario(ingestMode);
        testScenario.setDatasets(Datasets.builder()
                .mainDataset(mainTableWithBitemporalFromOnlyWithDateTimeBasedSchema)
                .stagingDataset(stagingTableWithBitemporalFromOnlySchema)
                .tempDataset(tempTableWithBitemporalFromOnlyWithDateTimeBasedSchema)
                .build());
        return testScenario;
    }
}
