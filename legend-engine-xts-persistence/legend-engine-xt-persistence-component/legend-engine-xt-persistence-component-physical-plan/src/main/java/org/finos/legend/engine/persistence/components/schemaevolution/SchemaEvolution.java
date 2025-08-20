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
import org.finos.legend.engine.persistence.components.ingestmode.NoOpAbstract;
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
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.AlterAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.operations.AlterOptimizationKeyAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.operations.AlterOptimizationKey;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    public enum DataTypeEvolutionType
    {
        SAME_DATA_TYPE,
        IMPLICIT_DATATYPE_CONVERSION,
        EXPLICIT_DATATYPE_CONVERSION
    }

    private final Sink sink;
    private final IngestMode ingestMode;
    private final Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet;
    private final boolean ignoreCase;

    /*
        1. Validate that nothing has changed with the primary keys of the main table.
        2. Check if Schema of main table and staging table is different and accordingly modify schema.
        3. Check executor capabilities to check if operation is permitted, else throw an exception.
        4. Generate the logical operation and modify the schema of the main dataset object.
     */

    public SchemaEvolution(Sink sink, IngestMode ingestMode, Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet, boolean ignoreCase)
    {
        this.sink = sink;
        this.ingestMode = ingestMode;
        this.schemaEvolutionCapabilitySet = schemaEvolutionCapabilitySet;
        this.ignoreCase = ignoreCase;
    }

    public SchemaEvolutionResult buildLogicalPlanForSchemaEvolution(Dataset mainDataset, SchemaDefinition stagingDataset)
    {
        List<Operation> operations = new ArrayList<>();
        Set<Field> modifiedFields = new HashSet<>();
        validatePrimaryKeys(mainDataset, stagingDataset);
        operations.addAll(stagingToMainTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(STAGING_TABLE_FIELDS_TO_IGNORE), modifiedFields));
        operations.addAll(mainToStagingTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(MAIN_TABLE_FIELDS_TO_IGNORE), modifiedFields));

        SchemaDefinition evolvedSchema = evolveSchemaDefinition(mainDataset.schema(), modifiedFields);

        return SchemaEvolutionResult.of(LogicalPlan.of(operations), mainDataset.withSchema(evolvedSchema));
    }

    private void validatePrimaryKeys(Dataset mainDataset, SchemaDefinition stagingDataset)
    {
        List<Field> stagingFilteredFields = stagingDataset.fields().stream().filter(field -> !collectionContainsElement(ingestMode.accept(STAGING_TABLE_FIELDS_TO_IGNORE), field.name())).collect(Collectors.toList());
        Set<String> stagingPkNames = stagingFilteredFields.stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        List<Field> mainFilteredFields = mainDataset.schema().fields().stream().filter(field -> !collectionContainsElement(ingestMode.accept(MAIN_TABLE_FIELDS_TO_IGNORE), field.name())).collect(Collectors.toList());
        Set<String> mainPkNames = mainFilteredFields.stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        if (!areEqual(stagingPkNames, mainPkNames))
        {
            throw new IncompatibleSchemaChangeException("Primary keys for main table has changed which is not allowed");
        }
    }

    //Validate all columns (allowing exceptions) in staging dataset must have a matching column in main dataset
    private List<Operation> stagingToMainTableColumnMatch(Dataset mainDataset, SchemaDefinition stagingDataset, Set<String> fieldsToIgnore, Set<Field> modifiedFields)
    {
        List<Operation> operations = new ArrayList<>();

        for (Field stagingField : stagingDataset.fields().stream().filter(field -> !collectionContainsElement(fieldsToIgnore, field.name())).collect(Collectors.toList()))
        {
            Field matchedMainField = mainDataset.schema().fields().stream().filter(mainField -> areEqual(mainField.name(), stagingField.name())).findFirst().orElse(null);
            if (matchedMainField == null)
            {
                addColumn(mainDataset, modifiedFields, operations, stagingField);
            }
            else
            {
                evolveColumn(mainDataset, modifiedFields, operations, matchedMainField, stagingField);
            }
        }

        if (!areEqual(stagingDataset.clusterKeys().toString(), mainDataset.schema().clusterKeys().toString()))
        {
            operations.add(AlterOptimizationKey.of(mainDataset, AlterOptimizationKeyAbstract.AlterOperation.ALTER_CLUSTER_KEY, stagingDataset.clusterKeys()));
        }
        return operations;
    }

    private void addColumn(Dataset mainDataset, Set<Field> modifiedFields, List<Operation> operations, Field stagingField)
    {
        // Add the new column in the main table if database supports ADD_COLUMN capability and if user capability supports ADD_COLUMN
        if (sink.capabilities().contains(Capability.ADD_COLUMN)
            && (schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.ADD_COLUMN)))
        {
            if (stagingField.nullable())
            {
                operations.add(Alter.of(mainDataset, Alter.AlterOperation.ADD, stagingField, Optional.empty()));
                modifiedFields.add(stagingField);
            }
            else
            {
                throw new IncompatibleSchemaChangeException(String.format("Non-nullable field \"%s\" in staging dataset cannot be added, as it is backward-incompatible change.", stagingField.name()));
            }
        }
        else
        {
            throw new IncompatibleSchemaChangeException(String.format("Field \"%s\" in staging dataset does not exist in main dataset. Couldn't add column since sink/user capabilities do not permit operation.", stagingField.name()));
        }
    }

    private void evolveColumn(Dataset mainDataset, Set<Field> modifiedFields, List<Operation> operations, Field matchedMainField, Field stagingField)
    {
        String columnName = matchedMainField.name();

        DataType mainDataType = matchedMainField.type().dataType();
        Optional<Integer> mainLength = matchedMainField.type().length();
        Optional<Integer> mainScale = matchedMainField.type().scale();
        DataType stagingDataType = stagingField.type().dataType();
        Optional<Integer> stagingLength = stagingField.type().length();
        Optional<Integer> stagingScale = stagingField.type().scale();

        DataTypeEvolutionType dataTypeEvolutionType = getDataTypeEvolutionType(mainDataType, stagingDataType);
        DataType evolveToDataType = getEvolveToDataType(mainDataType, stagingDataType, dataTypeEvolutionType);
        Optional<Integer> evolveToLength = sink.getEvolveToLength(columnName, mainLength, stagingLength, mainDataType, stagingDataType, dataTypeEvolutionType);
        Optional<Integer> evolveToScale = sink.getEvolveToScale(columnName, mainScale, stagingScale, mainDataType, stagingDataType, dataTypeEvolutionType);
        boolean evolveToNullable = stagingField.nullable();

        Field evolveTo = matchedMainField
            .withType(FieldType.builder().dataType(evolveToDataType).length(evolveToLength).scale(evolveToScale).build())
            .withNullable(evolveToNullable);

        createAlterColumnStatements(evolveTo, matchedMainField, mainDataset, operations, modifiedFields);
    }

    private DataTypeEvolutionType getDataTypeEvolutionType(DataType mainDataType, DataType stagingDataType)
    {
        if (mainDataType.equals(stagingDataType))
        {
            return DataTypeEvolutionType.SAME_DATA_TYPE;
        }
        else
        {
            if (sink.capabilities().contains(Capability.IMPLICIT_DATA_TYPE_CONVERSION)
                && sink.supportsImplicitMapping(mainDataType, stagingDataType))
            {
                return DataTypeEvolutionType.IMPLICIT_DATATYPE_CONVERSION;
            }
            else if (sink.capabilities().contains(Capability.EXPLICIT_DATA_TYPE_CONVERSION)
                && schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_CONVERSION))
            {
                if (sink.supportsExplicitMapping(mainDataType, stagingDataType))
                {
                    return DataTypeEvolutionType.EXPLICIT_DATATYPE_CONVERSION;
                }
                else
                {
                    throw new IncompatibleSchemaChangeException(String.format("Breaking schema change from datatype \"%s\" to \"%s\"", mainDataType, stagingDataType));
                }
            }
            else
            {
                throw new IncompatibleSchemaChangeException(String.format("Explicit data type conversion from \"%s\" to \"%s\" couldn't be performed since sink/user capability does not allow it", mainDataType, stagingDataType));
            }
        }
    }

    private DataType getEvolveToDataType(DataType mainDataType, DataType stagingDataType, DataTypeEvolutionType dataTypeEvolutionType)
    {
        switch (dataTypeEvolutionType)
        {
            case SAME_DATA_TYPE:
            case IMPLICIT_DATATYPE_CONVERSION:
                return mainDataType;
            case EXPLICIT_DATATYPE_CONVERSION:
                return stagingDataType;
            default:
                throw new IllegalStateException("Unexpected value: " + dataTypeEvolutionType);
        }
    }

    //Create alter statements if newField is different from mainDataField
    private void createAlterColumnStatements(Field newField, Field mainDataField, Dataset mainDataset, List<Operation> operations, Set<Field> modifiedFields)
    {
        if (!mainDataField.equals(newField))
        {
            createAlterColumnTypeAndSizeStatements(newField, mainDataField, mainDataset, operations, modifiedFields);
            createAlterColumnNullabilityStatements(newField, mainDataField, mainDataset, operations, modifiedFields);
        }
    }

    private void createAlterColumnTypeAndSizeStatements(Field newField, Field mainDataField, Dataset mainDataset, List<Operation> operations, Set<Field> modifiedFields)
    {
        // If there are any data type length changes, make sure sink/user capability allows it before creating the alter statement
        if (!Objects.equals(mainDataField.type().length(), newField.type().length()))
        {
            if (!sink.capabilities().contains(Capability.DATA_TYPE_LENGTH_CHANGE)
                    || !schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE))
            {
                throw new IncompatibleSchemaChangeException(String.format("Data type length changes couldn't be performed on column \"%s\" since sink/user capability does not allow it", newField.name()));
            }
        }
        // If there are any data type scale changes, make sure sink/user capability allows it before creating the alter statement
        if (!Objects.equals(mainDataField.type().scale(), newField.type().scale()))
        {
            if (!sink.capabilities().contains(Capability.DATA_TYPE_SCALE_CHANGE)
                    || !schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE))
            {
                throw new IncompatibleSchemaChangeException(String.format("Data type scale changes couldn't be performed on column \"%s\" since sink/user capability does not allow it", newField.name()));
            }
        }

        // Create the alter statement for changing the data type and sizing as required
        if (!mainDataField.type().equals(newField.type()))
        {
            operations.add(Alter.of(mainDataset, Alter.AlterOperation.CHANGE_DATATYPE, newField, Optional.empty()));
            modifiedFields.add(newField);
        }
    }

    private void createAlterColumnNullabilityStatements(Field newField, Field mainDataField, Dataset mainDataset, List<Operation> operations, Set<Field> modifiedFields)
    {
        if (mainDataField.nullable() && !newField.nullable())
        {
            throw new IncompatibleSchemaChangeException(String.format("Column \"%s\" cannot be changed from nullable to non-nullable", mainDataField.name()));
        }
        else if (!mainDataField.nullable() && newField.nullable())
        {
            // We do not allow changing nullability for PKs
            if (!mainDataField.primaryKey())
            {
                // Create the alter statement for changing the column nullability
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
    }

    private List<Operation> mainToStagingTableColumnMatch(Dataset mainDataset, SchemaDefinition stagingDataset, Set<String> fieldsToIgnore, Set<Field> modifiedFields)
    {
        List<Operation> operations = new ArrayList<>();
        List<Field> mainFields = mainDataset.schema().fields();
        Set<String> stagingFieldNames = stagingDataset.fields().stream().map(Field::name).collect(Collectors.toSet());
        for (Field mainField : mainFields.stream().filter(field -> !collectionContainsElement(fieldsToIgnore, field.name())).collect(Collectors.toList()))
        {
            String mainFieldName = mainField.name();
            if (!collectionContainsElement(stagingFieldNames, mainFieldName))
            {
                if (schemaEvolutionCapabilitySet.contains(SchemaEvolutionCapability.ALLOW_MISSING_COLUMNS))
                {
                    //Modify the column to nullable in main table
                    if (!mainField.nullable())
                    {
                        mainField = mainField.withNullable(true);
                        operations.add(Alter.of(mainDataset, AlterAbstract.AlterOperation.NULLABLE_COLUMN, mainField, Optional.empty()));
                        modifiedFields.add(mainField);
                    }
                }
                else
                {
                    throw new IncompatibleSchemaChangeException(String.format("Column \"%s\" is missing from incoming schema, but user capability does not allow missing columns", mainField.name()));
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
                .filter(f -> !collectionContainsElement(modifiedFieldNames, f.name()))
                .collect(Collectors.toList());

        evolvedFields.addAll(modifiedFields);

        return schema.withFields(evolvedFields);
    }

    private boolean areEqual(String str1, String str2)
    {
        return ignoreCase ? str1.equalsIgnoreCase(str2) : str1.equals(str2);
    }

    private boolean areEqual(Set<String> set1, Set<String> set2)
    {
        if (ignoreCase)
        {
            Set<String> upperCasedSet1 = set1.stream().map(String::toUpperCase).collect(Collectors.toSet());
            Set<String> upperCasedSet2 = set2.stream().map(String::toUpperCase).collect(Collectors.toSet());
            return Objects.equals(upperCasedSet1, upperCasedSet2);
        }
        else
        {
            return Objects.equals(set1, set2);
        }
    }

    private boolean collectionContainsElement(Collection<String> collection, String target)
    {
        return ignoreCase ? collection.stream().anyMatch(element -> element.equalsIgnoreCase(target)) : collection.contains(target);
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
            unitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_INDICATOR_FIELD).ifPresent(stagingFieldsToIgnore::add);
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
            bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_INDICATOR_FIELD).ifPresent(stagingFieldsToIgnore::add);
            return stagingFieldsToIgnore;
        }

        @Override
        public Set<String> visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitNoOp(NoOpAbstract noOpAbstract)
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

        @Override
        public Set<String> visitNoOp(NoOpAbstract noOpAbstract)
        {
            return Collections.emptySet();
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
                    if (sourceSpecifiesFromDateTime.preserveSourceSpecifiedField().orElse(false))
                    {
                        return new HashSet<>();
                    }
                    else
                    {
                        return new HashSet<>(Arrays.asList(sourceSpecifiesFromDateTime.sourceDateTimeFromField()));
                    }
                }

                @Override
                public Set<String> visitSourceSpecifiesFromAndThruDateTime(SourceSpecifiesFromAndThruDateTimeAbstract sourceSpecifiesFromAndThruDateTime)
                {
                    if (sourceSpecifiesFromAndThruDateTime.preserveSourceSpecifiedField().orElse(false))
                    {
                        return new HashSet<>();
                    }
                    else
                    {
                        return new HashSet<>(Arrays.asList(sourceSpecifiesFromAndThruDateTime.sourceDateTimeFromField(), sourceSpecifiesFromAndThruDateTime.sourceDateTimeThruField()));
                    }
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
