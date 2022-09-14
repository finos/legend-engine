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
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoningVisitor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    /*
        1. Check if Schema of main table and staging table is different
        2. Check executor capabilities to check if operation is permitted
        3. If the change is a datatype change, check if it is a implicit, breaking or non-breaking change
        4. Generate the logical operation and modify the milestoning object of main table
     */

    public SchemaEvolution(Sink sink, IngestMode ingestMode)
    {
        this.sink = sink;
        this.ingestMode = ingestMode;
    }

    //todo : check if user mainDataset schema matches with the actual table structure
    public SchemaEvolutionResult buildLogicalPlanForSchemaEvolution(Dataset mainDataset, Dataset stagingDataset)
    {
        List<Operation> operations = new ArrayList<>();
        List<Field> modifiedFields = new ArrayList<>();
        operations.addAll(stagingToMainTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(MAIN_TABLE_FIELDS_TO_IGNORE), modifiedFields));
        operations.addAll(mainToStagingTableColumnMatch(mainDataset, stagingDataset, ingestMode.accept(STAGING_TABLE_FIELDS_TO_IGNORE), modifiedFields));

        SchemaDefinition evolvedSchema = evolveSchemaDefinition(mainDataset.schema(), modifiedFields);

        return SchemaEvolutionResult.of(LogicalPlan.of(operations), mainDataset.withSchema(evolvedSchema));
    }

    //todo : validate primary keys b/w the two tables and throw a ValidationException if discrepancy
    //Validate all columns (allowing exceptions) in staging dataset must have a matching column in main dataset
    private List<Operation> stagingToMainTableColumnMatch(Dataset mainDataset,
                                                          Dataset stagingDataset,
                                                          Set<String> fieldsToIgnore,
                                                          List<Field> modifiedFields)
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
                //Add the new column in the main table if schemaEvolutionPermitted and database supports ADD_COLUMN capability
                if (sink.capabilities().contains(Capability.ADD_COLUMN))
                {
                    operations.add(Alter.of(mainDataset, Alter.AlterOperation.ADD, stagingField, Optional.empty()));
                    modifiedFields.add(stagingField);
                }
                else
                {
                    throw new IllegalStateException(String.format("Field \"%s\" in staging dataset does not exist in main dataset", stagingFieldName));
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
                        if (sink.capabilities().contains(Capability.IMPLICIT_DATA_TYPE_CONVERSION) && sink.supportsImplicitMapping(matchedMainField.type().dataType(), stagingFieldType.dataType()))
                        {
                            Field newField = evolveFieldLength(stagingField, matchedMainField);
                            evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                        }
                        // If the datatype is a non-breaking change, we alter the datatype.
                        // We also alter the length if required (pick the maximum length)
                        else if (sink.capabilities().contains(Capability.EXPLICIT_DATA_TYPE_CONVERSION) && sink.supportsExplicitMapping(matchedMainField.type().dataType(), stagingFieldType.dataType()))
                        {
                            //Modify the column in main table
                            Field newField = evolveFieldLength(matchedMainField, stagingField);
                            evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                        }

                        //Else, it is a breaking change. We throw an exception
                        else
                        {
                            throw new IncompatibleSchemaChangeException("Breaking schema change from datatype " + matchedMainField.type().dataType() + " to " + stagingFieldType.dataType());
                        }
                    }
                    else
                    {
                        Field newField = evolveFieldLength(stagingField, matchedMainField);
                        evolveDataType(newField, matchedMainField, mainDataset, operations, modifiedFields);
                    }
                }
            }
        }
        return operations;
    }

    //Create alter statements if newField is different from mainDataField
    private void evolveDataType(Field newField, Field mainDataField, Dataset mainDataset, List<Operation> operations, List<Field> modifiedFields)
    {
        if (!mainDataField.equals(newField))
        {
            operations.add(Alter.of(mainDataset, Alter.AlterOperation.CHANGE_DATATYPE, newField, Optional.empty()));
            modifiedFields.add(newField);
        }
    }

    private List<Operation> mainToStagingTableColumnMatch(Dataset mainDataset, Dataset stagingDataset, Set<String> fieldsToIgnore, List<Field> modifiedFields)
    {
        List<Operation> operations = new ArrayList<>();
        List<Field> mainFields = mainDataset.schema().fields();
        Set<String> stagingFieldNames = stagingDataset.schema().fields().stream().map(Field::name).collect(Collectors.toSet());
        for (Field mainField : mainFields.stream().filter(field -> !fieldsToIgnore.contains(field.name())).collect(Collectors.toList()))
        {
            String mainFieldName = mainField.name();
            if (!stagingFieldNames.contains(mainFieldName))
            {
                if (mainField.primaryKey())
                {
                    throw new IllegalStateException(String.format("Non-nullable field \"%s\" does not exist in staging dataset", mainFieldName));
                }
                //todo : capability for nullable check?
                //Modify the column to nullable in main table
                operations.add(Alter.of(mainDataset, Alter.AlterOperation.NULLABLE_COLUMN, mainField, Optional.empty()));
                mainField = mainField.withNullable(true);
                modifiedFields.add(mainField);
            }
        }
        return operations;
    }

    private Field evolveFieldLength(Field oldField, Field newField)
    {
        int length = 0;
        int scale = 0;
        FieldType modifiedFieldType;
        if (sink.capabilities().contains(Capability.DATA_SIZING_CHANGES))
        {
            //If the oldField and newField have a length associated, pick the greater length
            if (oldField.type().length().isPresent() && newField.type().length().isPresent())
            {
                length = newField.type().length().get() >= oldField.type().length().get()
                    ? newField.type().length().get()
                    : oldField.type().length().get();
            }
            //Allow length evolution from unspecified length only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
            else if (oldField.type().dataType().equals(newField.type().dataType())
                && oldField.type().length().isPresent() && !newField.type().length().isPresent())
            {
                length = oldField.type().length().get();
            }

            //If the oldField and newField have a scale associated, pick the greater scale
            if (oldField.type().scale().isPresent() && newField.type().scale().isPresent())
            {
                scale = newField.type().scale().get() >= oldField.type().scale().get()
                    ? newField.type().scale().get()
                    : oldField.type().scale().get();
            }
            //Allow scale evolution from unspecified scale only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
            else if (oldField.type().dataType().equals(newField.type().dataType())
                && oldField.type().scale().isPresent() && !newField.type().scale().isPresent())
            {
                scale = oldField.type().scale().get();
            }
        }

        modifiedFieldType = FieldType.of(newField.type().dataType(), Optional.ofNullable(length == 0 ? null : length), Optional.ofNullable(scale == 0 ? null : scale));

        boolean nullability = newField.nullable() || oldField.nullable();
        //boolean uniqueness = !newField.isUnique() || !oldField.isUnique();

        //todo : how to handle default value, identity, uniqueness ?
        return Field.builder().name(newField.name()).primaryKey(newField.primaryKey())
            .fieldAlias(newField.fieldAlias()).nullable(nullability)
            .identity(newField.identity()).unique(newField.unique())
            .defaultValue(newField.defaultValue()).type(modifiedFieldType).build();
    }

    private SchemaDefinition evolveSchemaDefinition(SchemaDefinition schema, List<Field> modifiedFields)
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

    private static final IngestModeVisitor<Set<String>> MAIN_TABLE_FIELDS_TO_IGNORE = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return unitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD)
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
        }

        @Override
        public Set<String> visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return Collections.emptySet();
        }

        @Override
        public Set<String> visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD)
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
        }
    };

    private static final IngestModeVisitor<Set<String>> STAGING_TABLE_FIELDS_TO_IGNORE = new IngestModeVisitor<Set<String>>()
    {
        @Override
        public Set<String> visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return appendOnly.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD)
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
        }

        @Override
        public Set<String> visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return nontemporalSnapshot.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD)
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
        }

        @Override
        public Set<String> visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return nontemporalDelta.auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD)
                .map(Collections::singleton)
                .orElse(Collections.emptySet());
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
}
