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

package org.finos.legend.engine.testable.persistence.mapper.v2;

import org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing;
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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.ActionIndicatorFieldsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.DeleteIndicator;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.DeleteIndicatorForGraphFetch;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.DeleteIndicatorForTds;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.NoActionIndicator;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.ProcessingDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedDimensionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceDerivedTime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeFieldsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStart;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.sourcederived.SourceTimeStartAndEnd;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.path.Path;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.path.PathElement;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.path.PropertyPathElement;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.testable.persistence.mapper.v2.DatasetMapper.isFieldNamePresent;

public class MappingVisitors
{
    public static final String BATCH_ID_IN_FIELD_DEFAULT = "BATCH_IN";
    public static final String BATCH_ID_OUT_FIELD_DEFAULT = "BATCH_OUT";
    public static final String BATCH_TIME_IN_FIELD_DEFAULT = "BATCH_TIME_IN";
    public static final String BATCH_TIME_OUT_FIELD_DEFAULT = "BATCH_TIME_OUT";

    public static final org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingVisitor<Auditing> MAP_TO_COMPONENT_NONTEMPORAL_AUDITING = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.AuditingVisitor<Auditing>()
    {
        @Override
        public Auditing visitAuditingDateTime(AuditingDateTime val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing.builder().dateTimeField(val.auditingDateTimeName).build();
        }

        @Override
        public Auditing visitNoAuditing(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.NoAuditing val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing.builder().build();
        }
    };

    public static final ActionIndicatorFieldsVisitor<MergeStrategy> MAP_TO_COMPONENT_DELETE_STRATEGY = new ActionIndicatorFieldsVisitor<MergeStrategy>()
    {

        @Override
        public MergeStrategy visitNoActionIndicator(NoActionIndicator val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategy.builder().build();
        }

        @Override
        public MergeStrategy visitDeleteIndicator(DeleteIndicator val)
        {
            if (val instanceof DeleteIndicatorForTds)
            {
                return DeleteIndicatorMergeStrategy.builder()
                        .deleteField(((DeleteIndicatorForTds) val).deleteField)
                        .addAllDeleteValues(val.deleteValues)
                        .build();
            }
            String property = getPropertyPathElement(((DeleteIndicatorForGraphFetch)val).deleteFieldPath);
            return DeleteIndicatorMergeStrategy.builder()
                    .deleteField(property)
                    .addAllDeleteValues(val.deleteValues)
                    .build();
        }
    };

    public static final ProcessingDimensionVisitor<TransactionMilestoning> MAP_TO_COMPONENT_PROCESSING_DIMENSION = new ProcessingDimensionVisitor<TransactionMilestoning>()
    {

        @Override
        public TransactionMilestoning visitBatchId(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId val)
        {
            return BatchId.of(
                    val.batchIdIn == null || val.batchIdIn.isEmpty() ? BATCH_ID_IN_FIELD_DEFAULT : val.batchIdIn,
                    val.batchIdOut == null || val.batchIdOut.isEmpty() ? BATCH_ID_OUT_FIELD_DEFAULT : val.batchIdOut);
        }

        @Override
        public TransactionMilestoning visitDateTime(ProcessingDateTime val)
        {
            return TransactionDateTime.of(
                    val.timeIn == null || val.timeIn.isEmpty() ? BATCH_TIME_IN_FIELD_DEFAULT : val.timeIn,
                    val.timeOut == null || val.timeOut.isEmpty() ? BATCH_TIME_OUT_FIELD_DEFAULT : val.timeOut);
        }

        @Override
        public TransactionMilestoning visitBatchIdAndDateTime(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime val)
        {
            return BatchIdAndDateTime.of(
                    val.batchIdIn,
                    val.batchIdOut,
                    val.timeIn,
                    val.timeOut);
        }
    };

    public static final SourceDerivedDimensionVisitor<ValidityMilestoning> MAP_TO_COMPONENT_SOURCE_DERIVED_DIMENSION = new SourceDerivedDimensionVisitor<ValidityMilestoning>()
    {
        @Override
        public ValidityMilestoning visitSourceDerivedTime(SourceDerivedTime val)
        {
            return ValidDateTime.builder()
                    .dateTimeFromName(val.timeStart)
                    .dateTimeThruName(val.timeEnd)
                    .validityDerivation(val.sourceTimeFields.accept(MAP_TO_COMPONENT_SOURCE_TIME_FIELDS))
                    .build();
        }
    };

    public static final SourceTimeFieldsVisitor<ValidityDerivation> MAP_TO_COMPONENT_SOURCE_TIME_FIELDS = new SourceTimeFieldsVisitor<ValidityDerivation>()
    {
        @Override
        public ValidityDerivation visit(SourceTimeStart val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(val.startField)
                    .build();
        }

        @Override
        public ValidityDerivation visit(SourceTimeStartAndEnd val)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(val.startField)
                    .sourceDateTimeThruField(val.endField)
                    .build();
        }
    };

    public static class DeriveStagingSchemaWithActionIndicatorStrategy implements ActionIndicatorFieldsVisitor<Void>
    {
        private Set<Field> fieldsToAdd;
        private SchemaDefinition baseSchema;

        public DeriveStagingSchemaWithActionIndicatorStrategy(SchemaDefinition baseSchema, Set<Field> fieldsToAdd)
        {
            this.fieldsToAdd = fieldsToAdd;
            this.baseSchema = baseSchema;
        }

        @Override
        public Void visitNoActionIndicator(NoActionIndicator val)
        {
            return null;
        }

        @Override
        public Void visitDeleteIndicator(DeleteIndicator val)
        {
                if (val instanceof DeleteIndicatorForTds && !isFieldNamePresent(baseSchema, ((DeleteIndicatorForTds) val).deleteField))
                {
                    Field deleted = Field.builder()
                            .name(((DeleteIndicatorForTds) val).deleteField)
                            .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                            .build();
                    fieldsToAdd.add(deleted);
                }
                else if (val instanceof DeleteIndicatorForGraphFetch)
                {
                    String property = getPropertyPathElement(((DeleteIndicatorForGraphFetch)val).deleteFieldPath);
                    Field deleted = Field.builder()
                            .name(property)
                            .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                            .build();
                    fieldsToAdd.add(deleted);
                }
            return null;
        }
    }

    public static String getPropertyPathElement(Path fieldPath)
    {
        List<PathElement> pathElements = fieldPath.path;
        return ((PropertyPathElement)pathElements.get(pathElements.size() - 1)).property;
    }

    public static class DeriveStagingSchemaWithProcessingDimension implements ProcessingDimensionVisitor<Void>
    {
        private Set<String> fieldsToIgnore;

        public DeriveStagingSchemaWithProcessingDimension(Set<String> fieldsToIgnore)
        {
            this.fieldsToIgnore = fieldsToIgnore;
        }

        @Override
        public Void visitBatchId(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchId val)
        {
            fieldsToIgnore.add(val.batchIdIn);
            fieldsToIgnore.add(val.batchIdOut);
            return null;
        }

        @Override
        public Void visitDateTime(ProcessingDateTime val)
        {
            fieldsToIgnore.add(val.timeIn);
            fieldsToIgnore.add(val.timeOut);
            return null;
        }

        @Override
        public Void visitBatchIdAndDateTime(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.processing.BatchIdAndDateTime val)
        {
            fieldsToIgnore.add(val.batchIdIn);
            fieldsToIgnore.add(val.batchIdOut);
            fieldsToIgnore.add(val.timeIn);
            fieldsToIgnore.add(val.timeOut);
            return null;
        }
    }

    public static class DeriveStagingSchemaWithSourceDimension implements SourceDerivedDimensionVisitor<Void>
    {
        private Set<String> fieldsToIgnore;
        private Set<Field> fieldsToAdd;
        private SchemaDefinition baseSchema;

        public DeriveStagingSchemaWithSourceDimension(Set<String> fieldsToIgnore, Set<Field> fieldsToAdd, SchemaDefinition baseSchema)
        {
            this.fieldsToIgnore = fieldsToIgnore;
            this.fieldsToAdd = fieldsToAdd;
            this.baseSchema = baseSchema;
        }

        @Override
        public Void visitSourceDerivedTime(SourceDerivedTime val)
        {
            fieldsToIgnore.add(val.timeStart);
            fieldsToIgnore.add(val.timeEnd);
            val.sourceTimeFields.accept(new DeriveStagingSchemaWithSourceTimeFields(fieldsToAdd, baseSchema));
            return null;
        }
    }

    public static class DeriveStagingSchemaWithSourceTimeFields implements SourceTimeFieldsVisitor<Void>
    {
        private Set<Field> fieldsToAdd;
        private SchemaDefinition baseSchema;

        public DeriveStagingSchemaWithSourceTimeFields(Set<Field> fieldsToAdd, SchemaDefinition baseSchema)
        {
            this.fieldsToAdd = fieldsToAdd;
            this.baseSchema = baseSchema;
        }

        @Override
        public Void visit(SourceTimeStart val)
        {
            if (!isFieldNamePresent(baseSchema, val.startField))
            {
                Field startField = Field.builder()
                        .name(val.startField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                fieldsToAdd.add(startField);
            }
            return null;
        }

        @Override
        public Void visit(SourceTimeStartAndEnd val)
        {
            if (!isFieldNamePresent(baseSchema, val.startField))
            {
                Field startField = Field.builder()
                        .name(val.startField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                fieldsToAdd.add(startField);
            }
            if (!isFieldNamePresent(baseSchema, val.endField))
            {
                Field endField = Field.builder()
                        .name(val.endField)
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                fieldsToAdd.add(endField);
            }
            return null;
        }
    }
}
