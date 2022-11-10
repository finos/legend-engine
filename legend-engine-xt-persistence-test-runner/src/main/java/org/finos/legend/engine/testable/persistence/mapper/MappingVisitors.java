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

import java.util.Optional;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.AuditingVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.DateTimeAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.auditing.NoAuditing;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.MergeStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.NoDeletesMergeStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdAndDateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.BatchIdTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.DateTimeTransactionMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.DateTimeValidityMilestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor;
import static org.finos.legend.engine.testable.persistence.mapper.DatasetMapper.isFieldNamePresent;

public class MappingVisitors
{
    public static final String BATCH_ID_IN_FIELD_DEFAULT = "BATCH_IN";
    public static final String BATCH_ID_OUT_FIELD_DEFAULT = "BATCH_OUT";
    public static final String BATCH_TIME_IN_FIELD_DEFAULT = "BATCH_TIME_IN";
    public static final String BATCH_TIME_OUT_FIELD_DEFAULT = "BATCH_TIME_OUT";

    public static final AuditingVisitor<org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing> MAP_TO_COMPONENT_AUDITING = new AuditingVisitor<org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing>()
    {
        @Override
        public org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing visit(NoAuditing val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing.builder().build();
        }

        @Override
        public org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing visit(DateTimeAuditing val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing.builder().dateTimeField(val.dateTimeName).build();
        }
    };

    public static final MergeStrategyVisitor<MergeStrategy> MAP_TO_COMPONENT_MERGE_STRATEGY = new MergeStrategyVisitor<MergeStrategy>()
    {

        @Override
        public MergeStrategy visit(NoDeletesMergeStrategy val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategy.builder().build();
        }

        @Override
        public MergeStrategy visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy val)
        {
            return DeleteIndicatorMergeStrategy.builder()
                    .deleteField(val.deleteField)
                    .addAllDeleteValues(val.deleteValues)
                    .build();
        }
    };

    public static final TransactionMilestoningVisitor<TransactionMilestoning> MAP_TO_COMPONENT_TRANSACTION_MILESTONING = new TransactionMilestoningVisitor<TransactionMilestoning>()
    {

        @Override
        public TransactionMilestoning visit(BatchIdTransactionMilestoning val)
        {
            return BatchId.of(
                    val.batchIdInName == null || val.batchIdInName.isEmpty() ? BATCH_ID_IN_FIELD_DEFAULT : val.batchIdInName,
                    val.batchIdOutName == null || val.batchIdOutName.isEmpty() ? BATCH_ID_OUT_FIELD_DEFAULT : val.batchIdOutName);
        }

        @Override
        public TransactionMilestoning visit(DateTimeTransactionMilestoning val)
        {
            return TransactionDateTime.of(
                    val.dateTimeInName == null || val.dateTimeInName.isEmpty() ? BATCH_TIME_IN_FIELD_DEFAULT : val.dateTimeInName,
                    val.dateTimeOutName == null || val.dateTimeOutName.isEmpty() ? BATCH_TIME_OUT_FIELD_DEFAULT : val.dateTimeOutName);
        }

        @Override
        public TransactionMilestoning visit(BatchIdAndDateTimeTransactionMilestoning val)
        {
            return BatchIdAndDateTime.of(
                    val.batchIdInName == null || val.batchIdInName.isEmpty() ? BATCH_ID_IN_FIELD_DEFAULT : val.batchIdInName,
                    val.batchIdOutName == null || val.batchIdOutName.isEmpty() ? BATCH_ID_OUT_FIELD_DEFAULT : val.batchIdOutName,
                    val.dateTimeInName == null || val.dateTimeInName.isEmpty() ? BATCH_TIME_IN_FIELD_DEFAULT : val.dateTimeInName,
                    val.dateTimeOutName == null || val.dateTimeOutName.isEmpty() ? BATCH_TIME_OUT_FIELD_DEFAULT : val.dateTimeOutName);
        }
    };

    public static final ValidityMilestoningVisitor<ValidityMilestoning> MAP_TO_COMPONENT_VALIDITY_MILESTONING = new ValidityMilestoningVisitor<ValidityMilestoning>()
    {
        @Override
        public ValidityMilestoning visit(DateTimeValidityMilestoning val)
        {
            return ValidDateTime.builder()
                    .dateTimeFromName(val.dateTimeFromName)
                    .dateTimeThruName(val.dateTimeThruName)
                    .validityDerivation(val.derivation.accept(MAP_TO_COMPONENT_VALIDITY_DERIVATION))
                    .build();
        }
    };

    public static final org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor<ValidityDerivation> MAP_TO_COMPONENT_VALIDITY_DERIVATION = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.validitymilestoning.derivation.ValidityDerivationVisitor<ValidityDerivation>()
    {
        @Override
        public ValidityDerivation visit(SourceSpecifiesFromDateTime val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(val.sourceDateTimeFromField)
                    .build();
        }

        @Override
        public ValidityDerivation visit(SourceSpecifiesFromAndThruDateTime val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(val.sourceDateTimeFromField)
                    .sourceDateTimeThruField(val.sourceDateTimeThruField)
                    .build();
        }
    };

    public static class EnrichSchemaWithAuditing implements AuditingVisitor
    {
        private SchemaDefinition.Builder schemaDefinitionBuilder;
        private Dataset mainDataset;
        private SchemaDefinition baseSchema;

        public EnrichSchemaWithAuditing(SchemaDefinition.Builder schemaDefinitionBuilder, Dataset mainDataset)
        {
            this.schemaDefinitionBuilder = schemaDefinitionBuilder;
            this.baseSchema = mainDataset.schema();
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visit(NoAuditing auditing)
        {
            return null;
        }

        @Override
        public Void visit(DateTimeAuditing auditing)
        {
            // if DateTimeAuditing -> user provided BATCH_TIME_IN field addition
            if (!isFieldNamePresent(baseSchema, auditing.dateTimeName))
            {
                Field auditDateTime = Field.builder()
                        .name(auditing.dateTimeName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .build();
                schemaDefinitionBuilder.addFields(auditDateTime);
            }
            return null;
        }
    }

    public static class EnrichSchemaWithMergyStrategy implements MergeStrategyVisitor
    {
        private SchemaDefinition.Builder schemaDefinitionBuilder;
        private Dataset mainDataset;
        private SchemaDefinition baseSchema;

        public EnrichSchemaWithMergyStrategy(SchemaDefinition.Builder schemaDefinitionBuilder, Dataset mainDataset)
        {
            this.schemaDefinitionBuilder = schemaDefinitionBuilder;
            this.baseSchema = mainDataset.schema();
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visit(NoDeletesMergeStrategy mergeStrategy)
        {
            return null;
        }

        @Override
        public Void visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.merge.DeleteIndicatorMergeStrategy mergeStrategy)
        {
            // if DeleteIndicatorMergeStrategy -> user provided DELETED field addition
            if (!isFieldNamePresent(baseSchema, mergeStrategy.deleteField))
            {
                Field deleted = Field.builder()
                        .name(mergeStrategy.deleteField)
                        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                        .build();
                schemaDefinitionBuilder.addFields(deleted);
            }
            return null;
        }
    }

    public static class EnrichSchemaWithTransactionMilestoning implements TransactionMilestoningVisitor
    {
        private SchemaDefinition.Builder schemaDefinitionBuilder;
        private Dataset mainDataset;
        private SchemaDefinition baseSchema;

        public EnrichSchemaWithTransactionMilestoning(SchemaDefinition.Builder schemaDefinitionBuilder, Dataset mainDataset)
        {
            this.schemaDefinitionBuilder = schemaDefinitionBuilder;
            this.baseSchema = mainDataset.schema();
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visit(BatchIdTransactionMilestoning transactionMilestoning)
        {
            // if BatchId based transactionMilestoning -> user provided BATCH_IN BATCH_OUT fields addition
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.batchIdInName))
            {
                Field batchIdIn = Field.builder()
                        .name(transactionMilestoning.batchIdInName)
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdIn);
            }
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.batchIdOutName))
            {
                Field batchIdOut = Field.builder()
                        .name(transactionMilestoning.batchIdOutName)
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdOut);
            }
            return null;
        }

        @Override
        public Void visit(DateTimeTransactionMilestoning transactionMilestoning)
        {
            // if TransactionDateTime based transactionMilestoning -> user provided IN_Z OUT_Z fields addition
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.dateTimeInName))
            {
                Field dateTimeIn = Field.builder()
                        .name(transactionMilestoning.dateTimeInName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeIn);
            }
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.dateTimeOutName))
            {
                Field dateTimeOut = Field.builder()
                        .name(transactionMilestoning.dateTimeOutName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeOut);
            }
            return null;
        }

        @Override
        public Void visit(BatchIdAndDateTimeTransactionMilestoning transactionMilestoning)
        {
            // if TransactionDateTime based transactionMilestoning -> user provided IN_Z OUT_Z fields addition
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.batchIdInName))
            {
                Field batchIdIn = Field.builder()
                        .name(transactionMilestoning.batchIdInName)
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdIn);
            }
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.batchIdOutName))
            {
                Field batchIdOut = Field.builder()
                        .name(transactionMilestoning.batchIdOutName)
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdOut);
            }
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.dateTimeInName))
            {
                Field dateTimeIn = Field.builder()
                        .name(transactionMilestoning.dateTimeInName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeIn);
            }
            if (!isFieldNamePresent(baseSchema, transactionMilestoning.dateTimeOutName))
            {
                Field dateTimeOut = Field.builder()
                        .name(transactionMilestoning.dateTimeOutName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeOut);
            }
            return null;
        }
    }

    public static class EnrichSchemaWithValidityMilestoning implements ValidityMilestoningVisitor
    {
        private SchemaDefinition.Builder mainSchemaDefinitionBuilder;
        private SchemaDefinition.Builder stagingSchemaDefinitionBuilder;
        private Dataset mainDataset;
        private SchemaDefinition baseSchema;

        public EnrichSchemaWithValidityMilestoning(SchemaDefinition.Builder mainSchemaDefinitionBuilder,
                                                   SchemaDefinition.Builder stagingSchemaDefinitionBuilder,
                                                   Dataset mainDataset)
        {
            this.mainSchemaDefinitionBuilder = mainSchemaDefinitionBuilder;
            this.stagingSchemaDefinitionBuilder = stagingSchemaDefinitionBuilder;
            this.baseSchema = mainDataset.schema();
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visit(DateTimeValidityMilestoning validDateTime)
        {
            // if ValidDateTime based validityMilestoning -> user provided FROM_Z THRU_Z fields addition
            if (!isFieldNamePresent(baseSchema, validDateTime.dateTimeFromName))
            {
                Field dateTimeFrom = Field.builder()
                        .name(validDateTime.dateTimeFromName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                mainSchemaDefinitionBuilder.addFields(dateTimeFrom);
            }
            if (!isFieldNamePresent(baseSchema, validDateTime.dateTimeThruName))
            {
                Field dateTimeThru = Field.builder()
                        .name(validDateTime.dateTimeThruName)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                mainSchemaDefinitionBuilder.addFields(dateTimeThru);
            }

            validDateTime.derivation.accept(new EnrichSchemaWithValidityMilestoningDerivation(mainSchemaDefinitionBuilder, mainDataset));
            validDateTime.derivation.accept(new EnrichSchemaWithValidityMilestoningDerivation(stagingSchemaDefinitionBuilder, mainDataset));
            return null;
        }
    }

    public static class EnrichSchemaWithValidityMilestoningDerivation implements ValidityDerivationVisitor
    {
        private SchemaDefinition.Builder schemaDefinitionBuilder;
        private Dataset mainDataset;
        private SchemaDefinition baseSchema;

        public EnrichSchemaWithValidityMilestoningDerivation(SchemaDefinition.Builder schemaDefinitionBuilder, Dataset mainDataset)
        {
            this.schemaDefinitionBuilder = schemaDefinitionBuilder;
            this.baseSchema = mainDataset.schema();
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visit(SourceSpecifiesFromDateTime validityMilestoningDerivation)
        {
            // if SourceSpecifiesFromDateTime based validityMilestoningDerivation -> user provided SOURCE_FROM field addition
            if (!isFieldNamePresent(baseSchema, validityMilestoningDerivation.sourceDateTimeFromField))
            {
                Field sourceDateTimeFrom = Field.builder()
                        .name(validityMilestoningDerivation.sourceDateTimeFromField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeFrom);
            }
            return null;
        }

        @Override
        public Void visit(SourceSpecifiesFromAndThruDateTime validityMilestoningDerivation)
        {
            // if SourceSpecifiesFromDateTime based validityMilestoningDerivation -> user provided SOURCE_FROM SOURCE_THRU fields addition
            if (!isFieldNamePresent(baseSchema, validityMilestoningDerivation.sourceDateTimeFromField))
            {
                Field sourceDateTimeFrom = Field.builder()
                        .name(validityMilestoningDerivation.sourceDateTimeFromField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeFrom);
            }
            if (!isFieldNamePresent(baseSchema, validityMilestoningDerivation.sourceDateTimeThruField))
            {
                Field sourceDateTimeThru = Field.builder()
                        .name(validityMilestoningDerivation.sourceDateTimeThruField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeThru);
            }
            return null;
        }
    }
}
