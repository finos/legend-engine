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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The shared relational executor classes (RelationalResult, SQLExecutionResult, SQLResult, SQLUpdateResult, all in
 * legend-engine-xt-relationalStore-executionPlan) catch any {@link Throwable} their JDBC calls raise and, before
 * falling back to a generic {@code new RuntimeException(e)} (which buries the driver's own message behind
 * {@code e.toString()}, hiding it from Pure's exact-match {@code assertError} check), already pass a
 * {@link RuntimeException} through completely unmodified via an untouched {@code if (e instanceof RuntimeException)
 * throw (RuntimeException) e;} branch.
 * <p>
 * This proxy exploits that existing branch: it wraps a real JDBC {@link Connection} (and, recursively, every
 * {@link Statement}/{@link PreparedStatement}/{@link CallableStatement}/{@link ResultSet} it hands back) so that any
 * {@link SQLException} raised by an underlying call is cleaned via {@link DatabricksManager#cleanErrorMessage} and
 * rethrown as a {@code org.finos.legend.pure.m3.exception.PureExecutionException} - already a
 * {@link RuntimeException} - before the shared executor code's generic catch block ever sees it. No shared executor
 * class needs to change.
 * <p>
 * That exception type is constructed reflectively rather than via a compile-time dependency: this module
 * ({@code legend-engine-xt-relationalStore-databricks-execution}) is also pulled in by
 * {@code legend-engine-xt-relationalStore-executionPlan-connection-authentication-default}, which enforces (via the
 * root POM's {@code executionEnforcement} rule) that no Pure-compiler dependency reaches its classpath. The real
 * plan-execution path that actually triggers this proxy always runs with the full Pure runtime on the classpath, so
 * the reflective lookup below succeeds there; the fallback exists only so a hypothetical Pure-free caller of this
 * proxy still gets a clean {@link RuntimeException} instead of a {@link NoClassDefFoundError}.
 */
public final class DatabricksErrorCleaningJdbcProxy implements InvocationHandler
{
    private static final String PURE_EXECUTION_EXCEPTION_CLASS_NAME = "org.finos.legend.pure.m3.exception.PureExecutionException";

    private final Object delegate;
    private final DatabricksManager databaseManager;

    private DatabricksErrorCleaningJdbcProxy(Object delegate, DatabricksManager databaseManager)
    {
        this.delegate = delegate;
        this.databaseManager = databaseManager;
    }

    public static Connection wrapConnection(Connection connection, DatabricksManager databaseManager)
    {
        return wrap(connection, Connection.class, databaseManager);
    }

    private static <T> T wrap(T target, Class<T> jdbcInterface, DatabricksManager databaseManager)
    {
        if (target == null || isAlreadyWrapped(target))
        {
            return target;
        }
        Object proxy = Proxy.newProxyInstance(
                jdbcInterface.getClassLoader(),
                new Class<?>[]{jdbcInterface},
                new DatabricksErrorCleaningJdbcProxy(target, databaseManager));
        return jdbcInterface.cast(proxy);
    }

    // Proxy.isProxyClass(...) alone is not enough: the object being wrapped may itself happen to be some
    // unrelated dynamic proxy (a test double, or a future driver/pool implementation detail), which must
    // still be wrapped. Only skip wrapping when it is specifically already one of our own proxies.
    private static boolean isAlreadyWrapped(Object target)
    {
        return Proxy.isProxyClass(target.getClass()) && Proxy.getInvocationHandler(target) instanceof DatabricksErrorCleaningJdbcProxy;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        Object result;
        try
        {
            result = method.invoke(this.delegate, args);
        }
        catch (InvocationTargetException e)
        {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException)
            {
                throw newPureExecutionException(this.databaseManager.cleanErrorMessage(cause.getMessage()), cause);
            }
            throw cause;
        }
        return wrapIfJdbcHandle(result);
    }

    private static RuntimeException newPureExecutionException(String cleanedMessage, Throwable cause)
    {
        try
        {
            Class<?> pureExecutionExceptionClass = Class.forName(PURE_EXECUTION_EXCEPTION_CLASS_NAME);
            Constructor<?> constructor = pureExecutionExceptionClass.getConstructor(String.class, Throwable.class);
            return (RuntimeException) constructor.newInstance(cleanedMessage, cause);
        }
        catch (ReflectiveOperationException reflectionFailure)
        {
            return new RuntimeException(cleanedMessage, cause);
        }
    }

    private Object wrapIfJdbcHandle(Object result)
    {
        if (result instanceof CallableStatement)
        {
            return wrap((CallableStatement) result, CallableStatement.class, this.databaseManager);
        }
        if (result instanceof PreparedStatement)
        {
            return wrap((PreparedStatement) result, PreparedStatement.class, this.databaseManager);
        }
        if (result instanceof Statement)
        {
            return wrap((Statement) result, Statement.class, this.databaseManager);
        }
        if (result instanceof ResultSet)
        {
            return wrap((ResultSet) result, ResultSet.class, this.databaseManager);
        }
        return result;
    }
}
