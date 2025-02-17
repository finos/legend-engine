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

package org.finos.legend.engine.persistence.components.relational;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnlyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoadAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NoOpAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.api.DataError;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.executor.RelationalExecutionHelper;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.schemaevolution.IncompatibleSchemaChangeException;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;

import java.time.Clock;
import java.util.*;
import java.util.function.Function;

public abstract class RelationalSink implements Sink
{
    private final Set<Capability> capabilities;
    private final Map<DataType, Set<DataType>> implicitDataTypeMapping;
    private final Map<DataType, Set<DataType>> explicitDataTypeMapping;
    private final DataTypeToDefaultSizeMapping dataTypeToDefaultSizeMapping;
    private final String quoteIdentifier;
    private final Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass;

    private final DatasetExists datasetExists;
    private final ValidateMainDatasetSchema validateMainDatasetSchema;
    private final ConstructDatasetFromDatabase constructDatasetFromDatabase;

    protected RelationalSink(Set<Capability> capabilities,
                             Map<DataType, Set<DataType>> implicitDataTypeMapping,
                             Map<DataType, Set<DataType>> explicitDataTypeMapping,
                             DataTypeToDefaultSizeMapping dataTypeToDefaultSizeMapping,
                             String quoteIdentifier,
                             Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass,
                             DatasetExists datasetExists,
                             ValidateMainDatasetSchema validateMainDatasetSchema,
                             ConstructDatasetFromDatabase constructDatasetFromDatabase)
    {
        this.capabilities = capabilities;
        this.implicitDataTypeMapping = implicitDataTypeMapping;
        this.explicitDataTypeMapping = explicitDataTypeMapping;
        this.dataTypeToDefaultSizeMapping = dataTypeToDefaultSizeMapping;
        this.quoteIdentifier = quoteIdentifier;
        this.logicalPlanVisitorByClass = logicalPlanVisitorByClass;
        this.datasetExists = datasetExists;
        this.validateMainDatasetSchema = validateMainDatasetSchema;
        this.constructDatasetFromDatabase = constructDatasetFromDatabase;
    }

    @Override
    public Set<Capability> capabilities()
    {
        return capabilities;
    }

    @Override
    public boolean supportsImplicitMapping(DataType source, DataType target)
    {
        return implicitDataTypeMapping.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    @Override
    public boolean supportsExplicitMapping(DataType source, DataType target)
    {
        return explicitDataTypeMapping.getOrDefault(source, Collections.emptySet()).contains(target);
    }

    @Override
    public String quoteIdentifier()
    {
        return quoteIdentifier;
    }

    @Override
    public <L extends LogicalPlanNode> LogicalPlanVisitor<L> visitorForClass(Class<?> clazz)
    {
        final LogicalPlanVisitor<?> visitor = logicalPlanVisitorByClass.get(clazz);
        if (visitor == null)
        {
            throw new IllegalArgumentException("Unable to find logical plan visitor for class: " + clazz.toString());
        }
        return (LogicalPlanVisitor<L>) visitor;
    }

    public DatasetExists datasetExistsFn()
    {
        return datasetExists;
    }

    public ValidateMainDatasetSchema validateMainDatasetSchemaFn()
    {
        return validateMainDatasetSchema;
    }

    public ConstructDatasetFromDatabase constructDatasetFromDatabaseFn()
    {
        return constructDatasetFromDatabase;
    }

    public abstract Optional<Optimizer> optimizerForCaseConversion(CaseConversion caseConversion);

    public abstract Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection connection);

    //evolve to = field to replace main column (datatype)
    //evolve from = reference field to compare sizing/nullability requirements
    @Deprecated
    @Override
    public Field evolveFieldLength(Field evolveFrom, Field evolveTo)
    {
        Optional<Integer> length = evolveTo.type().length();
        Optional<Integer> scale = evolveTo.type().scale();

        //If the oldField and newField have a length associated, pick the greater length
        if (evolveFrom.type().length().isPresent() && evolveTo.type().length().isPresent())
        {
            length = evolveTo.type().length().get() >= evolveFrom.type().length().get()
                    ? evolveTo.type().length()
                    : evolveFrom.type().length();
        }
        //Allow length evolution from unspecified length only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
        else if (evolveFrom.type().dataType().equals(evolveTo.type().dataType())
                && evolveFrom.type().length().isPresent() && !evolveTo.type().length().isPresent())
        {
            length = evolveFrom.type().length();
        }

        //If the oldField and newField have a scale associated, pick the greater scale
        if (evolveFrom.type().scale().isPresent() && evolveTo.type().scale().isPresent())
        {
            scale = evolveTo.type().scale().get() >= evolveFrom.type().scale().get()
                    ? evolveTo.type().scale()
                    : evolveFrom.type().scale();
        }
        //Allow scale evolution from unspecified scale only when data types are same. This is to avoid evolution like SMALLINT(6) -> INT(6) or INT -> DOUBLE(6) and allow for DATETIME -> DATETIME(6)
        else if (evolveFrom.type().dataType().equals(evolveTo.type().dataType())
                && evolveFrom.type().scale().isPresent() && !evolveTo.type().scale().isPresent())
        {
            scale = evolveFrom.type().scale();
        }
        return createNewField(evolveTo, evolveFrom, length, scale);
    }

    public Optional<Integer> getEvolveToLength(String columnName, Optional<Integer> mainLength, Optional<Integer> stagingLength, DataType mainType, DataType stagingType, SchemaEvolution.DataTypeEvolutionType dataTypeEvolutionType)
    {
        return getEvolveToSize(columnName, mainLength, stagingLength, mainType, stagingType, dataTypeEvolutionType, this.dataTypeToDefaultSizeMapping::getDefaultLength);
    }

    public Optional<Integer> getEvolveToScale(String columnName, Optional<Integer> mainScale, Optional<Integer> stagingScale, DataType mainType, DataType stagingType, SchemaEvolution.DataTypeEvolutionType dataTypeEvolutionType)
    {
        return getEvolveToSize(columnName, mainScale, stagingScale, mainType, stagingType, dataTypeEvolutionType, this.dataTypeToDefaultSizeMapping::getDefaultScale);
    }

    private Optional<Integer> getEvolveToSize(String columnName, Optional<Integer> mainSize, Optional<Integer> stagingSize, DataType mainType, DataType stagingType, SchemaEvolution.DataTypeEvolutionType dataTypeEvolutionType, Function<DataType, Optional<Integer>> dataTypeToDefaultSizeFunction)
    {
        // When both main and staging have size, return staging size if it is larger
        if (mainSize.isPresent() && stagingSize.isPresent())
        {
            return Optional.of(validateAgainstDecrementAndReturnStagingSize(columnName, mainSize.get(), stagingSize.get()));
        }

        // When both sides do not have size, return empty size
        if (!mainSize.isPresent() && !stagingSize.isPresent())
        {
            return Optional.empty();
        }

        // When either side does not have size, we give the default value to the missing side and return staging size if it is larger
        Optional<Integer> newStagingSize = getDefaultValueForMissingSizeAndReturnStagingSize(columnName, mainSize, stagingSize, mainType, stagingType, dataTypeToDefaultSizeFunction);
        if (newStagingSize.isPresent())
        {
            return newStagingSize;
        }
        else
        {
            // When we are unable to retrieve a default size, this means the data type does not support size
            switch (dataTypeEvolutionType)
            {
                case IMPLICIT_DATATYPE_CONVERSION:
                    return mainSize; // we follow main size because the final data type will be main type
                case EXPLICIT_DATATYPE_CONVERSION:
                    return stagingSize; // we follow staging size because the final data type will be staging type
                case SAME_DATA_TYPE:
                    return Optional.empty();
                default:
                    throw new IllegalStateException("Unexpected value: " + dataTypeEvolutionType);
            }
        }
    }

    private Optional<Integer> getDefaultValueForMissingSizeAndReturnStagingSize(String columnName, Optional<Integer> mainSize, Optional<Integer> stagingSize, DataType mainType, DataType stagingType, Function<DataType, Optional<Integer>> dataTypeToDefaultSizeFunction)
    {
        if (mainSize.isPresent() && !stagingSize.isPresent())
        {
            Optional<Integer> defaultSizeForStaging = dataTypeToDefaultSizeFunction.apply(stagingType);
            return defaultSizeForStaging.map(size -> validateAgainstDecrementAndReturnStagingSize(columnName, mainSize.get(), size));
        }
        else if (!mainSize.isPresent() && stagingSize.isPresent())
        {
            Optional<Integer> defaultSizeForMain = dataTypeToDefaultSizeFunction.apply(mainType);
            return defaultSizeForMain.map(size -> validateAgainstDecrementAndReturnStagingSize(columnName, size, stagingSize.get()));
        }
        else
        {
            throw new IllegalStateException("Size is expected to be present in either main or staging only");
        }
    }

    private int validateAgainstDecrementAndReturnStagingSize(String columnName, int mainSize, int stagingSize)
    {
        if (stagingSize < mainSize)
        {
            throw new IncompatibleSchemaChangeException(String.format("Data type size is decremented from \"%s\" to \"%s\" for column \"%s\"", mainSize, stagingSize, columnName));
        }

        return stagingSize;
    }

    @Deprecated
    @Override
    public Field createNewField(Field evolveTo, Field evolveFrom, Optional<Integer> length, Optional<Integer> scale)
    {
        FieldType modifiedFieldType = FieldType.of(evolveTo.type().dataType(), length, scale);
        boolean nullability = evolveTo.nullable() || evolveFrom.nullable();

        //todo : how to handle default value, identity, uniqueness ?
        return Field.builder().name(evolveTo.name()).primaryKey(evolveTo.primaryKey())
                .fieldAlias(evolveTo.fieldAlias()).nullable(nullability)
                .identity(evolveTo.identity()).unique(evolveTo.unique())
                .defaultValue(evolveTo.defaultValue()).type(modifiedFieldType).build();
    }

    public interface DatasetExists
    {
        boolean apply(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public interface ValidateMainDatasetSchema
    {
        void execute(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public interface ConstructDatasetFromDatabase
    {
        Dataset execute(Executor<SqlGen, TabularData, SqlPlan> executor, RelationalExecutionHelper sink, Dataset dataset);
    }

    public abstract IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues, Clock executionTimestampClock);

    public abstract List<DataError> performDryRun(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan dryRunSqlPlan, Map<ValidationCategory, List<Pair<Set<FieldValue>, org.finos.legend.engine.persistence.components.relational.SqlPlan>>> dryRunValidationSqlPlan, int sampleRowCount, CaseConversion caseConversion);

    public boolean isIngestModeSupported(IngestMode ingestMode)
    {
        return ingestMode.accept(IS_INGEST_MODE_SUPPORTED);
    }

    public static final IngestModeVisitor<Boolean> IS_INGEST_MODE_SUPPORTED = new IngestModeVisitor<Boolean>()
    {
        @Override
        public Boolean visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return true;
        }

        @Override
        public Boolean visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return true;
        }

        @Override
        public Boolean visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return false;
        }

        @Override
        public Boolean visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return true;
        }

        @Override
        public Boolean visitBulkLoad(BulkLoadAbstract bulkLoad)
        {
            return true;
        }

        @Override
        public Boolean visitNoOp(NoOpAbstract noOpAbstract)
        {
            return true;
        }
    };
}
