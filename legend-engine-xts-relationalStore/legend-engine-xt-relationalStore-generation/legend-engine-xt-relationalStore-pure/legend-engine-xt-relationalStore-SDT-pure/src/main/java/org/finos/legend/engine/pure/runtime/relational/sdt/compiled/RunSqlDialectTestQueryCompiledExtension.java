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

package org.finos.legend.engine.pure.runtime.relational.sdt.compiled;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.pure.runtime.relational.sdt.RunSqlDialectTestQueryHelper;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_SQLNull_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_ResultSet;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_ResultSet_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_Row;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_execute_Row_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.RelationalNativeImplementation;
import org.finos.legend.pure.runtime.java.extension.store.relational.compiled.natives.ResultSetValueHandlers;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class RunSqlDialectTestQueryCompiledExtension extends BaseCompiledExtension
{
    public RunSqlDialectTestQueryCompiledExtension()
    {
        super(
                "core_external_store_relational_sdt",
                () -> Lists.fixedSize.with(new RunSqlDialectTestQuery()),
                Lists.fixedSize.with(),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static class RunSqlDialectTestQuery extends AbstractNative
    {
        public RunSqlDialectTestQuery()
        {
            super("runSqlDialectTestQuery_String_1__String_1__String_MANY__String_MANY__ResultSet_1_");
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            return "org.finos.legend.engine.pure.runtime.relational.sdt.compiled.RunSqlDialectTestQueryCompiledExtension.RunSqlDialectTestQuery.compileExec(" +
                    transformedParams.get(0) + ", " + // DB Type
                    transformedParams.get(1) + ", " + // Test Query
                    transformedParams.get(2) + ", " + // Setup Sqls
                    transformedParams.get(3) + ", " + // Teardown Sqls
                    "(CompiledExecutionSupport) es)";
        }

        public static Root_meta_relational_metamodel_execute_ResultSet compileExec(String dbType, String testQuery, RichIterable<? extends String> setupSqls, RichIterable<? extends String> teardownSqls, CompiledExecutionSupport es)
        {
            Function<ResultSet, Root_meta_relational_metamodel_execute_ResultSet> transformer = (resultSet) ->
            {
                try
                {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    Root_meta_relational_metamodel_execute_ResultSet pureResult = new Root_meta_relational_metamodel_execute_ResultSet_Impl("");
                    SQLNull sqlNull = new Root_meta_relational_metamodel_SQLNull_Impl("SQLNull");

                    int count = resultSetMetaData.getColumnCount();
                    MutableList<String> columns = FastList.newList(count);
                    for (int i = 1; i <= count; i++)
                    {
                        String column = resultSetMetaData.getColumnLabel(i);
                        columns.add(column);
                    }
                    pureResult._columnNames(columns);

                    ListIterable<ResultSetValueHandlers.ResultSetValueHandler> handlers = ResultSetValueHandlers.getHandlers(resultSetMetaData);
                    MutableList<Root_meta_relational_metamodel_execute_Row> rows = FastList.newList();
                    while (resultSet.next())
                    {
                        MutableList<Object> rowValues = RelationalNativeImplementation.processRow(resultSet, handlers, sqlNull, new GregorianCalendar(TimeZone.getTimeZone("UTC")));
                        rows.add((new Root_meta_relational_metamodel_execute_Row_Impl(""))._valuesAddAll(rowValues)._parent(pureResult));
                    }
                    pureResult._rows(rows);
                    return  pureResult;
                }
                catch (SQLException e)
                {
                    throw new PureExecutionException(e, Stacks.mutable.empty());
                }
            };

            try
            {
                return RunSqlDialectTestQueryHelper.runTestQueryAndTransformResultSet(dbType, testQuery, setupSqls, teardownSqls, transformer, Stacks.mutable.empty());
            }
            catch (SQLException e)
            {
                throw new PureExecutionException(e, Stacks.mutable.empty());
            }
        }
    }
}
