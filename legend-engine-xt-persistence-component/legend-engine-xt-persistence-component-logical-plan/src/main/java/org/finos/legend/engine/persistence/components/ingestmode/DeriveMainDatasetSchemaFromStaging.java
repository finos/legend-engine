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

import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeriveMainDatasetSchemaFromStaging implements IngestModeVisitor<Dataset>
{

    private DatasetDefinition.Builder mainDatasetDefinitionBuilder;
    private SchemaDefinition.Builder mainSchemaDefinitionBuilder;
    private List<Field> mainSchemaFields;

    public DeriveMainDatasetSchemaFromStaging(Dataset mainDataset, Dataset stagingDataset)
    {
        this.mainDatasetDefinitionBuilder = DatasetDefinition.builder()
                .name(mainDataset.datasetReference().name().get())
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().orElse(null));

        this.mainSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllIndexes(mainDataset.schema().indexes())
                .shardSpecification(mainDataset.schema().shardSpecification())
                .columnStoreSpecification(mainDataset.schema().columnStoreSpecification());

        this.mainSchemaFields = new ArrayList<>();
        this.mainSchemaFields.addAll(stagingDataset.schema().fields());
    }

    @Override
    public Dataset visitAppendOnly(AppendOnlyAbstract appendOnly)
    {
        if (appendOnly.digestField().isPresent())
        {
            addDigestField(mainSchemaFields, appendOnly.digestField().get());
        }
        removeDataSplitField(appendOnly.dataSplitField());
        boolean isAuditingFieldPK = appendOnly.deduplicationStrategy().accept(new DeriveAuditingFieldPKForAppendOnly(appendOnly.dataSplitField().isPresent()));
        appendOnly.auditing().accept(new EnrichSchemaWithAuditing(mainSchemaFields, isAuditingFieldPK));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
    {
        removeDataSplitField(nontemporalSnapshot.dataSplitField());
        boolean isAuditingFieldPK = doesDatasetContainsAnyPK(mainSchemaFields);
        nontemporalSnapshot.auditing().accept(new EnrichSchemaWithAuditing(mainSchemaFields, isAuditingFieldPK));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
    {
        addDigestField(mainSchemaFields, nontemporalDelta.digestField());
        removeDataSplitField(nontemporalDelta.dataSplitField());
        nontemporalDelta.mergeStrategy().accept(new EnrichSchemaWithMergeStrategy(mainSchemaFields));
        nontemporalDelta.auditing().accept(new EnrichSchemaWithAuditing(mainSchemaFields, true));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
    {
        addDigestField(mainSchemaFields, unitemporalSnapshot.digestField());
        unitemporalSnapshot.transactionMilestoning().accept(new EnrichSchemaWithTransactionMilestoning(mainSchemaFields));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
    {
        addDigestField(mainSchemaFields, unitemporalDelta.digestField());
        removeDataSplitField(unitemporalDelta.dataSplitField());
        unitemporalDelta.mergeStrategy().accept(new EnrichSchemaWithMergeStrategy(mainSchemaFields));
        unitemporalDelta.transactionMilestoning().accept(new EnrichSchemaWithTransactionMilestoning(mainSchemaFields));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
    {
        addDigestField(mainSchemaFields, bitemporalSnapshot.digestField());
        bitemporalSnapshot.transactionMilestoning().accept(new EnrichSchemaWithTransactionMilestoning(mainSchemaFields));
        bitemporalSnapshot.validityMilestoning().accept(new EnrichSchemaWithValidityMilestoning(mainSchemaFields));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    @Override
    public Dataset visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
    {
        addDigestField(mainSchemaFields, bitemporalDelta.digestField());
        removeDataSplitField(bitemporalDelta.dataSplitField());
        bitemporalDelta.mergeStrategy().accept(new EnrichSchemaWithMergeStrategy(mainSchemaFields));
        bitemporalDelta.transactionMilestoning().accept(new EnrichSchemaWithTransactionMilestoning(mainSchemaFields));
        bitemporalDelta.validityMilestoning().accept(new EnrichSchemaWithValidityMilestoning(mainSchemaFields));
        return mainDatasetDefinitionBuilder.schema(mainSchemaDefinitionBuilder.addAllFields(mainSchemaFields).build()).build();
    }

    private void removeDataSplitField(Optional<String> dataSplitField)
    {
        if (dataSplitField.isPresent())
        {
            mainSchemaFields.removeIf(field -> field.name().equals(dataSplitField.get()));
        }
    }

    public static void addDigestField(List<Field> schemaFields, String digestFieldName)
    {
        // DIGEST field addition
        if (!schemaFields.stream().anyMatch(field -> field.name().equals(digestFieldName)))
        {
            Field digest = Field.builder()
                    .name(digestFieldName)
                    .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                    .build();
            schemaFields.add(digest);
        }
    }

    private boolean doesDatasetContainsAnyPK(List<Field> mainSchemaFields)
    {
        return mainSchemaFields.stream().anyMatch(field -> field.primaryKey());
    }

    public static class DeriveAuditingFieldPKForAppendOnly implements DeduplicationStrategyVisitor<Boolean>
    {

        private boolean isDataSplitEnabled;

        public DeriveAuditingFieldPKForAppendOnly(boolean isDataSplitEnabled)
        {
            this.isDataSplitEnabled = isDataSplitEnabled;
        }

        @Override
        public Boolean visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
        {
            return isDataSplitEnabled;
        }

        @Override
        public Boolean visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
        {
            return true;
        }

        @Override
        public Boolean visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
        {
            return false;
        }
    }


    public static class EnrichSchemaWithMergeStrategy implements MergeStrategyVisitor<Void>
    {
        private List<Field> mainSchemaFields;

        public EnrichSchemaWithMergeStrategy(List<Field> mainSchemaFields)
        {
            this.mainSchemaFields = mainSchemaFields;
        }

        @Override
        public Void visitNoDeletesMergeStrategy(NoDeletesMergeStrategyAbstract noDeletesMergeStrategy)
        {
            return null;
        }

        @Override
        public Void visitDeleteIndicatorMergeStrategy(DeleteIndicatorMergeStrategyAbstract deleteIndicatorMergeStrategy)
        {
            mainSchemaFields.removeIf(field -> field.name().equals(deleteIndicatorMergeStrategy.deleteField()));
            return null;
        }
    }

    public static class EnrichSchemaWithAuditing implements AuditingVisitor<Void>
    {

        private List<Field> mainSchemaFields;
        private boolean isPrimary;

        public EnrichSchemaWithAuditing(List<Field> mainSchemaFields, boolean isPrimary)
        {
            this.mainSchemaFields = mainSchemaFields;
            this.isPrimary = isPrimary;
        }

        @Override
        public Void visitNoAuditing(NoAuditingAbstract noAuditing)
        {
            return null;
        }

        @Override
        public Void visitDateTimeAuditing(DateTimeAuditingAbstract dateTimeAuditing)
        {
            Field auditingField = getBatchTimeField(dateTimeAuditing.dateTimeField(), isPrimary);
            mainSchemaFields.add(auditingField);
            return null;
        }
    }

    public static class EnrichSchemaWithTransactionMilestoning implements TransactionMilestoningVisitor<Void>
    {
        private List<Field> mainSchemaFields;

        public EnrichSchemaWithTransactionMilestoning(List<Field> mainSchemaFields)
        {
            this.mainSchemaFields = mainSchemaFields;
        }

        @Override
        public Void visitBatchId(BatchIdAbstract batchId)
        {
            Field batchIdIn = getBatchIdField(batchId.batchIdInName(), true);
            Field batchIdOut = getBatchIdField(batchId.batchIdOutName(), false);
            mainSchemaFields.add(batchIdIn);
            mainSchemaFields.add(batchIdOut);
            return null;
        }

        @Override
        public Void visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            Field dateTimeIn = getBatchTimeField(transactionDateTime.dateTimeInName(), true);
            Field dateTimeOut = getBatchTimeField(transactionDateTime.dateTimeOutName(), false);
            mainSchemaFields.add(dateTimeIn);
            mainSchemaFields.add(dateTimeOut);
            return null;
        }

        @Override
        public Void visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            Field batchIdIn = getBatchIdField(batchIdAndDateTime.batchIdInName(), true);
            Field batchIdOut = getBatchIdField(batchIdAndDateTime.batchIdOutName(), false);
            Field dateTimeIn = getBatchTimeField(batchIdAndDateTime.dateTimeInName(), false);
            Field dateTimeOut = getBatchTimeField(batchIdAndDateTime.dateTimeOutName(), false);
            mainSchemaFields.add(batchIdIn);
            mainSchemaFields.add(batchIdOut);
            mainSchemaFields.add(dateTimeIn);
            mainSchemaFields.add(dateTimeOut);
            return null;
        }
    }

    public static class EnrichSchemaWithValidityMilestoning implements ValidityMilestoningVisitor<Void>
    {
        private List<Field> mainSchemaFields;

        public EnrichSchemaWithValidityMilestoning(List<Field> mainSchemaFields)
        {
            this.mainSchemaFields = mainSchemaFields;
        }

        @Override
        public Void visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            Field dateTimeFrom = getBatchTimeField(validDateTime.dateTimeFromName(), true);
            Field dateTimeThru = getBatchTimeField(validDateTime.dateTimeThruName(), false);
            mainSchemaFields.add(dateTimeFrom);
            mainSchemaFields.add(dateTimeThru);
            validDateTime.validityDerivation().accept(new EnrichSchemaWithValidityMilestoningDerivation(mainSchemaFields));
            return null;
        }
    }

    public static class EnrichSchemaWithValidityMilestoningDerivation implements ValidityDerivationVisitor<Void>
    {
        private List<Field> mainSchemaFields;

        public EnrichSchemaWithValidityMilestoningDerivation(List<Field> mainSchemaFields)
        {
            this.mainSchemaFields = mainSchemaFields;
        }

        @Override
        public Void visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
        {
            mainSchemaFields.removeIf(field -> field.name().equals(sourceSpecifiesFromDateTime.sourceDateTimeFromField()));
            return null;
        }

        @Override
        public Void visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
        {
            mainSchemaFields.removeIf(field -> field.name().equals(sourceSpecifiesFromAndThruDateTime.sourceDateTimeFromField()));
            mainSchemaFields.removeIf(field -> field.name().equals(sourceSpecifiesFromAndThruDateTime.sourceDateTimeThruField()));
            return null;
        }
    }

    private static Field getBatchTimeField(String batchTimeName, boolean isPrimary)
    {
        return Field.builder()
                .name(batchTimeName)
                .type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty()))
                .primaryKey(isPrimary)
                .build();
    }

    private static Field getBatchIdField(String batchIdName, boolean isPrimary)
    {
        return Field.builder()
                .name(batchIdName)
                .type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty()))
                .primaryKey(isPrimary)
                .build();
    }

}