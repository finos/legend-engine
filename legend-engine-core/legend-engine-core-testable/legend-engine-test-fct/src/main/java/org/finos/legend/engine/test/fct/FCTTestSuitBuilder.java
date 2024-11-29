/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.engine.test.fct;

import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.util.GlobalTracer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Comparator;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.junit.Assert;
import static org.finos.legend.pure.m3.execution.test.TestCollection.collectTests;
import static org.junit.Assert.fail;

public class FCTTestSuitBuilder extends PureTestBuilder
{
    public static String LINEAGE_FUNCTION = "meta::analytics::lineage::computeTestLineage_FunctionDefinition_1__TestParameters_1__LineageResult_1_";
    public static String EXECUTE_FUNCTION = "meta::pure::test::fct::executeWrapper_FunctionDefinition_1__TestParameters_1__ExecuteResult_1_";

    public static TestSuite buildFCTTestSuiteWithExecutorFunction(TestCollection collection,  MutableMap<String, String> exclusions, String function, boolean includeBeforeAndAfter, ExecutionSupport executionSupport)
    {
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) -> fctExecuteFn(a, function, exclusions, executionSupport, b);
        TestSuite suite = new TestSuite();
        suite.addTest(buildFCTSuite(collection, testExecutor,includeBeforeAndAfter, executionSupport));
        return suite;
    }

    public static void addCollectionToSuite(TestSuite suite, TestCollection collection, MutableMap<String, String> exclusions, String function, boolean includeBeforeAndAfter, ExecutionSupport executionSupport)
    {
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) -> fctExecuteFn(a, function, exclusions, executionSupport, b);
        suite.addTest(buildFCTSuite(collection, testExecutor, includeBeforeAndAfter,executionSupport));
    }

    private static Object fctExecuteFn(CoreInstance coreInstance, String FCTexecutor, MutableMap<String, String> exclusions, ExecutionSupport executionSupport, MutableList<Object> paramList) throws Throwable
    {
        Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(coreInstance.getSourceInformation()));
        if (isFCTTest(coreInstance, ((CompiledExecutionSupport) executionSupport).getProcessorSupport()))
        {
            CoreInstance executor = _Package.getByUserPath(FCTexecutor, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
            if (executor == null)
            {
                throw new RuntimeException(FCTexecutor + "can't be found in the graph");
            }
            paramList.add(executor);
        }
        paramList = paramList.with(executionSupport);
        Object[] params = paramList.toArray();
        String methodName = FunctionProcessor.functionNameToJava(coreInstance);
        Method method = params.length == 1 ? _class.getMethod(methodName, ExecutionSupport.class)
                : ArrayIterate.detect(_class.getMethods(), m -> methodName.equals(m.getName()));

        // NOTE: mock out the global tracer for test
        // See https://github.com/opentracing/opentracing-java/issues/170
        // See https://github.com/opentracing/opentracing-java/issues/364
        GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
        try
        {
            Object res = method.invoke(null, params);
            // Ensure we didn't expect an error
            String message = exclusions.get(PackageableElement.getUserPathForPackageableElement(coreInstance, "::"));
            if (message != null)
            {
                PCTTools.displayExpectedErrorFailMessage(message, coreInstance, FCTexecutor);
            }
            return res;
        }
        catch (InvocationTargetException e)
        {
            // Check if the error was expected
            String message = exclusions.get(PackageableElement.getUserPathForPackageableElement(coreInstance, "::"));
            Throwable thrown = e.getCause().getMessage().contains("Unexpected error executing function with params") && e.getCause().getCause() != null ? e.getCause().getCause() : e.getCause();
            if (message != null && thrown.getMessage().contains(message))
            {
                return null;
            }
            else
            {
                PCTTools.displayErrorMessage(message, coreInstance, FCTexecutor, ((CompiledExecutionSupport) executionSupport).getProcessorSupport(), thrown);
                if (thrown instanceof PureAssertFailException)
                {
                    fail(thrown.getMessage());
                }
                throw thrown;
            }
        }
        finally
        {
            try
            {
                // HACK since GlobalTracer api doesnt provide a way to reset the tracer which is needed for testing
                Field tracerField = GlobalTracer.get().getClass().getDeclaredField("isRegistered");
                tracerField.setAccessible(true);
                tracerField.set(GlobalTracer.get(), false);
                Assert.assertFalse(GlobalTracer.isRegistered());
            }
            catch (Exception ignored)
            {
            }
        }
    }


    public static TestCollection buildFCTTestCollection(String path, ProcessorSupport processorSupport)
    {
        return collectTests(path, processorSupport, (node) -> isFCTTest(node, processorSupport));
    }

    public static boolean isFCTTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, "meta::pure::test::fct::FCT", "test", processorSupport);
    }

    public static TestSuite buildFCTSuite(TestCollection testCollection, F2<CoreInstance, MutableList<Object>, Object> executor, boolean includeBeforeAndAfter, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = Lists.mutable.empty();
        for (TestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            subSuites.add(buildSuite(collection, executor, executionSupport));
        }
        return buildFCTSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(testCollection.getPackage()),
                testCollection.getBeforeFunctions(),
                testCollection.getAfterFunctions(),
                testCollection.getPureAndAlloyOnlyFunctions(),
                testCollection.getTestFunctionParam(),
                testCollection.getTestFunctionParamCustomizer(),
                testCollection.getTestParameterizationId(),
                subSuites,
                executor,
                executionSupport,
                includeBeforeAndAfter
        );
    }

    private static TestSuite buildFCTSuite(String packageName, RichIterable<CoreInstance> beforeFunctions, RichIterable<CoreInstance> afterFunctions,
                                           RichIterable<CoreInstance> testFunctions, Object param, CoreInstance paramCustomizer, String parameterizationId,
                                           ListIterable<TestSuite> subSuites, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport, boolean includeBeforeAndAfter)
    {
        TestSuite suite = new TestSuite();
        suite.setName(packageName + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
        if (includeBeforeAndAfter)
        {
            beforeFunctions.collect(fn -> new FCTPureTestCase(fn, param, paramCustomizer, parameterizationId, executor, executionSupport)).each(suite::addTest);
        }
        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }
        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {
            Test theTest = new FCTPureTestCase(testFunc, param, paramCustomizer, parameterizationId, executor, executionSupport);
            suite.addTest(theTest);
        }

        if (includeBeforeAndAfter)
        {
            afterFunctions.collect(fn -> new FCTPureTestCase(fn, param, paramCustomizer, parameterizationId, executor, executionSupport)).each(suite::addTest);
        }
        return suite;
    }

    public static class FCTPureTestCase extends TestCase
    {

        CoreInstance coreInstance;
        Object param;
        CoreInstance paramCustomizer;
        ExecutionSupport executionSupport;
        F2<CoreInstance, MutableList<Object>, Object> executor;

        public FCTPureTestCase()
        {
        }

        FCTPureTestCase(CoreInstance coreInstance, Object param, CoreInstance paramCustomizer, String parameterizationId, F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport)
        {
            super(coreInstance.getValueForMetaPropertyToOne("functionName").getName() + (parameterizationId == null ? "" : "[" + parameterizationId + "]"));
            this.coreInstance = coreInstance;
            this.param = param;
            this.paramCustomizer = paramCustomizer;
            this.executionSupport = executionSupport;
            this.executor = executor;
        }

        @Override
        protected void runTest() throws Throwable
        {
            Object customizedParam = this.param;
            if (this.param != null && this.paramCustomizer != null)
            {
                customizedParam = this.executor.value(this.paramCustomizer, Lists.mutable.with(this.coreInstance, this.param));
            }
            if (customizedParam != null)
            {
                this.executor.value(this.coreInstance, Lists.mutable.with(customizedParam));
            }
            else
            {
                this.executor.value(this.coreInstance, Lists.mutable.empty());
            }
        }
    }



}