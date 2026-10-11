// Copyright 2026 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TestDatabricksErrorCleaningJdbcProxy
{
    private final DatabricksManager databaseManager = new DatabricksManager();

    @Test
    public void delegatesSuccessfulCallsAndRecursivelyWrapsReturnedStatementsAndResultSets() throws SQLException
    {
        FakeResultSet fakeResultSet = new FakeResultSet();
        FakeStatement fakeStatement = new FakeStatement(fakeResultSet);
        FakeConnection fakeConnection = new FakeConnection(fakeStatement);

        Connection wrapped = DatabricksErrorCleaningJdbcProxy.wrapConnection(fakeConnection.proxy, this.databaseManager);
        Statement statement = wrapped.createStatement();
        Assert.assertTrue(Proxy.isProxyClass(statement.getClass()));

        ResultSet resultSet = statement.executeQuery("select 1");
        Assert.assertTrue(Proxy.isProxyClass(resultSet.getClass()));
        Assert.assertTrue(resultSet.next());
        Assert.assertEquals("row-1", resultSet.getString(1));

        wrapped.close();
        Assert.assertTrue(fakeConnection.closed);
    }

    @Test
    public void translatesASqlExceptionRaisedDuringExecuteIntoACleanedPureExecutionException() throws SQLException
    {
        FakeStatement fakeStatement = new FakeStatement(null);
        fakeStatement.failureToRaise = new SQLException(
                "Operation failed with error: ... [USER_RAISED_EXCEPTION] Unsupported number of bits to shift - max bits allowed is 62 SQLSTATE: P0001\n\tat org.apache.spark...(Foo.scala:1)");
        FakeConnection fakeConnection = new FakeConnection(fakeStatement);

        Connection wrapped = DatabricksErrorCleaningJdbcProxy.wrapConnection(fakeConnection.proxy, this.databaseManager);
        Statement statement = wrapped.createStatement();

        try
        {
            statement.executeQuery("select raise_error(...)");
            Assert.fail("Expected a PureExecutionException");
        }
        catch (PureExecutionException e)
        {
            Assert.assertEquals("Unsupported number of bits to shift - max bits allowed is 62", e.getInfo());
            Assert.assertSame(fakeStatement.failureToRaise, e.getCause());
        }
    }

    @Test
    public void leavesNonSqlExceptionsUntranslated()
    {
        FakeConnection fakeConnection = new FakeConnection(null);
        fakeConnection.failureToRaiseOnCreateStatement = new IllegalStateException("boom");

        Connection wrapped = DatabricksErrorCleaningJdbcProxy.wrapConnection(fakeConnection.proxy, this.databaseManager);

        try
        {
            wrapped.createStatement();
            Assert.fail("Expected an IllegalStateException");
        }
        catch (SQLException e)
        {
            Assert.fail("Did not expect a checked SQLException here");
        }
        catch (IllegalStateException e)
        {
            Assert.assertEquals("boom", e.getMessage());
        }
    }

    @Test
    public void doesNotDoubleWrapAnAlreadyWrappedConnection()
    {
        FakeConnection fakeConnection = new FakeConnection(null);
        Connection wrappedOnce = DatabricksErrorCleaningJdbcProxy.wrapConnection(fakeConnection.proxy, this.databaseManager);
        Connection wrappedTwice = DatabricksErrorCleaningJdbcProxy.wrapConnection(wrappedOnce, this.databaseManager);
        Assert.assertSame(wrappedOnce, wrappedTwice);
    }

    /**
     * A hand-rolled dynamic JDBC stub (rather than a mocking library, not currently a dependency of this module)
     * that only implements the handful of {@link Connection} methods this proxy actually touches; every other
     * method throws, so any unexpected interaction fails loudly instead of silently returning null/0/false.
     */
    private static final class FakeConnection implements InvocationHandler
    {
        private final Connection proxy = (Connection) Proxy.newProxyInstance(
                FakeConnection.class.getClassLoader(), new Class<?>[]{Connection.class}, this);
        private final FakeStatement statementToReturn;
        private RuntimeException failureToRaiseOnCreateStatement;
        private boolean closed;

        private FakeConnection(FakeStatement statementToReturn)
        {
            this.statementToReturn = statementToReturn;
        }

        @Override
        public Object invoke(Object proxyArg, Method method, Object[] args)
        {
            switch (method.getName())
            {
                case "createStatement":
                    if (this.failureToRaiseOnCreateStatement != null)
                    {
                        throw this.failureToRaiseOnCreateStatement;
                    }
                    return this.statementToReturn.proxy;
                case "close":
                    this.closed = true;
                    return null;
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }

    private static final class FakeStatement implements InvocationHandler
    {
        private final Statement proxy = (Statement) Proxy.newProxyInstance(
                FakeStatement.class.getClassLoader(), new Class<?>[]{Statement.class}, this);
        private final FakeResultSet resultSetToReturn;
        private SQLException failureToRaise;

        private FakeStatement(FakeResultSet resultSetToReturn)
        {
            this.resultSetToReturn = resultSetToReturn;
        }

        @Override
        public Object invoke(Object proxyArg, Method method, Object[] args) throws SQLException
        {
            if ("executeQuery".equals(method.getName()))
            {
                if (this.failureToRaise != null)
                {
                    throw this.failureToRaise;
                }
                return this.resultSetToReturn.proxy;
            }
            throw new UnsupportedOperationException(method.getName());
        }
    }

    private static final class FakeResultSet implements InvocationHandler
    {
        private final ResultSet proxy = (ResultSet) Proxy.newProxyInstance(
                FakeResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class}, this);
        private boolean advanced;

        @Override
        public Object invoke(Object proxyArg, Method method, Object[] args)
        {
            switch (method.getName())
            {
                case "next":
                    boolean hadNotAdvancedYet = !this.advanced;
                    this.advanced = true;
                    return hadNotAdvancedYet;
                case "getString":
                    return "row-1";
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        }
    }
}
