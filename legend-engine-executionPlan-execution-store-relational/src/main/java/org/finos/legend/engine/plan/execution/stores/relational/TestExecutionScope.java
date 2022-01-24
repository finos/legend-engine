// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

import io.opentracing.Scope;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.block.procedure.checked.CheckedProcedure;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.tools.Server;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class TestExecutionScope implements Closeable
{
    private final int port;
    private final Server server;

    public TestExecutionScope(int port, Server server)
    {
        this.port = port;
        this.server = server;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public void close() throws IOException
    {
        this.server.stop();
    }
    public static int generatePort()
    {
       return DynamicPortGenerator.generatePort();
    }
    public static TestExecutionScope setupTestServer(RichIterable<? extends String> sqls, Scope scope) throws SQLException
    {
        // Start Test Database
        int relationalDBPort = generatePort();
        RelationalExecutor executor ;
        Server server ;
        try
        {
           executor = buildTestExecutor(relationalDBPort);
           server = AlloyH2Server.startServer(relationalDBPort);
        }
        catch (JdbcSQLNonTransientConnectionException b)
        {
            scope.span().log("exception opening port"+ relationalDBPort+". Retrying with another port.");
            relationalDBPort = generatePort();
            executor = buildTestExecutor(relationalDBPort);
            server = AlloyH2Server.startServer(relationalDBPort);
        }
        scope.span().log("In memory database started on port " + relationalDBPort);

        try
        {
            // Set Up Data
            try (Connection connection = executor.getConnectionManager().getTestDatabaseConnection())
            {
                sqls.forEach(new CheckedProcedure<String>()
                {
                    @Override
                    public void safeValue(String s) throws Exception
                    {
                        try (Statement statement = connection.createStatement())
                        {
                            statement.executeUpdate(s);
                        }
                    }
                });
            }

            scope.span().log("Data inserted in the test database");
        }
        catch (Exception e)
        {
            server.shutdown();
            server.stop();
            throw e;
        }
        return new TestExecutionScope(relationalDBPort, server);
    }

    public static RelationalExecutor buildTestExecutor(int port)
    {
        RelationalExecutionConfiguration relationalExecutionConfiguration = new RelationalExecutionConfiguration();
        relationalExecutionConfiguration.tempPath = "/tmp/";
        TemporaryTestDbConfiguration temporaryTestDbConfiguration = new TemporaryTestDbConfiguration();
        temporaryTestDbConfiguration.port = port;
        return new RelationalExecutor(temporaryTestDbConfiguration, relationalExecutionConfiguration);
    }

    public static Runtime buildTestRuntime(Runtime runtime)
    {
        return buildTestRuntime(runtime, null);
    }

    public static Runtime buildTestRuntime(Runtime runtime, String testData)
    {
        return buildTestRuntime(runtime, testData, null);
    }

    public static Runtime buildTestRuntime(Runtime runtime, String testData, List<String> setupSqls)
    {
        if (runtime instanceof LegacyRuntime)
        {
            LegacyRuntime newRuntime = new LegacyRuntime();
            newRuntime.connections = ListIterate.collect(((LegacyRuntime) runtime).connections, connection -> {
                if (connection instanceof ModelChainConnection)
                {
                    return connection;
                }
                return ConnectionManagerSelector.transformToTestConnectionSpecification(connection, testData, setupSqls);
            });
            return newRuntime;
        }
        else if (runtime instanceof EngineRuntime)
        {
            EngineRuntime newRuntime = new EngineRuntime();
            newRuntime.connections = ListIterate.collect(((EngineRuntime) runtime).connections, storeConnections -> {
                StoreConnections newStoreConnections = new StoreConnections();
                newStoreConnections.store = storeConnections.store;
                newStoreConnections.storeConnections = ListIterate.collect(storeConnections.storeConnections, identifiedConnection -> {
                    if (identifiedConnection.connection instanceof ModelChainConnection)
                    {
                        return identifiedConnection;
                    }
                    IdentifiedConnection newIdentifiedConnection = new IdentifiedConnection();
                    newIdentifiedConnection.id = identifiedConnection.id;
                    newIdentifiedConnection.connection = ConnectionManagerSelector.transformToTestConnectionSpecification(identifiedConnection.connection, testData, setupSqls);
                    return newIdentifiedConnection;
                });
                return newStoreConnections;
            });
            return newRuntime;
        }
        else if (runtime instanceof RuntimePointer)
        {
            return runtime;
        }
        throw new UnsupportedOperationException();
    }
}
