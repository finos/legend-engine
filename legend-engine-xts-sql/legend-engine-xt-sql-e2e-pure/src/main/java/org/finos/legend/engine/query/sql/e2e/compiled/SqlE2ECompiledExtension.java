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

package org.finos.legend.engine.query.sql.e2e.compiled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.postgres.e2e.SqlE2ERunner;
import org.finos.legend.engine.query.sql.e2e.shared.SqlE2ENativeHelper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledProcessorSupport;
import org.finos.legend.pure.runtime.java.compiled.extension.BaseCompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.SharedPureFunction;
import org.finos.legend.pure.m3.navigation.Instance;

public class SqlE2ECompiledExtension extends BaseCompiledExtension
{
    public SqlE2ECompiledExtension()
    {
        super(
                "core_external_query_sql_e2e",
                () -> Lists.fixedSize.with(new ResolveCaseRefs(), new ExecuteSQLE2ETest(), new ExecuteAdhocSQL()),
                Lists.fixedSize.with(),
                Lists.fixedSize.empty(),
                Lists.fixedSize.empty());
    }

    public static class ResolveCaseRefs extends AbstractNative
    {
        public ResolveCaseRefs()
        {
            super("resolveCaseRefs_String_1__SqlE2ECaseRef_MANY_");
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            // double cast sidesteps a generic-inference conflict javac can't resolve on its own;
            // safe because every element genuinely is a Root_..._SqlE2ECaseRef at runtime
            return "((org.eclipse.collections.api.list.MutableList<org.finos.legend.pure.generated.Root_meta_external_query_sql_e2e_SqlE2ECaseRef>) (org.eclipse.collections.api.list.MutableList<?>) "
                    + "org.eclipse.collections.impl.factory.Lists.mutable.withAll(org.finos.legend.engine.query.sql.e2e.compiled.SqlE2ECompiledExtension.resolveCaseRefs("
                    + transformedParams.get(0) + ", es)))";
        }
    }

    public static List<CoreInstance> resolveCaseRefs(String filter, ExecutionSupport es)
    {
        List<CoreInstance> out = new ArrayList<>();
        for (SqlE2ERunner.CaseRef ref : SqlE2ENativeHelper.resolveCaseRefs(filter))
        {
            out.add(buildCaseRefInstance(ref, es));
        }
        return out;
    }

    private static CoreInstance buildCaseRefInstance(SqlE2ERunner.CaseRef ref, ExecutionSupport es)
    {
        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
        CoreInstance instance = processorSupport.newCoreInstance("Anonymous_NoProfile", SqlE2ENativeHelper.SQL_E2E_CASE_REF_CLASS, null);

        Instance.setValueForProperty(instance, "corpusId", ValCoreInstance.toCoreInstance(ref.corpusId), processorSupport);
        CoreInstance pathEnumVal = ((CompiledExecutionSupport) es).getMetadata().getEnum(SqlE2ENativeHelper.SQL_E2E_PATH_ENUM, ref.path);
        Instance.setValueForProperty(instance, "path", pathEnumVal, processorSupport);
        Instance.setValueForProperty(instance, "hasOrderBy", ValCoreInstance.toCoreInstance(ref.hasOrderBy), processorSupport);

        if (ref.sql != null)
        {
            Instance.setValueForProperty(instance, "sql", ValCoreInstance.toCoreInstance(ref.sql), processorSupport);
        }
        if (ref.skip != null)
        {
            Instance.setValueForProperty(instance, "skip", ValCoreInstance.toCoreInstance(ref.skip), processorSupport);
        }
        if (ref.bugReason != null)
        {
            Instance.setValueForProperty(instance, "bugReason", ValCoreInstance.toCoreInstance(ref.bugReason), processorSupport);
        }
        if (ref.rewriteError != null)
        {
            Instance.setValueForProperty(instance, "rewriteError", ValCoreInstance.toCoreInstance(ref.rewriteError), processorSupport);
        }
        if (ref.expectedStatus != null)
        {
            Instance.setValueForProperty(instance, "expectedStatus", ValCoreInstance.toCoreInstance(ref.expectedStatus), processorSupport);
        }

        return instance;
    }

    private static CoreInstance buildConnectionInstance(ExecutionSupport es)
    {
        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
        SqlE2ERunner.ConnectionInfo info = SqlE2ENativeHelper.connectionInfo();

        CoreInstance instance = processorSupport.newCoreInstance("Anonymous_NoProfile", SqlE2ENativeHelper.SQL_E2E_CONNECTION_CLASS, null);
        Instance.setValueForProperty(instance, "host", ValCoreInstance.toCoreInstance(info.host), processorSupport);
        Instance.setValueForProperty(instance, "port", ValCoreInstance.toCoreInstance(info.port), processorSupport);
        Instance.setValueForProperty(instance, "databaseName", ValCoreInstance.toCoreInstance(info.database), processorSupport);
        Instance.setValueForProperty(instance, "user", ValCoreInstance.toCoreInstance(info.user), processorSupport);
        Instance.setValueForProperty(instance, "password", ValCoreInstance.toCoreInstance(info.password), processorSupport);
        return instance;
    }

    public static class ExecuteSQLE2ETest extends AbstractNative
    {
        public ExecuteSQLE2ETest()
        {
            super("executeSQLE2ETest_String_1__SqlE2EPath_1__SqlE2ETestResult_1_");
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            return "((org.finos.legend.pure.generated.Root_meta_external_query_sql_e2e_SqlE2ETestResult) org.finos.legend.engine.query.sql.e2e.compiled.SqlE2ECompiledExtension.executeSQLE2ETest("
                    + transformedParams.get(0) + ", " + transformedParams.get(1) + ", es))";
        }
    }

    /**
     * Resolves the case ref for (corpusId, path); short-circuits skip/bug/rewrite-error cases without
     * invoking runOneCase, otherwise invokes it and classifies the outcome (or any throw).
     */
    public static Object executeSQLE2ETest(String corpusId, CoreInstance pathEnum, ExecutionSupport es)
    {
        long start = System.nanoTime();
        String pathName = pathEnum.getName();
        SqlE2ERunner.CaseRef resolved = SqlE2ENativeHelper.resolveCaseRef(corpusId, pathName);
        SqlE2ENativeHelper.Classification classification = SqlE2ENativeHelper.classifyShortCircuit(resolved);
        if (classification == null)
        {
            try
            {
                CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
                CoreInstance runOneCaseFn = processorSupport.package_getByUserPath(SqlE2ENativeHelper.RUN_ONE_CASE);
                if (runOneCaseFn == null)
                {
                    throw new IllegalStateException("Cannot resolve '" + SqlE2ENativeHelper.RUN_ONE_CASE
                            + "' - did runOneCase change signature?");
                }
                SharedPureFunction<String> fn = CoreGen.getSharedPureFunction((Function<?>) runOneCaseFn, es);

                CoreInstance refInstance = buildCaseRefInstance(resolved, es);
                CoreInstance connInstance = buildConnectionInstance(es);

                String raw = fn.execute(Lists.mutable.with(refInstance, connInstance), es);
                classification = SqlE2ENativeHelper.classifySuccess(corpusId, resolved.hasOrderBy, raw);
            }
            catch (Throwable t)
            {
                classification = SqlE2ENativeHelper.classifyThrow(t);
            }
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        return buildCompiledTestResult(corpusId + "|" + pathName, classification.status, elapsedMs, classification.message, es);
    }

    private static Object buildCompiledTestResult(String id, String status, long elapsedMs, String message, ExecutionSupport es)
    {
        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
        CoreInstance testResult = processorSupport.newCoreInstance("Anonymous_NoProfile", SqlE2ENativeHelper.SQL_E2E_TEST_RESULT_CLASS, null);

        Instance.setValueForProperty(testResult, "id", ValCoreInstance.toCoreInstance(id), processorSupport);
        Instance.setValueForProperty(testResult, "elapsed", ValCoreInstance.toCoreInstance(elapsedMs), processorSupport);

        CoreInstance enumVal = ((CompiledExecutionSupport) es).getMetadata().getEnum(SqlE2ENativeHelper.SQL_E2E_TEST_STATUS_ENUM, status);
        Instance.setValueForProperty(testResult, "status", enumVal, processorSupport);

        if (message != null)
        {
            Instance.setValueForProperty(testResult, "message", ValCoreInstance.toCoreInstance(message), processorSupport);
        }

        return testResult;
    }

    public static class ExecuteAdhocSQL extends AbstractNative
    {
        public ExecuteAdhocSQL()
        {
            super("executeAdhocSQL_String_1__SqlE2EPath_1__SqlE2EAdhocResult_MANY_");
        }

        @Override
        public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
        {
            // see ResolveCaseRefs.build() above for why the double cast is needed
            return "((org.eclipse.collections.api.list.MutableList<org.finos.legend.pure.generated.Root_meta_external_query_sql_e2e_SqlE2EAdhocResult>) (org.eclipse.collections.api.list.MutableList<?>) "
                    + "org.eclipse.collections.impl.factory.Lists.mutable.withAll(org.finos.legend.engine.query.sql.e2e.compiled.SqlE2ECompiledExtension.executeAdhocSQL("
                    + transformedParams.get(0) + ", " + transformedParams.get(1) + ", es)))";
        }
    }

    /** Runs sql against Legend for each requested path (Both means TDS and Relation both run). */
    public static List<CoreInstance> executeAdhocSQL(String sql, CoreInstance pathEnum, ExecutionSupport es)
    {
        String pathName = pathEnum.getName();
        List<String> paths = "Both".equals(pathName) ? Arrays.asList("TDS", "Relation") : Collections.singletonList(pathName);

        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
        CoreInstance runOneAdhocLegendFn = processorSupport.package_getByUserPath(SqlE2ENativeHelper.RUN_ONE_ADHOC_LEGEND);
        if (runOneAdhocLegendFn == null)
        {
            throw new IllegalStateException("Cannot resolve '" + SqlE2ENativeHelper.RUN_ONE_ADHOC_LEGEND
                    + "' - did runOneAdhocLegend change signature?");
        }
        SharedPureFunction<String> fn = CoreGen.getSharedPureFunction((Function<?>) runOneAdhocLegendFn, es);
        CoreInstance connInstance = buildConnectionInstance(es);

        List<CoreInstance> out = new ArrayList<>();
        for (String pathValue : paths)
        {
            SqlE2ERunner.AdhocPrep prep = SqlE2ENativeHelper.prepareAdhoc(sql, pathValue);

            String legendResultJson = null;
            String legendError = null;
            if (prep.rewrittenSql != null)
            {
                try
                {
                    legendResultJson = fn.execute(Lists.mutable.with(prep.rewrittenSql, prep.prefix, connInstance), es);
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

            out.add(buildCompiledAdhocResult(pathValue, legendResultJson, legendError, referenceRows, prep.referenceError, matched, es));
        }
        return out;
    }

    private static CoreInstance buildCompiledAdhocResult(String pathName, String legendResultJson, String legendError,
            String referenceRows, String referenceError, Boolean matched, ExecutionSupport es)
    {
        CompiledProcessorSupport processorSupport = ((CompiledExecutionSupport) es).getProcessorSupport();
        CoreInstance instance = processorSupport.newCoreInstance("Anonymous_NoProfile", SqlE2ENativeHelper.SQL_E2E_ADHOC_RESULT_CLASS, null);

        CoreInstance pathEnumVal = ((CompiledExecutionSupport) es).getMetadata().getEnum(SqlE2ENativeHelper.SQL_E2E_PATH_ENUM, pathName);
        Instance.setValueForProperty(instance, "path", pathEnumVal, processorSupport);

        if (legendResultJson != null)
        {
            Instance.setValueForProperty(instance, "legendResultJson", ValCoreInstance.toCoreInstance(legendResultJson), processorSupport);
        }
        if (legendError != null)
        {
            Instance.setValueForProperty(instance, "legendError", ValCoreInstance.toCoreInstance(legendError), processorSupport);
        }
        if (referenceRows != null)
        {
            Instance.setValueForProperty(instance, "referenceRows", ValCoreInstance.toCoreInstance(referenceRows), processorSupport);
        }
        if (referenceError != null)
        {
            Instance.setValueForProperty(instance, "referenceError", ValCoreInstance.toCoreInstance(referenceError), processorSupport);
        }
        if (matched != null)
        {
            Instance.setValueForProperty(instance, "matched", ValCoreInstance.toCoreInstance(matched), processorSupport);
        }

        return instance;
    }
}
