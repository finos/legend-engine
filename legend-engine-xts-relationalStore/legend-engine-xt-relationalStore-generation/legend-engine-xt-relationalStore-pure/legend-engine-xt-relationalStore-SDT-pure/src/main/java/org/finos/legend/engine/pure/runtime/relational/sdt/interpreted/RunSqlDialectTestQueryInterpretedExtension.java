//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.pure.runtime.relational.sdt.interpreted;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.pure.runtime.relational.sdt.RunSqlDialectTestQueryHelper;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.extension.store.relational.interpreted.natives.ExecuteInDb;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Stack;

public class RunSqlDialectTestQueryInterpretedExtension extends BaseInterpretedExtension
{
    public RunSqlDialectTestQueryInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("runSqlDialectTestQuery_String_1__String_1__String_MANY__String_MANY__ResultSet_1_", RunSqlDialectTestQuery::new)
        ));
    }

    public static class RunSqlDialectTestQuery extends NativeFunction
    {
        private final ModelRepository repository;

        public RunSqlDialectTestQuery(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
        {
            this.repository = repository;
        }

        @Override
        public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
        {
            String dbType = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();
            String testQuery = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport).getName();
            ListIterable<? extends String> setupSqls = Instance.getValueForMetaPropertyToManyResolved(params.get(2), M3Properties.values, processorSupport).collect(CoreInstance::getName);
            ListIterable<? extends String> teardownSqls = Instance.getValueForMetaPropertyToManyResolved(params.get(3), M3Properties.values, processorSupport).collect(CoreInstance::getName);

            Function<ResultSet, CoreInstance> transformer = (resultSet) ->
            {
                CoreInstance pureResult = repository.newAnonymousCoreInstance(functionExpressionToUseInStack.getSourceInformation(), processorSupport.package_getByUserPath("meta::relational::metamodel::execute::ResultSet"));
                CoreInstance rowClassifier = processorSupport.package_getByUserPath("meta::relational::metamodel::execute::Row");
                Instance.addValueToProperty(pureResult, "connectionAcquisitionTimeInNanoSecond", this.repository.newIntegerCoreInstance(-1), processorSupport);
                try
                {
                    ExecuteInDb.createPureResultSetFromDatabaseResultSet(pureResult, resultSet, functionExpressionToUseInStack, rowClassifier, "UTC", repository, System.nanoTime(), 200, processorSupport);
                    return ValueSpecificationBootstrap.wrapValueSpecification(pureResult, true, processorSupport);
                }
                catch (SQLException e)
                {
                    throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e);
                }
            };

            try
            {
                return RunSqlDialectTestQueryHelper.runTestQueryAndTransformResultSet(dbType, testQuery, setupSqls, teardownSqls, transformer);
            }
            catch (SQLException e)
            {
                throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), e);
            }
        }
    }
}
