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

package org.finos.legend.engine.persistence.components.relational.h2;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.LoadCsv;
import org.finos.legend.engine.persistence.components.logicalplan.values.HashFunction;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.h2.sql.H2DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.CsvExternalDatasetReferenceVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.HashFunctionVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.LoadCsvVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class H2Sink extends AnsiSqlSink
{
    private static final RelationalSink INSTANCE;

    private static final Set<Capability> CAPABILITIES;
    private static final Map<Class<?>, LogicalPlanVisitor<?>> LOGICAL_PLAN_VISITOR_BY_CLASS;
    private static final Map<DataType, Set<DataType>> IMPLICIT_DATA_TYPE_MAPPING;
    private static final Map<DataType, Set<DataType>> EXPLICIT_DATA_TYPE_MAPPING;

    static
    {
        Set<Capability> capabilities = new HashSet<>();
        capabilities.add(Capability.MERGE);
        capabilities.add(Capability.ADD_COLUMN);
        capabilities.add(Capability.IMPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.EXPLICIT_DATA_TYPE_CONVERSION);
        capabilities.add(Capability.DATA_SIZING_CHANGES);
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(HashFunction.class, new HashFunctionVisitor());
        logicalPlanVisitorByClass.put(LoadCsv.class, new LoadCsvVisitor());
        logicalPlanVisitorByClass.put(CsvExternalDatasetReference.class, new CsvExternalDatasetReferenceVisitor());
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
        EXPLICIT_DATA_TYPE_MAPPING = Collections.unmodifiableMap(explicitDataTypeMapping);

        INSTANCE = new H2Sink();
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

    private H2Sink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) -> sink.doesTableExist(dataset),
            (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new H2DataTypeMapping()));
    }
}
