// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.duckdb;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnlyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoadAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NoOpAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.CastFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.ConcatFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.api.IngestStatus;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalConnection;
import org.finos.legend.engine.persistence.components.relational.duckdb.jdbc.DuckDBJdbcHelper;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.DuckDBDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.DuckDBJdbcPropertiesToLogicalDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.CastFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.ConcatFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.CopyVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.DigestUdfVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.ParseJsonFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.SQLUpdateVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.StagedFilesDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.StagedFilesDatasetVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.StagedFilesFieldValueVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.StagedFilesSelectionVisitor;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.FieldVisitor;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.transformer.Transformer.TransformOptionsAbstract.DATE_TIME_FORMATTER;

public class DuckDBSink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.ALIAS_IN_HAVING);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.EXPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_TYPE_LENGTH_CHANGE);
        capabilities.add(Capability.DATA_TYPE_SCALE_CHANGE);
        capabilities.add(Capability.TRANSFORM_WHILE_COPY);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(Field.class, new FieldVisitor());
        logicalPlanVisitorByClass.put(Update.class, new SQLUpdateVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(ParseJsonFunction.class, new ParseJsonFunctionVisitor());
        logicalPlanVisitorByClass.put(Copy.class, new CopyVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDatasetReference.class, new StagedFilesDatasetReferenceVisitor());
        logicalPlanVisitorByClass.put(StagedFilesDataset.class, new StagedFilesDatasetVisitor());
        logicalPlanVisitorByClass.put(StagedFilesFieldValue.class, new StagedFilesFieldValueVisitor());
        logicalPlanVisitorByClass.put(StagedFilesSelection.class, new StagedFilesSelectionVisitor());
        logicalPlanVisitorByClass.put(ConcatFunction.class, new ConcatFunctionVisitor());
        logicalPlanVisitorByClass.put(CastFunction.class, new CastFunctionVisitor());
        logicalPlanVisitorByClass.put(DigestUdf.class, new DigestUdfVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        // TODO: These two mappings have not been confirmed, to do with schema evolution
        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.REAL, DataType.NUMERIC)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.DOUBLE)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT)));
        implicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.INT, DataType.TINYINT, DataType.SMALLINT)));
        implicitDataTypeMapping.put(DataType.SMALLINT, Collections.singleton(DataType.TINYINT));
        implicitDataTypeMapping.put(DataType.VARCHAR, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.STRING)));
        implicitDataTypeMapping.put(DataType.TIMESTAMP, Collections.singleton(DataType.DATETIME));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        Map<DataType, Set<DataType>> explicitDataTypeMapping = new HashMap<>();
        explicitDataTypeMapping.put(DataType.TINYINT, new HashSet<>(Arrays.asList(DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.SMALLINT, new HashSet<>(Arrays.asList(DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.DOUBLE, DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.DECIMAL, DataType.NUMERIC)));
        explicitDataTypeMapping.put(DataType.CHAR, new HashSet<>(Arrays.asList(DataType.VARCHAR, DataType.LONGTEXT, DataType.STRING)));
        explicitDataTypeMapping.put(DataType.VARCHAR, Collections.singleton(DataType.LONGTEXT));
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);

        INSTANCE = new DuckDBSink();
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    public static Connection createConnection(String user, String pwd, String jdbcUrl)
    {
        try
        {
            return DriverManager.getConnection(jdbcUrl, user, pwd);
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private DuckDBSink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) -> sink.doesTableExist(dataset),
            (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new DuckDBDataTypeMapping()),
            (executor, sink, dataset) -> sink.constructDatasetFromDatabase(dataset, new DuckDBJdbcPropertiesToLogicalDataTypeMapping(), false));
    }

    @Override
    public Executor<SqlGen, TabularData, SqlPlan> getRelationalExecutor(RelationalConnection relationalConnection)
    {
        if (relationalConnection instanceof JdbcConnection)
        {
            JdbcConnection jdbcConnection = (JdbcConnection) relationalConnection;
            return new RelationalExecutor(this, DuckDBJdbcHelper.of(jdbcConnection.connection()));
        }
        else
        {
            throw new UnsupportedOperationException("Only JdbcConnection is supported for DuckDB Sink");
        }
    }

    @Override
    public Field evolveFieldLength(Field evolveFrom, Field evolveTo)
    {
        // We are not allowing evolving from unspecified length/scale for DuckDB

        Optional<Integer> length = evolveTo.type().length();
        Optional<Integer> scale = evolveTo.type().scale();

        //If the oldField and newField have a length associated, pick the greater length
        if (evolveFrom.type().length().isPresent() && evolveTo.type().length().isPresent())
        {
            length = evolveTo.type().length().get() >= evolveFrom.type().length().get()
                ? evolveTo.type().length()
                : evolveFrom.type().length();
        }

        //If the oldField and newField have a scale associated, pick the greater scale
        if (evolveFrom.type().scale().isPresent() && evolveTo.type().scale().isPresent())
        {
            scale = evolveTo.type().scale().get() >= evolveFrom.type().scale().get()
                ? evolveTo.type().scale()
                : evolveFrom.type().scale();
        }

        return createNewField(evolveTo, evolveFrom, length, scale);
    }

    @Override
    public IngestorResult performBulkLoad(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan ingestSqlPlan, Map<StatisticName, SqlPlan> statisticsSqlPlan, Map<String, PlaceholderValue> placeHolderKeyValues, Clock executionTimestampClock)
    {
        executor.executePhysicalPlan(ingestSqlPlan, placeHolderKeyValues);

        Map<StatisticName, Object> stats = new HashMap<>();
        SqlPlan rowsInsertedSqlPlan = statisticsSqlPlan.get(StatisticName.ROWS_INSERTED);
        if (rowsInsertedSqlPlan != null)
        {
            stats.put(StatisticName.ROWS_INSERTED, executor.executePhysicalPlanAndGetResults(rowsInsertedSqlPlan, placeHolderKeyValues)
                .stream()
                .findFirst()
                .map(TabularData::data)
                .flatMap(t -> t.stream().findFirst())
                .map(Map::values)
                .flatMap(t -> t.stream().findFirst())
                .orElseThrow(IllegalStateException::new));
        }

        IngestorResult result;
        result = IngestorResult.builder()
            .status(IngestStatus.SUCCEEDED)
            .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN).value()) : null))
            .updatedDatasets(datasets)
            .putAllStatisticByName(stats)
            .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN).value())
            .ingestionEndTimestampUTC(LocalDateTime.now(executionTimestampClock).format(DATE_TIME_FORMATTER))
            .build();

        return result;
    }

    @Override
    public boolean isIngestModeSupported(IngestMode ingestMode)
    {
        return ingestMode.accept(IS_INGEST_MODE_SUPPORTED);
    }

    private static final IngestModeVisitor<Boolean> IS_INGEST_MODE_SUPPORTED = new IngestModeVisitor<Boolean>()
    {
        @Override
        public Boolean visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return true;
        }

        @Override
        public Boolean visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return false;
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
            return false;
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
