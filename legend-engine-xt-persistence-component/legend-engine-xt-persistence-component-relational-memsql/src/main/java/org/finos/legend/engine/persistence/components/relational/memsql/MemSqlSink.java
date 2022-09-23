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

package org.finos.legend.engine.persistence.components.relational.memsql;

import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.memsql.sql.MemSqlDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor.SQLUpdateVisitor;
import org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor.ShowVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.ColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.PKColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.UniqueColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MemSqlSink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.EXPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_SIZING_CHANGES);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Show.class, new ShowVisitor());
        logicalPlanVisitorByClass.put(Update.class, new SQLUpdateVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        Map<DataType, Set<DataType>> implicitDataTypeMapping = new HashMap<>();
        implicitDataTypeMapping.put(DataType.DECIMAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.DOUBLE, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.FLOAT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.REAL)));
        implicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.DOUBLE)));
        implicitDataTypeMapping.put(DataType.BIGINT, new HashSet<>(Arrays.asList(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT)));
        implicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.INT, DataType.TINYINT, DataType.SMALLINT)));
        implicitDataTypeMapping.put(DataType.INT, new HashSet<>(Arrays.asList(DataType.INTEGER, DataType.TINYINT, DataType.SMALLINT)));
        implicitDataTypeMapping.put(DataType.SMALLINT, Collections.singleton(DataType.TINYINT));
        implicitDataTypeMapping.put(DataType.VARCHAR, Collections.singleton(DataType.CHAR));
        implicitDataTypeMapping.put(DataType.LONGTEXT, new HashSet<>(Arrays.asList(DataType.CHAR, DataType.VARCHAR)));
        IMPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(implicitDataTypeMapping);

        Map<DataType, Set<DataType>> explicitDataTypeMapping = new HashMap<>();
        explicitDataTypeMapping.put(DataType.TINYINT, new HashSet<>(Arrays.asList(DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.SMALLINT, new HashSet<>(Arrays.asList(DataType.INTEGER, DataType.INT, DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.INTEGER, new HashSet<>(Arrays.asList(DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.INT, new HashSet<>(Arrays.asList(DataType.BIGINT, DataType.FLOAT, DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.BIGINT, Collections.singleton(DataType.DECIMAL));
        explicitDataTypeMapping.put(DataType.FLOAT, new HashSet<>(Arrays.asList(DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.REAL, new HashSet<>(Arrays.asList(DataType.DOUBLE, DataType.DECIMAL)));
        explicitDataTypeMapping.put(DataType.DOUBLE, Collections.singleton(DataType.DECIMAL));
        explicitDataTypeMapping.put(DataType.CHAR, new HashSet<>(Arrays.asList(DataType.VARCHAR, DataType.LONGTEXT)));
        explicitDataTypeMapping.put(DataType.VARCHAR, Collections.singleton(DataType.LONGTEXT));
        explicitDataTypeMapping.put(DataType.TIMESTAMP, Collections.singleton(DataType.DATETIME));
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);

        INSTANCE = new MemSqlSink();
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

    private MemSqlSink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.BACK_QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) -> sink.doesTableExist(dataset),
            VALIDATE_MAIN_DATASET_SCHEMA);
    }


    @Override
    public Optional<Optimizer> optimizerForCaseConversion(CaseConversion caseConversion)
    {
        switch (caseConversion)
        {
            case TO_LOWER:
                return Optional.of(new LowerCaseOptimizer());
            case TO_UPPER:
                return Optional.of(new UpperCaseOptimizer());
            case NONE:
                return Optional.empty();
            default:
                throw new IllegalArgumentException("Unrecognized case conversion: " + caseConversion);
        }
    }

    static final RelationalSink.ValidateMainDatasetSchema VALIDATE_MAIN_DATASET_SCHEMA = new RelationalSink.ValidateMainDatasetSchema()
    {
        @Override
        public void execute(Executor<SqlGen, TabularData, SqlPlan> executor, JdbcHelper sink, Dataset dataset)
        {
            RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
            LogicalPlan validateDatasetSchemaLogicalPlan = LogicalPlanFactory.getLogicalPlanForValidateDatasetSchema(dataset);
            SqlPlan validateDatasetSchemaPhysicalPlan = transformer.generatePhysicalPlan(validateDatasetSchemaLogicalPlan);
            List<TabularData> results = executor.executePhysicalPlanAndGetResults(validateDatasetSchemaPhysicalPlan);

            List<Field> userFields = new ArrayList<>(dataset.schema().fields());
            List<Column> userColumns = JdbcHelper.convertUserProvidedFieldsToColumns(userFields, new MemSqlDataTypeMapping());
            List<Column> dbColumns = new ArrayList<>();

            for (Map<String, Object> m : results.get(0).getData())
            {
                String columnName = (String) m.get("Field");

                // Get the data type
                String dataTypeString = (String) m.get("Type");
                org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType dataType = convertStringToDataType(dataTypeString, new MemSqlDataTypeMapping());

                // Get the constraints
                List<ColumnConstraint> columnConstraints = new ArrayList<>();
                String[] keyStringArr = ((String) m.get("Key")).split(",");
                boolean isPrimaryKey = Arrays.asList(keyStringArr).contains("PRI");
                boolean isUnique = Arrays.asList(keyStringArr).contains("UNI");
                boolean isNullable = m.get("Null").equals(JdbcHelper.BOOL_TRUE_STRING_VALUE);

                if (!isNullable || isPrimaryKey)
                {
                    columnConstraints.add(new NotNullColumnConstraint());
                }
                if (isPrimaryKey)
                {
                    columnConstraints.add(new PKColumnConstraint());
                }
                if (isUnique || isPrimaryKey)
                {
                    columnConstraints.add(new UniqueColumnConstraint());
                }

                Column column = new Column(columnName, dataType, columnConstraints, null);
                dbColumns.add(column);
            }

            // Compare the schemas
            JdbcHelper.validateColumns(userColumns, dbColumns);
        }

        private org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType convertStringToDataType(String s, DataTypeMapping dataTypeMapping)
        {
            // Handle different formats: DECIMAL, DECIMAL(10), DECIMAL(10,2)
            String[] stringArr = s.split("\\(");
            String dataTypeString = stringArr[0];
            Integer length = null;
            Integer scale = null;

            if (stringArr.length > 1)
            {
                String temp = stringArr[1].substring(0, stringArr[1].length() - 1);
                String[] tempArr = temp.split(",");
                length = Integer.parseInt(tempArr[0]);
                if (tempArr.length > 1)
                {
                    scale = Integer.parseInt(tempArr[1]);
                }
            }

            DataType matchedDataType = null;
            for (DataType d : DataType.values())
            {
                if (d.name().equalsIgnoreCase(dataTypeString))
                {
                    matchedDataType = d;
                    break;
                }
            }
            if (matchedDataType == null)
            {
                throw new IllegalStateException("Unknown data type in the database schema");
            }

            FieldType fieldType = FieldType.of(matchedDataType, length, scale);
            return dataTypeMapping.getDataType(fieldType);
        }
    };
}
