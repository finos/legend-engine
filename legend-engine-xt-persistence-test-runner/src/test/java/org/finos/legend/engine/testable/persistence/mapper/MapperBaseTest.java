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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.IngestMode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.UnitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;

import java.util.Arrays;

public class MapperBaseTest
{
    // NonTemporalSnapshot IngestMode
    protected IngestMode getNonTemporalSnapshotNoAuditing()
    {
        NontemporalSnapshot ingestMode = new NontemporalSnapshot();
        ingestMode.auditing = new NoAuditing();
        return ingestMode;
    }

    protected IngestMode getNonTemporalSnapshotDateTimeAuditing()
    {
        NontemporalSnapshot ingestMode = new NontemporalSnapshot();
        ingestMode.auditing = getDatetimeAuditing();
        return ingestMode;
    }

    // Append Only IngestMode
    protected IngestMode getAppendOnlyNoAuditingNoFilteringDuplicates()
    {
        AppendOnly ingestMode = new AppendOnly();
        ingestMode.auditing = new NoAuditing();
        ingestMode.filterDuplicates = false;
        return ingestMode;
    }

    protected IngestMode getAppendOnlyNoAuditingWithFilteringDuplicates()
    {
        AppendOnly ingestMode = new AppendOnly();
        ingestMode.auditing = new NoAuditing();
        ingestMode.filterDuplicates = true;
        return ingestMode;
    }

    protected IngestMode getAppendOnlyDatetimeAuditingNoFilteringDuplicates()
    {
        AppendOnly ingestMode = new AppendOnly();
        ingestMode.auditing = getDatetimeAuditing();
        ingestMode.filterDuplicates = false;
        return ingestMode;
    }

    protected IngestMode getAppendOnlyDatetimeAuditingWithFilteringDuplicates()
    {
        AppendOnly ingestMode = new AppendOnly();
        ingestMode.auditing = getDatetimeAuditing();
        ingestMode.filterDuplicates = true;
        return ingestMode;
    }

    // NonTemporalDelta IngestMode
    protected IngestMode getNontemporalDeltaNoAuditingNoMergeStrategy()
    {
        NontemporalDelta ingestMode = new NontemporalDelta();
        ingestMode.auditing = new NoAuditing();
        ingestMode.mergeStrategy = new NoDeletesMergeStrategy();
        return ingestMode;
    }

    protected IngestMode getNontemporalDeltaNoAuditingDeleteIndMergeStrategy()
    {
        NontemporalDelta ingestMode = new NontemporalDelta();
        ingestMode.auditing = new NoAuditing();
        ingestMode.mergeStrategy = getDeleteIndicatorMergeStrategy();
        return ingestMode;
    }

    protected IngestMode getNontemporalDeltaWithAuditingNoMergeStrategy()
    {
        NontemporalDelta ingestMode = new NontemporalDelta();
        ingestMode.auditing = getDatetimeAuditing();
        ingestMode.mergeStrategy = new NoDeletesMergeStrategy();
        return ingestMode;
    }

    protected IngestMode getNontemporalDeltaWithAuditingDeleteIndMergeStrategy()
    {
        NontemporalDelta ingestMode = new NontemporalDelta();
        ingestMode.auditing = getDatetimeAuditing();
        ingestMode.mergeStrategy = getDeleteIndicatorMergeStrategy();
        return ingestMode;
    }

    // UnitemporalDelta IngestMode
    protected IngestMode getUnitempDeltaNoMergeBatchIdBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = new NoDeletesMergeStrategy();
        ingestMode.transactionMilestoning = getBatchIdTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitempDeltaNoMergeBatchIdAndTimeBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = new NoDeletesMergeStrategy();
        ingestMode.transactionMilestoning = getBatchIdAndTimeTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitempDeltaNoMergeTimeBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = new NoDeletesMergeStrategy();
        ingestMode.transactionMilestoning = getBatchTimeTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitempDeltaDelIndMergeBatchIdBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = getDeleteIndicatorMergeStrategy();
        ingestMode.transactionMilestoning = getBatchIdTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitempDeltaDelIndMergeBatchIdAndTimeBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = getDeleteIndicatorMergeStrategy();
        ingestMode.transactionMilestoning = getBatchIdAndTimeTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitempDeltaDelIndMergeTimeBased()
    {
        UnitemporalDelta ingestMode = new UnitemporalDelta();
        ingestMode.mergeStrategy = getDeleteIndicatorMergeStrategy();
        ingestMode.transactionMilestoning = getBatchTimeTransactionMilestoning();
        return ingestMode;
    }

    // UnitemporalSnapshot IngestMode
    protected IngestMode getUnitemporalSnapshotBatchIdBased()
    {
        UnitemporalSnapshot ingestMode = new UnitemporalSnapshot();
        ingestMode.transactionMilestoning = getBatchIdTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitemporalSnapshotBatchIdAndTimeBased()
    {
        UnitemporalSnapshot ingestMode = new UnitemporalSnapshot();
        ingestMode.transactionMilestoning = getBatchIdAndTimeTransactionMilestoning();
        return ingestMode;
    }

    protected IngestMode getUnitemporalSnapshotTimeBased()
    {
        UnitemporalSnapshot ingestMode = new UnitemporalSnapshot();
        ingestMode.transactionMilestoning = getBatchTimeTransactionMilestoning();
        return ingestMode;
    }

    protected DateTimeAuditing getDatetimeAuditing()
    {
        DateTimeAuditing auditing = new DateTimeAuditing();
        auditing.dateTimeName = "AUDIT_TIME";
        return auditing;
    }

    protected DeleteIndicatorMergeStrategy getDeleteIndicatorMergeStrategy()
    {
        DeleteIndicatorMergeStrategy mergeStrategy = new DeleteIndicatorMergeStrategy();
        mergeStrategy.deleteField = "DELETE_INDICATOR";
        mergeStrategy.deleteValues = Arrays.asList("1");
        return mergeStrategy;
    }

    protected BatchIdTransactionMilestoning getBatchIdTransactionMilestoning()
    {
        BatchIdTransactionMilestoning milestoning = new BatchIdTransactionMilestoning();
        milestoning.batchIdInName = "BATCH_ID_IN";
        milestoning.batchIdOutName = "BATCH_ID_OUT";
        return milestoning;
    }

    protected BatchIdAndDateTimeTransactionMilestoning getBatchIdAndTimeTransactionMilestoning()
    {
        BatchIdAndDateTimeTransactionMilestoning milestoning = new BatchIdAndDateTimeTransactionMilestoning();
        milestoning.batchIdInName = "BATCH_ID_IN";
        milestoning.batchIdOutName = "BATCH_ID_OUT";
        milestoning.dateTimeInName = "BATCH_TIME_IN";
        milestoning.dateTimeOutName = "BATCH_TIME_OUT";
        return milestoning;
    }

    protected DateTimeTransactionMilestoning getBatchTimeTransactionMilestoning()
    {
        DateTimeTransactionMilestoning milestoning = new DateTimeTransactionMilestoning();
        milestoning.dateTimeInName = "BATCH_TIME_IN";
        milestoning.dateTimeOutName = "BATCH_TIME_OUT";
        return milestoning;
    }

}
