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

package org.finos.legend.engine.persistence.components.relational.snowflake;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Truncate;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.LowerCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.SnowflakeDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.AlterVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.BatchEndTimestampVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.SchemaDefinitionVisitor;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor.ShowVisitor;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

public class SnowflakeSink extends AnsiSqlSink
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
        CAPABILITIES = Collections.unmodifiableSet(capabilities);

        Map<Class<?>, LogicalPlanVisitor<?>> logicalPlanVisitorByClass = new HashMap<>();
        logicalPlanVisitorByClass.put(SchemaDefinition.class, new SchemaDefinitionVisitor());
        logicalPlanVisitorByClass.put(Alter.class, new AlterVisitor());
        logicalPlanVisitorByClass.put(Show.class, new ShowVisitor());
        logicalPlanVisitorByClass.put(BatchEndTimestamp.class, new BatchEndTimestampVisitor());
        LOGICAL_PLAN_VISITOR_BY_CLASS = Collections.unmodifiableMap(logicalPlanVisitorByClass);

        IMPLICIT_DATA_TYPE_MAPPING = Collections.emptyMap();

        EXPLICIT_DATA_TYPE_MAPPING = Collections.emptyMap();

        INSTANCE = new SnowflakeSink();
    }

    public static RelationalSink get()
    {
        return INSTANCE;
    }

    public static Connection createConnection(String user,
                                              String pwd,
                                              String jdbcUrl,
                                              String account,
                                              String db,
                                              String schema)
    {
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", pwd);
        properties.put("account", account);
        properties.put("db", db);
        properties.put("schema", schema);

        try
        {
            return DriverManager.getConnection(jdbcUrl, properties);
        }
        catch (SQLException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    private SnowflakeSink()
    {
        super(
            CAPABILITIES,
            IMPLICIT_DATA_TYPE_MAPPING,
            EXPLICIT_DATA_TYPE_MAPPING,
            SqlGenUtils.QUOTE_IDENTIFIER,
            LOGICAL_PLAN_VISITOR_BY_CLASS,
            (executor, sink, dataset) ->
            {
                //TODO: pass transformer as an argument
                RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
                LogicalPlan datasetExistLogicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
                SqlPlan physicalPlanForDoesDatasetExist = transformer.generatePhysicalPlan(datasetExistLogicalPlan);
                List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlanForDoesDatasetExist);
                return results.size() > 0;
            },
            //TODO -- need to verify whether to use JDBC or SHOW command
            (executor, sink, dataset) -> sink.validateDatasetSchema(dataset, new SnowflakeDataTypeMapping()));
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
}
