// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.*;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.util.function.Function;
import java.util.stream.Collectors;

public class IngestModeCaseConverter implements IngestModeVisitor<IngestMode>
{
    protected Function<String, String> strategy;

    public IngestModeCaseConverter(Function<String, String> strategy)
    {
        this.strategy = strategy;
    }

    @Override
    public IngestMode visitAppendOnly(AppendOnlyAbstract appendOnly)
    {
        return AppendOnly
                .builder()
                .digestField(applyCase(appendOnly.digestField()))
                .auditing(appendOnly.auditing().accept(new AuditingCaseConverter()))
                .deduplicationStrategy(appendOnly.deduplicationStrategy())
                .versioningStrategy(appendOnly.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .filterExistingRecords(appendOnly.filterExistingRecords())
                .build();
    }

    @Override
    public IngestMode visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
    {
        return NontemporalSnapshot
                .builder()
                .auditing(nontemporalSnapshot.auditing().accept(new AuditingCaseConverter()))
                .deduplicationStrategy(nontemporalSnapshot.deduplicationStrategy())
                .versioningStrategy(nontemporalSnapshot.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    @Override
    public IngestMode visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
    {
        return NontemporalDelta
                .builder()
                .digestField(applyCase(nontemporalDelta.digestField()))
                .mergeStrategy(nontemporalDelta.mergeStrategy().accept(new MergeStrategyCaseConverter()))
                .auditing(nontemporalDelta.auditing().accept(new AuditingCaseConverter()))
                .deduplicationStrategy(nontemporalDelta.deduplicationStrategy())
                .versioningStrategy(nontemporalDelta.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    @Override
    public IngestMode visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
    {
        return UnitemporalSnapshot
                .builder()
                .digestField(applyCase(unitemporalSnapshot.digestField()))
                .transactionMilestoning(unitemporalSnapshot.transactionMilestoning().accept(new TransactionMilestoningCaseConverter()))
                .addAllPartitionFields(applyCase(unitemporalSnapshot.partitionFields()))
                .putAllPartitionValuesByField(applyCase(unitemporalSnapshot.partitionValuesByField()))
                .emptyDatasetHandling(unitemporalSnapshot.emptyDatasetHandling())
                .deduplicationStrategy(unitemporalSnapshot.deduplicationStrategy())
                .versioningStrategy(unitemporalSnapshot.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    @Override
    public IngestMode visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
    {
        return UnitemporalDelta
                .builder()
                .digestField(applyCase(unitemporalDelta.digestField()))
                .addAllOptimizationFilters(unitemporalDelta.optimizationFilters().stream().map(filter -> applyCase(filter)).collect(Collectors.toList()))
                .transactionMilestoning(unitemporalDelta.transactionMilestoning().accept(new TransactionMilestoningCaseConverter()))
                .mergeStrategy(unitemporalDelta.mergeStrategy().accept(new MergeStrategyCaseConverter()))
                .deduplicationStrategy(unitemporalDelta.deduplicationStrategy())
                .versioningStrategy(unitemporalDelta.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    @Override
    public IngestMode visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
    {
        return BitemporalSnapshot
                .builder()
                .digestField(applyCase(bitemporalSnapshot.digestField()))
                .transactionMilestoning(bitemporalSnapshot.transactionMilestoning().accept(new TransactionMilestoningCaseConverter()))
                .validityMilestoning(bitemporalSnapshot.validityMilestoning().accept(new ValidityMilestoningCaseConverter()))
                .addAllPartitionFields(applyCase(bitemporalSnapshot.partitionFields()))
                .putAllPartitionValuesByField(applyCase(bitemporalSnapshot.partitionValuesByField()))
                .deduplicationStrategy(bitemporalSnapshot.deduplicationStrategy())
                .versioningStrategy(bitemporalSnapshot.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    @Override
    public IngestMode visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
    {
        return BitemporalDelta
                .builder()
                .digestField(applyCase(bitemporalDelta.digestField()))
                .transactionMilestoning(bitemporalDelta.transactionMilestoning().accept(new TransactionMilestoningCaseConverter()))
                .validityMilestoning(bitemporalDelta.validityMilestoning().accept(new ValidityMilestoningCaseConverter()))
                .deduplicationStrategy(bitemporalDelta.deduplicationStrategy())
                .mergeStrategy(bitemporalDelta.mergeStrategy().accept(new MergeStrategyCaseConverter()))
                .versioningStrategy(bitemporalDelta.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .filterExistingRecords(bitemporalDelta.filterExistingRecords())
                .build();
    }

    @Override
    public IngestMode visitBulkLoad(BulkLoadAbstract bulkLoad)
    {
        return BulkLoad.builder()
                .batchIdField(applyCase(bulkLoad.batchIdField()))
                .digestGenStrategy(bulkLoad.digestGenStrategy().accept(new DigestGenStrategyCaseConverter()))
                .auditing(bulkLoad.auditing().accept(new AuditingCaseConverter()))
                .deduplicationStrategy(bulkLoad.deduplicationStrategy())
                .versioningStrategy(bulkLoad.versioningStrategy().accept(new VersionStrategyCaseConverter()))
                .build();
    }

    private Optional<String> applyCase(Optional<String> field)
    {
        return Optional.ofNullable(field.isPresent() ? this.strategy.apply(field.get()) : null);
    }

    private String applyCase(String field)
    {
        return this.strategy.apply(field);
    }

    private List<String> applyCase(List<String> fields)
    {
        return fields.stream().map(field -> this.strategy.apply(field)).collect(Collectors.toList());
    }

    private OptimizationFilter applyCase(OptimizationFilter optimizationFilter)
    {
        return optimizationFilter.withFieldName(this.applyCase(optimizationFilter.fieldName()));
    }

    private Map<String, Set<String>> applyCase(Map<String, Set<String>> map)
    {
        Map<String, Set<String>> caseAppliedMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : map.entrySet())
        {
            caseAppliedMap.put(applyCase(entry.getKey()), entry.getValue());
        }
        return caseAppliedMap;
    }

    private class MergeStrategyCaseConverter implements MergeStrategyVisitor<MergeStrategy>
    {
        @Override
        public MergeStrategy visitNoDeletesMergeStrategy(NoDeletesMergeStrategyAbstract noDeletesMergeStrategy)
        {
            return noDeletesMergeStrategy;
        }

        @Override
        public MergeStrategy visitDeleteIndicatorMergeStrategy(DeleteIndicatorMergeStrategyAbstract deleteIndicatorMergeStrategy)
        {
            return DeleteIndicatorMergeStrategy
                    .builder()
                    .deleteField(strategy.apply(deleteIndicatorMergeStrategy.deleteField()))
                    .addAllDeleteValues(deleteIndicatorMergeStrategy.deleteValues())
                    .build();
        }
    }

    private class DigestGenStrategyCaseConverter implements DigestGenStrategyVisitor<DigestGenStrategy>
    {

        @Override
        public DigestGenStrategy visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
        {
            return noDigestGenStrategy;
        }

        @Override
        public DigestGenStrategy visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
        {
            return UDFBasedDigestGenStrategy.builder()
                    .digestUdfName(udfBasedDigestGenStrategy.digestUdfName())
                    .digestField(applyCase(udfBasedDigestGenStrategy.digestField()))
                    .build();
        }
    }


    private class TransactionMilestoningCaseConverter implements TransactionMilestoningVisitor<TransactionMilestoning>
    {

        @Override
        public TransactionMilestoning visitBatchId(BatchIdAbstract batchId)
        {
            return BatchId
                    .builder()
                    .batchIdInName(applyCase(batchId.batchIdInName()))
                    .batchIdOutName(applyCase(batchId.batchIdOutName()))
                    .build();
        }

        @Override
        public TransactionMilestoning visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            return TransactionDateTime
                    .builder()
                    .dateTimeInName(applyCase(transactionDateTime.dateTimeInName()))
                    .dateTimeOutName(applyCase(transactionDateTime.dateTimeOutName()))
                    .build();
        }

        @Override
        public TransactionMilestoning visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            return BatchIdAndDateTime
                    .builder()
                    .batchIdInName(applyCase(batchIdAndDateTime.batchIdInName()))
                    .batchIdOutName(applyCase(batchIdAndDateTime.batchIdOutName()))
                    .dateTimeInName(applyCase(batchIdAndDateTime.dateTimeInName()))
                    .dateTimeOutName(applyCase(batchIdAndDateTime.dateTimeOutName()))
                    .build();
        }
    }

    private class AuditingCaseConverter implements AuditingVisitor<Auditing>
    {
        @Override
        public Auditing visitNoAuditing(NoAuditingAbstract noAuditing)
        {
            return noAuditing;
        }

        @Override
        public Auditing visitDateTimeAuditing(DateTimeAuditingAbstract dateTimeAuditing)
        {
            return DateTimeAuditing
                    .builder()
                    .dateTimeField(applyCase(dateTimeAuditing.dateTimeField()))
                    .build();
        }
    }

    private class ValidityMilestoningCaseConverter implements ValidityMilestoningVisitor<ValidityMilestoning>
    {
        @Override
        public ValidityMilestoning visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            return ValidDateTime
                    .builder()
                    .dateTimeFromName(applyCase(validDateTime.dateTimeFromName()))
                    .dateTimeThruName(applyCase(validDateTime.dateTimeThruName()))
                    .validityDerivation(validDateTime.validityDerivation().accept(new ValidityDerivationCaseConverter()))
                    .build();
        }
    }

    private class ValidityDerivationCaseConverter implements ValidityDerivationVisitor<ValidityDerivation>
    {

        @Override
        public ValidityDerivation visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
        {
            return SourceSpecifiesFromDateTime.of(applyCase(sourceSpecifiesFromDateTime.sourceDateTimeFromField()));
        }

        @Override
        public ValidityDerivation visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
        {
            return SourceSpecifiesFromAndThruDateTime
                    .builder()
                    .sourceDateTimeFromField(applyCase(sourceSpecifiesFromAndThruDateTime.sourceDateTimeFromField()))
                    .sourceDateTimeThruField(applyCase(sourceSpecifiesFromAndThruDateTime.sourceDateTimeThruField()))
                    .build();
        }
    }

    private class VersionStrategyCaseConverter implements VersioningStrategyVisitor<VersioningStrategy>
    {
        @Override
        public VersioningStrategy visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
        {
            return noVersioningStrategy;
        }

        @Override
        public VersioningStrategy visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
        {
            return MaxVersionStrategy
                    .builder()
                    .mergeDataVersionResolver(maxVersionStrategy.mergeDataVersionResolver())
                    .versioningField(strategy.apply(maxVersionStrategy.versioningField()))
                    .performStageVersioning(maxVersionStrategy.performStageVersioning())
                    .build();
        }

        @Override
        public VersioningStrategy visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
        {
            return AllVersionsStrategy
                    .builder()
                    .mergeDataVersionResolver(allVersionsStrategyAbstract.mergeDataVersionResolver())
                    .versioningField(strategy.apply(allVersionsStrategyAbstract.versioningField()))
                    .versioningOrder(allVersionsStrategyAbstract.versioningOrder())
                    .dataSplitFieldName(strategy.apply(allVersionsStrategyAbstract.dataSplitFieldName()))
                    .performStageVersioning(allVersionsStrategyAbstract.performStageVersioning())
                    .build();
        }
    }
}
