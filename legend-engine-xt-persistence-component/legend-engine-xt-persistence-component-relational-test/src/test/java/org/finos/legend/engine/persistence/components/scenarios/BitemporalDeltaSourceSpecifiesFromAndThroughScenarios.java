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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;

import java.util.Arrays;
import java.util.Optional;

public class BitemporalDeltaSourceSpecifiesFromAndThroughScenarios extends BaseTest
{

    /*
    Test Scenarios for Bitemporal Delta
    Variables:
    1) transactionMilestoning: BatchId, BatchIdTime, Datetime
    2) Delete Indicator: Enabled, Disabled
    3) DataSplit: Enabled, Disabled

    Valid Combinations:
    1) BatchId Based, No Delete Ind, No Data Splits
    2) BatchIdAndTime Based, No Delete Ind, With Data Splits
    3) BatchId Based, With Delete Ind, No Data Splits
    4) Datetime Based, With Delete Ind, With Data Splits
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
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .sourceDateTimeThruField(validityThroughReferenceField)
                                .build())
                        .build())
                .build();
        return new TestScenario(mainTableWithBitemporalSchema, stagingTableWithBitemporalSchema, ingestMode);
    }

    public TestScenario BATCH_ID_AND_TIME_BASED__NO_DEL_IND__WITH_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(Optional.of(dataSplitField))
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInField)
                        .batchIdOutName(batchIdOutField)
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .sourceDateTimeThruField(validityThroughReferenceField)
                                .build())
                        .build())
                .build();
        return new TestScenario(mainTableWithBitemporalSchemaWithBatchIdAndTime, stagingTableWithBitemporalSchemaWithDataSplit, ingestMode);
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
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .sourceDateTimeThruField(validityThroughReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();
        return new TestScenario(mainTableWithBitemporalSchema, stagingTableWithBitemporalSchemaWithDeleteIndicator, ingestMode);
    }

    public TestScenario DATETIME_BASED__WITH_DEL_IND__WITH_DATA_SPLITS()
    {
        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestField)
                .dataSplitField(dataSplitField)
                .transactionMilestoning(TransactionDateTime.builder()
                        .dateTimeInName(batchTimeInField)
                        .dateTimeOutName(batchTimeOutField)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(validityFromTargetField)
                        .dateTimeThruName(validityThroughTargetField)
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(validityFromReferenceField)
                                .sourceDateTimeThruField(validityThroughReferenceField)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorField)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();
        return new TestScenario(mainTableWithBitemporalSchemaWithDateTime, stagingTableWithBitemporalSchemaWithDeleteIndicatorAndDataSplit, ingestMode);
    }
}
