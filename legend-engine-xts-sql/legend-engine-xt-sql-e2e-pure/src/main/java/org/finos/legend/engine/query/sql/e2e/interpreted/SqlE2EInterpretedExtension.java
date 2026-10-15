// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.query.sql.e2e.interpreted;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.postgres.e2e.SqlE2ERunner;
import org.finos.legend.engine.query.sql.e2e.shared.SqlE2ENativeHelper;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation.enumeration.Enumeration;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.extension.BaseInterpretedExtension;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class SqlE2EInterpretedExtension extends BaseInterpretedExtension
{
    public SqlE2EInterpretedExtension()
    {
        super(Lists.fixedSize.with(
                Tuples.pair("resolveCaseRefs_String_1__SqlE2ECaseRef_MANY_", ResolveCaseRefs::new),
                Tuples.pair("executeSQLE2ETest_String_1__SqlE2EPath_1__SqlE2ETestResult_1_", ExecuteSQLE2ETest::new),
                Tuples.pair("executeAdhocSQL_String_1__SqlE2EPath_1__SqlE2EAdhocResult_MANY_", ExecuteAdhocSQL::new)
        ));
    }

    public static class ResolveCaseRefs extends NativeFunction
    {
        private final ModelRepository repository;

        public ResolveCaseRefs(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
        {
            this.repository = repository;
        }

        @Override
        public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
        {
            String filter = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport).getName();

            MutableList<CoreInstance> refs = Lists.mutable.empty();
            for (SqlE2ERunner.CaseRef ref : SqlE2ENativeHelper.resolveCaseRefs(filter))
            {
                refs.add(buildCaseRefInstance(ref, this.repository, functionExpressionCallStack, processorSupport));
            }
            return ValueSpecificationBootstrap.wrapValueSpecification(refs, true, processorSupport);
        }
    }

    private static CoreInstance buildCaseRefInstance(SqlE2ERunner.CaseRef ref, ModelRepository repository, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance caseRefClass = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_CASE_REF_CLASS);
        CoreInstance pathEnumType = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_PATH_ENUM);

        CoreInstance instance = repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), caseRefClass);
        Instance.setValueForProperty(instance, "corpusId", repository.newStringCoreInstance(ref.corpusId), processorSupport);
        Instance.setValueForProperty(instance, "path", Enumeration.findEnum(pathEnumType, ref.path), processorSupport);
        Instance.setValueForProperty(instance, "hasOrderBy", repository.newBooleanCoreInstance(ref.hasOrderBy), processorSupport);

        if (ref.sql != null)
        {
            Instance.setValueForProperty(instance, "sql", repository.newStringCoreInstance(ref.sql), processorSupport);
        }
        if (ref.skip != null)
        {
            Instance.setValueForProperty(instance, "skip", repository.newStringCoreInstance(ref.skip), processorSupport);
        }
        if (ref.bugReason != null)
        {
            Instance.setValueForProperty(instance, "bugReason", repository.newStringCoreInstance(ref.bugReason), processorSupport);
        }
        if (ref.rewriteError != null)
        {
            Instance.setValueForProperty(instance, "rewriteError", repository.newStringCoreInstance(ref.rewriteError), processorSupport);
        }
        if (ref.expectedStatus != null)
        {
            Instance.setValueForProperty(instance, "expectedStatus", repository.newStringCoreInstance(ref.expectedStatus), processorSupport);
        }

        return instance;
    }

    private static CoreInstance buildConnectionInstance(ModelRepository repository, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
    {
        CoreInstance connectionClass = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_CONNECTION_CLASS);
        SqlE2ERunner.ConnectionInfo info = SqlE2ENativeHelper.connectionInfo();

        CoreInstance instance = repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), connectionClass);
        Instance.setValueForProperty(instance, "host", repository.newStringCoreInstance(info.host), processorSupport);
        Instance.setValueForProperty(instance, "port", repository.newIntegerCoreInstance(info.port), processorSupport);
        Instance.setValueForProperty(instance, "databaseName", repository.newStringCoreInstance(info.database), processorSupport);
        Instance.setValueForProperty(instance, "user", repository.newStringCoreInstance(info.user), processorSupport);
        Instance.setValueForProperty(instance, "password", repository.newStringCoreInstance(info.password), processorSupport);
        return instance;
    }

    /**
     * Resolves the case ref for (corpusId, path); short-circuits skip/bug/rewrite-error cases without
     * invoking runOneCase, otherwise invokes it and classifies the outcome (or any throw).
     */
    public static class ExecuteSQLE2ETest extends NativeFunction
    {
        private final FunctionExecutionInterpreted functionExecution;
        private final ModelRepository repository;

        public ExecuteSQLE2ETest(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
        {
            this.functionExecution = functionExecution;
            this.repository = repository;
        }

        @Override
        public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
        {
            String corpusId = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
            CoreInstance pathEnum = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
            String pathName = pathEnum.getName();

            long start = System.nanoTime();
            SqlE2ERunner.CaseRef resolved = SqlE2ENativeHelper.resolveCaseRef(corpusId, pathName);
            SqlE2ENativeHelper.Classification classification = SqlE2ENativeHelper.classifyShortCircuit(resolved);
            if (classification == null)
            {
                try
                {
                    CoreInstance runOneCaseFn = processorSupport.package_getByUserPath(SqlE2ENativeHelper.RUN_ONE_CASE);
                    if (runOneCaseFn == null)
                    {
                        throw new IllegalStateException("Cannot resolve '" + SqlE2ENativeHelper.RUN_ONE_CASE
                                + "' - did runOneCase change signature?");
                    }

                    CoreInstance refInstance = buildCaseRefInstance(resolved, this.repository, functionExpressionCallStack, processorSupport);
                    CoreInstance connInstance = buildConnectionInstance(this.repository, functionExpressionCallStack, processorSupport);

                    MutableList<CoreInstance> fnParams = Lists.mutable.with(
                            ValueSpecificationBootstrap.wrapValueSpecification(refInstance, true, processorSupport),
                            ValueSpecificationBootstrap.wrapValueSpecification(connInstance, true, processorSupport));

                    CoreInstance raw = this.functionExecution.executeFunction(
                            false,
                            FunctionCoreInstanceWrapper.toFunction(runOneCaseFn),
                            fnParams,
                            resolvedTypeParameters,
                            resolvedMultiplicityParameters,
                            getParentOrEmptyVariableContext(variableContext),
                            functionExpressionCallStack,
                            profiler,
                            instantiationContext,
                            executionSupport);
                    CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(raw, M3Properties.values, processorSupport);
                    String rawResult = PrimitiveUtilities.getStringValue(value);
                    classification = SqlE2ENativeHelper.classifySuccess(corpusId, resolved.hasOrderBy, rawResult);
                }
                catch (Throwable t)
                {
                    classification = SqlE2ENativeHelper.classifyThrow(t);
                }
            }
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;

            String id = corpusId + "|" + pathName;
            return buildTestResult(id, classification.status, elapsedMs, classification.message, functionExpressionCallStack, processorSupport);
        }

        private CoreInstance buildTestResult(String id, String statusName, long elapsedMs, String message, MutableStack<CoreInstance> functionExpressionCallStack, ProcessorSupport processorSupport)
        {
            CoreInstance testResultClass = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_TEST_RESULT_CLASS);
            CoreInstance testStatusEnum = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_TEST_STATUS_ENUM);

            CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), testResultClass);

            Instance.setValueForProperty(instance, "id", this.repository.newStringCoreInstance(id), processorSupport);

            CoreInstance enumValue = Enumeration.findEnum(testStatusEnum, statusName);
            Instance.setValueForProperty(instance, "status", enumValue, processorSupport);
            Instance.setValueForProperty(instance, "elapsed", this.repository.newIntegerCoreInstance(elapsedMs), processorSupport);

            if (message != null)
            {
                Instance.setValueForProperty(instance, "message", this.repository.newStringCoreInstance(message), processorSupport);
            }

            return ValueSpecificationBootstrap.wrapValueSpecification(instance, true, processorSupport);
        }
    }

    /** Runs sql against Legend for each requested path (Both means TDS and Relation both run). */
    public static class ExecuteAdhocSQL extends NativeFunction
    {
        private final FunctionExecutionInterpreted functionExecution;
        private final ModelRepository repository;

        public ExecuteAdhocSQL(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
        {
            this.functionExecution = functionExecution;
            this.repository = repository;
        }

        @Override
        public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
        {
            String sql = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport));
            CoreInstance pathEnum = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
            String pathName = pathEnum.getName();
            List<String> paths = "Both".equals(pathName) ? Arrays.asList("TDS", "Relation") : Collections.singletonList(pathName);

            CoreInstance runOneAdhocLegendFn = processorSupport.package_getByUserPath(SqlE2ENativeHelper.RUN_ONE_ADHOC_LEGEND);
            if (runOneAdhocLegendFn == null)
            {
                throw new IllegalStateException("Cannot resolve '" + SqlE2ENativeHelper.RUN_ONE_ADHOC_LEGEND
                        + "' - did runOneAdhocLegend change signature?");
            }
            CoreInstance adhocResultClass = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_ADHOC_RESULT_CLASS);
            CoreInstance pathEnumType = processorSupport.package_getByUserPath(SqlE2ENativeHelper.SQL_E2E_PATH_ENUM);
            CoreInstance connInstance = buildConnectionInstance(this.repository, functionExpressionCallStack, processorSupport);

            MutableList<CoreInstance> out = Lists.mutable.empty();
            for (String pathValue : paths)
            {
                SqlE2ERunner.AdhocPrep prep = SqlE2ENativeHelper.prepareAdhoc(sql, pathValue);

                String legendResultJson = null;
                String legendError = null;
                if (prep.rewrittenSql != null)
                {
                    try
                    {
                        MutableList<CoreInstance> fnParams = Lists.mutable.with(
                                ValueSpecificationBootstrap.wrapValueSpecification(this.repository.newStringCoreInstance(prep.rewrittenSql), true, processorSupport),
                                ValueSpecificationBootstrap.wrapValueSpecification(this.repository.newStringCoreInstance(prep.prefix), true, processorSupport),
                                ValueSpecificationBootstrap.wrapValueSpecification(connInstance, true, processorSupport));

                        CoreInstance raw = this.functionExecution.executeFunction(
                                false,
                                FunctionCoreInstanceWrapper.toFunction(runOneAdhocLegendFn),
                                fnParams,
                                resolvedTypeParameters,
                                resolvedMultiplicityParameters,
                                getParentOrEmptyVariableContext(variableContext),
                                functionExpressionCallStack,
                                profiler,
                                instantiationContext,
                                executionSupport);
                        CoreInstance value = Instance.getValueForMetaPropertyToOneResolved(raw, M3Properties.values, processorSupport);
                        legendResultJson = PrimitiveUtilities.getStringValue(value);
                    }
                    catch (Throwable t)
                    {
                        legendError = SqlE2ENativeHelper.unwrapThrowMessage(t);
                    }
                }
                else
                {
                    legendError = "rewrite error: " + prep.rewriteError;
                }

                Boolean matched = SqlE2ENativeHelper.matchAgainstReference(legendResultJson, prep.reference, prep.hasOrderBy);
                String referenceRows = prep.reference != null ? prep.reference.toString() : null;

                CoreInstance instance = this.repository.newEphemeralAnonymousCoreInstance(functionExpressionCallStack.peek().getSourceInformation(), adhocResultClass);
                Instance.setValueForProperty(instance, "path", Enumeration.findEnum(pathEnumType, pathValue), processorSupport);
                if (legendResultJson != null)
                {
                    Instance.setValueForProperty(instance, "legendResultJson", this.repository.newStringCoreInstance(legendResultJson), processorSupport);
                }
                if (legendError != null)
                {
                    Instance.setValueForProperty(instance, "legendError", this.repository.newStringCoreInstance(legendError), processorSupport);
                }
                if (referenceRows != null)
                {
                    Instance.setValueForProperty(instance, "referenceRows", this.repository.newStringCoreInstance(referenceRows), processorSupport);
                }
                if (prep.referenceError != null)
                {
                    Instance.setValueForProperty(instance, "referenceError", this.repository.newStringCoreInstance(prep.referenceError), processorSupport);
                }
                if (matched != null)
                {
                    Instance.setValueForProperty(instance, "matched", this.repository.newBooleanCoreInstance(matched), processorSupport);
                }
                out.add(instance);
            }
            return ValueSpecificationBootstrap.wrapValueSpecification(out, true, processorSupport);
        }
    }
}
