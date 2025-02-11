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
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.generated.Root_meta_pure_fct_AssertionRun;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.junit.Assert;

import static org.finos.legend.pure.generated.core_pure_test_fct.Root_meta_pure_fct_tests_testRunnerAssertionH2_Function_1__Function_1__Function_1__AssertionRun_MANY_;
import static org.junit.Assert.fail;

public class FCTTestSuitBuilder extends PureTestBuilder
{

    public static TestSuite buildFCTTestSuiteWithExecutorFunctionFromList(ImmutableList<FCTTestCollection> collection, MutableMap<String, String> exclusions, String function, boolean includeBeforeAndAfter, ExecutionSupport executionSupport)
    {
        TestSuite suite = new PureTestBuilderCompiled();
        collection.forEach(c -> suite.addTest(buildFCTSuite(c,function,c.getRuntimeFunction(), c.getSetupFunction(), includeBeforeAndAfter, executionSupport)));
        return suite;
    }

    private static Object fctExecuteFn(CoreInstance toEval, CoreInstance testParameter,  MutableMap<String, String> exclusions, ExecutionSupport executionSupport, MutableList<Object> paramList) throws Throwable
    {

        Class<?> _class = Class.forName("org.finos.legend.pure.generated." + IdBuilder.sourceToId(toEval.getSourceInformation()));

        paramList.add(testParameter);
        paramList = paramList.with(executionSupport);
        Object[] params = paramList.toArray();
        String methodName = FunctionProcessor.functionNameToJava(toEval);
        Method method = params.length == 1 ? _class.getMethod(methodName, ExecutionSupport.class)
                : ArrayIterate.detect(_class.getMethods(), m -> methodName.equals(m.getName()));

        // NOTE: mock out the global tracer for test
        // See https://github.com/opentracing/opentracing-java/issues/170
        // See https://github.com/opentracing/opentracing-java/issues/364
        GlobalTracer.registerIfAbsent(NoopTracerFactory.create());
        //TODO: Clean up error management to handle expected errors
        try
        {
            Object res = method.invoke(null, params);
            return res;
        }
        catch (Error e)
        {
            Throwable thrown = e.getCause().getMessage().contains("Unexpected error executing function with params") && e.getCause().getCause() != null ? e.getCause().getCause() : e.getCause();
            String message = "";//exclusions.get(PackageableElement.getUserPathForPackageableElement(toEval, "::"));
            if (thrown instanceof PureAssertFailException)
            {
                fail(thrown.getMessage());
            }
            throw thrown;
        }
        finally
        {
            try
            {
                // HACK since GlobalTracer api doesn't provide a way to reset the tracer which is needed for testing
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


    public static FCTTestCollection buildFCTTestCollection(String path, String runtimeFunction, String setupFunction, ProcessorSupport processorSupport)
    {

        return new FCTTestCollection(processorSupport.package_getByUserPath(path), runtimeFunction, setupFunction,processorSupport);
    }

    public static boolean isFCTTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, "meta::pure::test::fct::FCT", "test", processorSupport);
    }

    public static boolean isFCTTestCollection(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, "meta::pure::test::fct::FCT", "testCollection", processorSupport);
    }

    public static boolean isFCTAdaptor(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, "meta::pure::test::fct::FCT", "adapter", processorSupport);
    }

    public static TestSuite buildFCTSuite(FCTTestCollection testCollection,   String toEval, String runtimeFunction, String setupFunction, boolean includeBeforeAndAfter, ExecutionSupport executionSupport)
    {
        MutableList<TestSuite> subSuites = Lists.mutable.empty();
        for (FCTTestCollection collection : testCollection.getSubCollections().toSortedList(Comparator.comparing(a -> a.getPackage().getName())))
        {
            System.out.println("add subsuite");
            subSuites.add(buildFCTSuite(collection,  toEval, runtimeFunction, setupFunction, includeBeforeAndAfter, executionSupport));
        }
        System.out.println("add" + testCollection.getPackage().getName());

        return buildFCTSuite(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(testCollection.getPackage()),
                toEval,
                runtimeFunction,
                setupFunction,
                testCollection.getTestFunctions(),
                subSuites,
                executionSupport,
                includeBeforeAndAfter
        );
    }

    private static TestSuite buildFCTSuite(String packageName,
                                           String toEval,
                                           String runtimeFunction,
                                           String setupFunction,
                                           RichIterable<CoreInstance> testFunctions,
                                           ListIterable<TestSuite> subSuites,  ExecutionSupport executionSupport, boolean includeBeforeAndAfter)
    {
        PureTestBuilderCompiled suite = new PureTestBuilderCompiled();
        suite.setName(packageName);

        for (Test subSuite : subSuites.toSortedList(Comparator.comparing(TestSuite::getName)))
        {
            suite.addTest(subSuite);
        }

        for (CoreInstance testFunc : testFunctions.toSortedList(Comparator.comparing(CoreInstance::getName)))
        {

            if (includeBeforeAndAfter)
            {
                Function<?>  setup = ((Function<?>) _Package.getByUserPath(setupFunction, ((CompiledExecutionSupport) executionSupport).getProcessorSupport()));
                PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> setupExecutor = (a, b) -> fctExecuteFn(a, testFunc,org.eclipse.collections.impl.factory.Maps.mutable.empty(),executionSupport, b);
                suite.addTest(new FCTPureTestCase(setup, setupExecutor, executionSupport, Lists.mutable.empty(),"setup.getName()"));
            }

            Function<?>  runtimeFunctionInstance =  (Function<?>)  _Package.getByUserPath(runtimeFunction, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
             org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<? extends java.lang.Object>   testEval = (Function<? extends Object>) _Package.getByUserPath(toEval, ((CompiledExecutionSupport) executionSupport).getProcessorSupport());
            RichIterable<? extends Root_meta_pure_fct_AssertionRun> functions = Root_meta_pure_fct_tests_testRunnerAssertionH2_Function_1__Function_1__Function_1__AssertionRun_MANY_((Function<?>) testEval,runtimeFunctionInstance,(Function<?>) testFunc,executionSupport);


            for (Root_meta_pure_fct_AssertionRun assertionRun: functions)
                 {
                     //simplify this
                     PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) -> fctExecuteFn(a,assertionRun._parameter(),org.eclipse.collections.impl.factory.Maps.mutable.empty(),executionSupport, b);
                     Test theTest = new FCTPureTestCase(assertionRun._evalFn(),testExecutor, executionSupport,Lists.mutable.empty(),   assertionRun._parameter()._test().getName());
                     suite.addTest(theTest);

                 }

        }
        return suite;
    }

        public static class FCTPureTestCase extends TestCase
    {

        CoreInstance coreInstance;
        ExecutionSupport executionSupport;
        F2<CoreInstance, MutableList<Object>, Object> executor;
        MutableList<Object>  parameters;

        public FCTPureTestCase()
        {
        }

        FCTPureTestCase(CoreInstance coreInstance,  F2<CoreInstance, MutableList<Object>, Object> executor, ExecutionSupport executionSupport, MutableList<Object> parameters, String name)
        {
            this.coreInstance = coreInstance;
            this.executionSupport = executionSupport;
            this.executor = executor;
            this.parameters = parameters;
            this.setName(name);
        }

        @Override
        protected void runTest() throws Throwable
        {
            this.executor.value(this.coreInstance, parameters);

        }
    }



}