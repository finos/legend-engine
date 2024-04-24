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

package org.finos.legend.engine.persistence.components.schemaevolution;

import org.finos.legend.engine.persistence.components.ingestmode.AppendOnlyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoadAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivationVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Parameter;
import static org.immutables.value.Value.Style;

public class SchemaEvolution
{

    @Immutable
    @Style(
            typeAbstract = "*Abstract",
            typeImmutable = "*",
            jdkOnly = true,
            optionalAcceptNullable = true,
            strictBuilder = true
    )
    public interface SchemaEvolutionResultAbstract
    {
        @Parameter(order = 0)
        LogicalPlan logicalPlan();

        @Parameter(order = 1)
        Dataset evolvedDataset();
    }

    private final Sink sink;
    private final IngestMode ingestMode;
    private final Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet;

    /*
        1. Validate that nothing has changed with the primary keys of the main table.
        2. Check if Schema of main table and staging table is different and accordingly modify schema.
        3. Check executor capabilities to check if operation is permitted, else throw an exception.
        4. Generate the logical operation and modify the schema of the main dataset object.
     */

    public SchemaEvolution(Sink sink, IngestMode ingestMode, Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet)
    {
        this.sink = sink;
        this.ingestMode = ingestMode;
        this.schemaEvolutionCapabilitySet = schemaEvolutionCapabilitySet;
    }

    public SchemaEvolutionResult buildLogicalPlanForSchemaEvolution(Dataset mainDataset, Dataset stagingDataset)
    {
        List<Operation> operations = new ArrayList<>();
        Set<Field> modifiedFields = new HashSet<>();
        validatePrimaryKeys(mainDataset, stagingDataset);
        operations.addAll(stagingToMainTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(STAGING_TABLE_FIELDS_TO_IGNORE), modifiedFields));
        operations.addAll(mainToStagingTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(MAIN_TABLE_FIELDS_TO_IGNORE), modifiedFields));

        SchemaDefinition evolvedSchema = evolveSchemaDefinition(mainDataset.schema(), modifiedFields);

        return SchemaEvolutionResult.of(LogicalPlan.of(operations), mainDataset.withSchema(evolvedSchema));
    }

    private void validatePrimaryKeys(Dataset mainDataset, Dataset stagingDataset)
    {
        List<Field> stagingFilteredFields = stagingDataset.schema().fields().stream().filter(field -> !(ingestMode.accept(STAGING_TABLE_FIELDS_TO_IGNORE).contains(field.name()))).collect(Collectors.toList());
        Set<String> stagingPkNames = stagingFilteredFields.stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        List<Field> mainFilteredFields = mainDataset.schema().fields().stream().filter(field -> !(ingestMode.accept(MAIN_TABLE_FIELDS_TO_IGNORE).contains(field.name()))).collect(Collectors.toList());
        Set<String> mainPkNames = mainFilteredFields.stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        if (!Objects.equals(stagingPkNames, mainPkNames))
        {
            throw new IncompatibleSchemaChangeException("Primary keys for main table has changed which is not allowed");
        }
    }

    //Validate all columns (allowing exceptions) in staging dataset must have a matching column in main dataset
    private List<Operation> stagingToMainTableColumnMatch(Dataset mainDataset,
            Dataset stagingDataset,
            Set<String> fieldsToIgnore,
            Set<Field> modifiedFields)
    {
        List<Operation> operations = new ArrayList<>();
        List<Field> mainFields = mainDataset.schema().fields();
        List<Field> stagingFields = stagingDataset.schema().fields();
        List<Field> filteredFields = stagingFields.stream().filter(field -> !fieldsToIgnore.contains(field.name())).collect(Collectors.toList());
        for (Field stagingField : filteredFields)
        {
            String stagingFieldName = stagingField.name();
            Field matchedMainField = mainFields.stream().filter(mainField -> mainField.name().equals(stagingFieldName)).findFirst().orElse(null);
            if (matchedMainField == null)
            {
                // Add the new column in the main table if database supports ADD_COLUMN capability and
                // if user capability supports ADD_COLUMN or is empty (since empty means no overriden preference)
                if (sink.capabilities().contains(Capability.ADD_COLUMN)
                        && (schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.ADD_COLUMN)))
                {
                    operations.add(Alter.of(mainDataset, Alter.AlterOperation.ADD, stagingField, Optional.empty()));
                    modifiedFields.add(stagingField);
                }
                else
                {
                    throw new IncompatibleSchemaChangeException(String.format("Field \"%s\" in staging dataset does not exist in main dataset. Couldn't add column since sink/user capabilities do not permit operation.", stagingFieldName));
                }
            }
            else
            {
                FieldType stagingFieldType = stagingField.type();
                if (!matchedMainField.type().equals(stagingFieldType))
                {
                    if (!matchedMainField.type().dataType().equals(stagingFieldType.dataType()))
                    {
                        // If the datatype is an implicit change, we let the database handle the change.
                        // We only alter the length if required (pick the maximum length)
                        if (sink.capabilities().contains(Capability.IMPLICIT_DATA_TYPE_CONVERSION)
                                && sink.supportsImplicitMapping(matchedMainField.type().dataType(), stagingFieldType.dataType()))
                        {
                            Field newField = sink.evolveFieldLength(stagingField, matchedMainField);
                            evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                        }
                        // If the datatype is a non-breaking change, we alter the datatype.
                        // We also alter the length if required (pick the maximum length)
                        else if (sink.capabilities().contains(Capability.EXPLICIT_DATA_TYPE_CONVERSION)
                                && sink.supportsExplicitMapping(matchedMainField.type().dataType(), stagingFieldType.dataType()))
                        {
                            if (schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_CONVERSION))
                            {
                                //Modify the column in main table
                                Field newField = sink.evolveFieldLength(matchedMainField, stagingField);
                                evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                            }
                            else
                            {
                                throw new IncompatibleSchemaChangeException(String.format("Explicit data type conversion from \"%s\" to \"%s\" couldn't be performed since user capability does not allow it", matchedMainField.type().dataType(), stagingFieldType.dataType()));
                            }
                        }

                        //Else, it is a breaking change. We throw an exception
                        else
                        {
                            throw new IncompatibleSchemaChangeException(String.format("Breaking schema change from datatype \"%s\" to \"%s\"", matchedMainField.type().dataType(), stagingFieldType.dataType()));
                        }
                    }
                    //If data types are same, we check if length requires any evolution
                    else
                    {
                        Field newField = sink.evolveFieldLength(stagingField, matchedMainField);
                        evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                    }
                }
                //If Field types are same, we check to see if nullability needs any evolution
                else
                {
                    if (!matchedMainField.nullable() && stagingField.nullable())
                    {
                        Field newField = sink.createNewField(matchedMainField, stagingField, matchedMainField.type().length(), matchedMainField.type().scale());
                        evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                    }
                }
            }
        }
        return operations;
    }

    //Create alter statements if newField is different from mainDataField
    private void evolveDataType(Field newField, Field mainDataField, Dataset mainDataset, List<Operation> operations, Set<Field> modifiedFields)
    {
        if (!mainDataField.equals(newField))
        {
            // If there are any data type length changes, make sure user capability allows it before creating the alter statement
            if (!Objects.equals(mainDataField.type().length(), newField.type().length()))
            {
                if (!sink.capabilities().contains(Capability.DATA_TYPE_LENGTH_CHANGE)
                        || (!schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE)))
                {
                    throw new IncompatibleSchemaChangeException(String.format("Data type length changes couldn't be performed on column \"%s\" since user capability does not allow it", newField.name()));
                }
            }
            // If there are any data type scale changes, make sure user capability allows it before creating the alter statement
            if (!Objects.equals(mainDataField.type().scale(), newField.type().scale()))
            {
                if (!sink.capabilities().contains(Capability.DATA_TYPE_SCALE_CHANGE)
                        || (!schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE)))
                {
                    throw new IncompatibleSchemaChangeException(String.format("Data type scale changes couldn't be performed on column \"%s\" since user capability does not allow it", newField.name()));
                }
            }
            // Create the alter statement for changing the data type and sizing as required
            if (!mainDataField.type().equals(newField.type()))
            {
                operations.add(Alter.of(mainDataset, Alter.AlterOperation.CHANGE_DATATYPE, newField, Optional.empty()));
                modifiedFields.add(newField);
            }
            // Create the alter statement for changing the column nullability
            if (mainDataField.nullable() != newField.nullable())
            {
                alterColumnWithNullable(newField, mainDataset, operations, modifiedFields);
            }
        }
    }

    private void alterColumnWithNullable(Field newField, Dataset mainDataset, List<Operation> operations, Set<Field> modifiedFields)
    {
        // We do not allow changing nullability for PKs
        if (!newField.primaryKey())
        {
            if (schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE))
            {
                operations.add(Alter.of(mainDataset, Alter.AlterOperation.NULLABLE_COLUMN, newField, Optional.empty()));
                modifiedFields.add(newField);
            }
            else
            {
                throw new IncompatibleSchemaChangeException(String.format("Column \"%s\" couldn't be made nullable since user capability does not allow it", newField.name()));
            }
        }
    }

    private List<Operation> mainToStagingTableColumnMatch(Dataset mainDataset, Dataset stagingDataset, Set<String> fieldsToIgnore, Set<Field> modifiedFields)
    {
        List<Operation> operations = new ArrayList<>();
        List<Field> mainFields = mainDataset.schema().fields();
        Set<String> stagingFieldNames = stagingDataset.schema().fields().stream().map(Field::name).collect(Collectors.toSet());
        for (Field mainField : mainFields.stream().filter(field -> !fieldsToIgnore.contains(field.name())).collect(Collectors.toList()))
        {
            String mainFieldName = mainField.name();
            if (!stagingFieldNames.contains(mainFieldName))
            {
                //Modify the column to nullable in main table
                if (!mainField.nullable())
                {
                    mainField = mainField.withNullable(true);
                    alterColumnWithNullable(mainField, mainDataset, operations, modifiedFields);
                }
            }
        }
        return operations;
    }

    private SchemaDefinition evolveSchemaDefinition(SchemaDefinition schema, Set<Field> modifiedFields)
    {
        final Set<String> modifiedFieldNames = modifiedFields.stream().map(Field::name).collect(Collectors.toSet());

        List<Field> evolvedFields = schema.fields()
                .stream()
                .filter(f -> !modifiedFieldNames.contains(f.name()))
                .collect(Collectors.toList());

        evolvedFields.addAll(modifiedFields);

        return schema.withFields(evolvedFields);
    }

    // ingest mode visitors

    private static final IngestModeVisitor<Set<String>> STAGING_TABLE_FIELDS_TO_IGNORE = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(appendOnly);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(nontemporalSnapshot);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(nontemporalDelta);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(unitemporalSnapshot);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(unitemporalDelta);
            unitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD).ifPresent(stagingFieldsToIgnore::add);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(bitemporalSnapshot);
            stagingFieldsToIgnore.addAll(bitemporalSnapshot.validityMilestoning().accept(VALIDITY_FIELDS_TO_IGNORE_IN_STAGING));
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            Set stagingFieldsToIgnore = getDedupAndVersioningFields(bitemporalDelta);
            stagingFieldsToIgnore.addAll(bitemporalDelta.validityMilestoning().accept(VALIDITY_FIELDS_TO_IGNORE_IN_STAGING));
            bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD).ifPresent(stagingFieldsToIgnore::add);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return Collections.emptySet();
        }

        private Set<String> getDedupAndVersioningFields(IngestMode ingestMode)
        {
            Set<String> dedupAndVersioningFields = new HashSet<>();
            ingestMode.dataSplitField().ifPresent(dedupAndVersioningFields::add);
            ingestMode.deduplicationStrategy().accept(DeduplicationVisitors.EXTRACT_DEDUP_FIELD).ifPresent(dedupAndVersioningFields::add);
            return dedupAndVersioningFields;
        }
    };

    private static final IngestModeVisitor<Set<String>> MAIN_TABLE_FIELDS_TO_IGNORE = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(appendOnly.batchIdField());
            appendOnly.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).ifPresent(fieldsToIgnore::add);
            appendOnly.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_TO_IGNORE).ifPresent(fieldsToIgnore::add);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(nontemporalSnapshot.batchIdField());
            nontemporalSnapshot.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).ifPresent(fieldsToIgnore::add);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(nontemporalDelta.batchIdField());
            nontemporalDelta.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).ifPresent(fieldsToIgnore::add);
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return unitemporalSnapshot.transactionMilestoning().accept(TRANSACTION_FIELDS_TO_IGNORE);
        }

        @Override
        public Set<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return unitemporalDelta.transactionMilestoning().accept(TRANSACTION_FIELDS_TO_IGNORE);
        }

        @Override
        public Set<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.addAll(bitemporalSnapshot.transactionMilestoning().accept(TRANSACTION_FIELDS_TO_IGNORE));
            fieldsToIgnore.addAll(bitemporalSnapshot.validityMilestoning().accept(VALIDITY_FIELDS_TO_IGNORE));
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.addAll(bitemporalDelta.transactionMilestoning().accept(TRANSACTION_FIELDS_TO_IGNORE));
            fieldsToIgnore.addAll(bitemporalDelta.validityMilestoning().accept(VALIDITY_FIELDS_TO_IGNORE));
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(bulkLoad.batchIdField());
            bulkLoad.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).ifPresent(fieldsToIgnore::add);
            bulkLoad.digestGenStrategy().accept(EXTRACT_DIGEST_FIELD_TO_IGNORE).ifPresent(fieldsToIgnore::add);
            return fieldsToIgnore;
        }
    };

    // transaction milestoning visitors

    private static final TransactionMilestoningVisitor<Set<String>> TRANSACTION_FIELDS_TO_IGNORE = new TransactionMilestoningVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitBatchId(BatchIdAbstract batchId)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(batchId.batchIdInName());
            fieldsToIgnore.add(batchId.batchIdOutName());
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(transactionDateTime.dateTimeInName());
            fieldsToIgnore.add(transactionDateTime.dateTimeOutName());
            return fieldsToIgnore;
        }

        @Override
        public Set<String> visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(batchIdAndDateTime.batchIdInName());
            fieldsToIgnore.add(batchIdAndDateTime.batchIdOutName());
            fieldsToIgnore.add(batchIdAndDateTime.dateTimeInName());
            fieldsToIgnore.add(batchIdAndDateTime.dateTimeOutName());
            return fieldsToIgnore;
        }
    };

    // validity milestoning visitors

    private static final ValidityMilestoningVisitor<Set<String>> VALIDITY_FIELDS_TO_IGNORE = new ValidityMilestoningVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            Set<String> fieldsToIgnore = new HashSet<>();
            fieldsToIgnore.add(validDateTime.dateTimeFromName());
            fieldsToIgnore.add(validDateTime.dateTimeThruName());
            return fieldsToIgnore;
        }
    };

    private static final ValidityMilestoningVisitor<Set<String>> VALIDITY_FIELDS_TO_IGNORE_IN_STAGING = new ValidityMilestoningVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitDateTime(ValidDateTimeAbstract validDateTime)
        {
            Set<String> fieldsToIgnore = validDateTime.validityDerivation().accept(new ValidityDerivationVisitor<Set<String>>()
            {
                @Override
                public Set<String> visitSourceSpecifiesFromDateTime(SourceSpecifiesFromDateTimeAbstract sourceSpecifiesFromDateTime)
                {
                    return new HashSet<>(Arrays.asList(sourceSpecifiesFromDateTime.sourceDateTimeFromField()));
                }

                @Override
                public Set<String> visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
                {
                    return new HashSet<>(Arrays.asList(sourceSpecifiesFromAndThruDateTime.sourceDateTimeFromField(), sourceSpecifiesFromAndThruDateTime.sourceDateTimeThruField()));
                }
            });
            return fieldsToIgnore;
        }
    };

    private static final DigestGenStrategyVisitor<Optional<String>> EXTRACT_DIGEST_FIELD_TO_IGNORE = new DigestGenStrategyVisitor<Optional<String>>()
    {
        @Override
        public Optional<String> visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
        {
            return Optional.empty();
        }

        @Override
        public Optional<String> visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
        {
            return Optional.of(udfBasedDigestGenStrategy.digestField());
        }

        @Override
        public Optional<String> visitUserProvidedDigestGenStrategy(UserProvidedDigestGenStrategyAbstract userProvidedDigestGenStrategy)
        {
            return Optional.empty();
        }
    };
}
