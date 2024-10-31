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


package org.finos.legend.engine.pure.runtime.relational.sdt;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Optional;

public class RunSqlDialectTestQueryHelper
{
    private RunSqlDialectTestQueryHelper()
    {

    }

    public static <T> T runTestQueryAndTransformResultSet(String dbType, String testQuery, RichIterable<? extends String> setupSqls, RichIterable<? extends String> teardownSqls, Function<ResultSet, T> resultSetTransformFunction, MutableStack<CoreInstance> functionExpressionCallStack) throws SQLException
    {
        TestConnectionIntegration found = TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.valueOf(dbType)).getFirst();
        if (found == null)
        {
            throw new PureExecutionException("Can't find a TestConnectionIntegration for dbType " + dbType + ". Available ones are " + TestConnectionIntegrationLoader.extensions().collect(c -> c.getDatabaseType().name()), functionExpressionCallStack);
        }
        RelationalDatabaseConnection relationalDatabaseConnection = found.getConnection();

        Connection connection = null;
        Statement statement = null;
        try
        {
            LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
            flowProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
            ConnectionManagerSelector connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
            connection = connectionManagerSelector.getDatabaseConnection((Subject) null, relationalDatabaseConnection);
            statement = connection.createStatement();

            for (String s : setupSqls)
            {
                statement.execute(s);
            }

            try (ResultSet resultSet = statement.executeQuery(testQuery))
            {
                return resultSetTransformFunction.apply(resultSet);
            }
        }
        finally
        {
            if (statement != null)
            {
                for (String s : teardownSqls)
                {
                    try
                    {
                        statement.execute(s);
                    }
                    catch (Exception e)
                    {
                        // Run remaining teardown stmts without failing
                        System.out.println(e.getMessage());
                    }
                }

                statement.close();
                connection.close();
            }
        }
    }
}
