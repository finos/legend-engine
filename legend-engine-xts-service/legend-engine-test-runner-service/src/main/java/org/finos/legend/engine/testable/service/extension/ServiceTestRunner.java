//  Copyright 2022 Goldman Sachs
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


package org.finos.legend.engine.testable.service.extension;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.generation.ServicePlanGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.AbstractTestSuiteSessionWithResources;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.finos.legend.engine.testable.helper.TestResultHelper;
import org.finos.legend.engine.testable.helper.TestReturnTypeHelper;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureMultiExecution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceTestRunner implements TestRunner
{
    private final Root_meta_legend_service_metamodel_Service pureService;

    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor planExecutor;

    private final String pureVersion;

    public ServiceTestRunner(Root_meta_legend_service_metamodel_Service pureService, String pureVersion)
    {
        this.pureService = pureService;
        this.planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        this.pureVersion = pureVersion;
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Service Test should be executed in context of Service Test Suite only");
    }

    /**
     * Open a per-suite session that caches the execution plan and any
     * runtime closeables across {@link TestSuiteSession#runAtomicTest}
     * calls. Dispatches by execution type:
     * <ul>
     *   <li>{@link PureSingleExecution}: one plan, shared by every atomic test in the suite.</li>
     *   <li>{@link PureMultiExecution} with {@code executionParameters}: one plan per env key,
     *       built for every key that any test in the suite could use; each atomic test runs
     *       once per applicable env and returns a {@link MultiExecutionServiceTestResult}.</li>
     *   <li>{@link PureMultiExecution} with executionEnvironment reference: one composite plan
     *       generated for the keys resolved from every test's parameters; each atomic test
     *       picks its own plan from the composite.</li>
     * </ul>
     */
    @Override
    public TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        Service protocolService = ListIterate.detect(data.getElementsOfType(Service.class), ele -> ele.getPath().equals(HelperModelBuilder.getElementFullPath(this.pureService, pureModel.getExecutionSupport())));
        if (protocolService == null)
        {
            throw new IllegalArgumentException("Service not found: " + HelperModelBuilder.getElementFullPath(this.pureService, pureModel.getExecutionSupport()));
        }
        ServiceTestSuite protocolSuite = ListIterate.detect(protocolService.testSuites, ts -> ts.id.equals(testSuite._id()));
        if (protocolSuite == null)
        {
            throw new IllegalArgumentException("Test suite '" + testSuite._id() + "' not found in service");
        }
        if (protocolService.execution instanceof PureSingleExecution)
        {
            return new SingleExecutionTestSuiteSession(testSuite, protocolSuite, pureModel, data, (PureSingleExecution) protocolService.execution);
        }
        if (protocolService.execution instanceof PureMultiExecution)
        {
            PureMultiExecution multiExecution = (PureMultiExecution) protocolService.execution;
            if (multiExecution.executionParameters != null && !multiExecution.executionParameters.isEmpty())
            {
                return new MultiExecutionParametersTestSuiteSession(testSuite, protocolSuite, pureModel, data, multiExecution);
            }
            return new MultiExecutionEnvironmentTestSuiteSession(testSuite, protocolSuite, pureModel, data, multiExecution);
        }
        throw new UnsupportedOperationException("Execution type : " + protocolService.execution.getClass().getSimpleName() + " not supported with ServiceTestRunner");
    }

    private TestConnectionBuildParameters computeHints(PureModel pureModel)
    {
        try
        {
            if (this.pureService._execution() instanceof Root_meta_legend_service_metamodel_PureExecution)
            {
                Root_meta_legend_service_metamodel_PureExecution pureExecution = (Root_meta_legend_service_metamodel_PureExecution) this.pureService._execution();
                if (TestReturnTypeHelper.isRelationReturnType(pureExecution._func(), pureModel))
                {
                    return TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();
                }
            }
        }
        catch (Exception ignored)
        {
            // If we can't determine the return type, default to H2
        }
        return TestConnectionBuildParameters.NONE;
    }

    private Map<String, String> getAllValidKeysForExecEnv(String execKey, ServiceTestSuite suite, List<String> testIds)
    {
        Map<String, String> testWithKey = Maps.mutable.empty();
        for (Test test : suite.tests)
        {
            if (testIds.contains(test.id))
            {
                ServiceTest serviceTest = (ServiceTest) test;
                if (serviceTest.parameters != null)
                {
                    Optional<ParameterValue> keyValue = serviceTest.parameters.stream()
                            .filter(parameterValue -> parameterValue.name.equals(execKey)).findFirst();
                    if (keyValue.isPresent())
                    {
                        testWithKey.put(test.id, keyValue.get().value.accept(new PrimitiveValueSpecificationToObjectVisitor()).toString());
                    }
                    else
                    {
                        throw new EngineException("Please provide a value for the key mentioned as part of the execution environment for test: " + test.id);
                    }
                }
            }
        }
        return testWithKey;
    }

    private TestResult executeServiceTest(ServiceTest serviceTest, SingleExecutionPlan executionPlan)
    {
        SerializationFormat testSerializationFormat = getSerializationFormatForTest(serviceTest);

        try (Scope scope = GlobalTracer.get().buildSpan("Test status for: " + serviceTest.id).startActive(true))
        {
            Span span = scope.span();
            Map<String, Object> parameters = Maps.mutable.empty();
            if (serviceTest.parameters != null)
            {
                for (ParameterValue parameterValue : serviceTest.parameters)
                {
                    parameters.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                }
            }

            Result result = this.planExecutor.execute(executionPlan, parameters);

            boolean isResultReusable = executionPlan.rootExecutionNode.isResultPrimitiveType();
            if (isResultReusable && result instanceof StreamingResult)
            {
                result = new ConstantResult(((StreamingResult) result).flush(((StreamingResult) result).getSerializer(testSerializationFormat)));
            }

            List<AssertionStatus> assertionStatusList = Lists.mutable.empty();
            for (TestAssertion assertion : serviceTest.assertions)
            {
                AssertionStatus status = assertion.accept(new TestAssertionEvaluator(result, testSerializationFormat));
                if (status == null)
                {
                    throw new RuntimeException("Can't evaluate the test assertion: '" + assertion.id + "'");
                }
                assertionStatusList.add(status);
                if (!isResultReusable)
                {
                    result = this.planExecutor.execute(executionPlan, parameters);
                }

                if (status instanceof EqualToJsonAssertFail)
                {
                    span.log("ASSERTION: " + assertion.id + " FAILED");
                    span.log("EXPECTED: " + ((EqualToJsonAssertFail) status).expected);
                    span.log("ACTUAL: " + ((EqualToJsonAssertFail) status).actual);
                }
                else if (status instanceof AssertFail)
                {
                    span.log("ASSERTION: " + assertion.id + " FAILED");
                    span.log("MESSAGE: " + ((AssertFail) status).message);
                }
                else
                {
                    span.log("ASSERTION: " + assertion.id + " PASSED");
                }
            }

            TestExecuted testResult = new TestExecuted(assertionStatusList);
            testResult.atomicTestId = serviceTest.id;

            return testResult;
        }
        catch (Exception e)
        {
            return TestResultHelper.newTestError(serviceTest.id, e);
        }
    }

    private static SerializationFormat getSerializationFormatForTest(ServiceTest serviceTest)
    {
        if (serviceTest.serializationFormat == null)
        {
            return SerializationFormat.defaultFormat;
        }
        try
        {
            return SerializationFormat.valueOf(serviceTest.serializationFormat);
        }
        catch (IllegalArgumentException exception)
        {
            throw new UnsupportedOperationException("Unsupported serialization format '" + serviceTest.serializationFormat + "'. Supported formats are:" + Stream.of(SerializationFormat.values()).map(SerializationFormat::name).collect(Collectors.joining(",")));
        }
    }

    private static PureSingleExecution shallowCopySingleExecution(PureSingleExecution pureSingleExecution)
    {
        PureSingleExecution shallowCopy = new PureSingleExecution();
        shallowCopy.func = pureSingleExecution.func;
        shallowCopy.mapping = pureSingleExecution.mapping;
        shallowCopy.runtime = pureSingleExecution.runtime;
        return shallowCopy;
    }

    private static PureMultiExecution shallowCopyMultiExecution(PureMultiExecution pureMultiExecution)
    {
        PureMultiExecution shallowCopy = new PureMultiExecution();
        shallowCopy.func = pureMultiExecution.func;
        shallowCopy.executionKey = pureMultiExecution.executionKey;
        return shallowCopy;
    }

    private String testablePath(PureModel pureModel)
    {
        return HelperModelBuilder.getElementFullPath(this.pureService, pureModel.getExecutionSupport());
    }

    private abstract class ServiceTestSuiteSession extends AbstractTestSuiteSessionWithResources<ServiceTestSuite, ServiceTest, TestResult>
    {
        private ServiceTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, ServiceTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, ServiceTestSuiteSession::getTestSuiteTests, ServiceTestSuiteSession::getAtomicTestId);
        }

        @Override
        protected TestResult buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestError(testablePath(this.pureModel), getTestSuiteId(), atomicTestId, t);
        }

        protected TestResult runSingleExecTest(ServiceTest atomicTest, SingleExecutionPlan executionPlan)
        {
            TestResult testResult = executeServiceTest(atomicTest, executionPlan);
            testResult.testable = testablePath(this.pureModel);
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }
    }

    /**
     * Lift the runtime build, plan generation, and plan compile into a
     * session that lives across atomic-test invocations.
     * {@link #runAtomicTest} becomes a lookup of the protocol test by id
     * plus the existing {@code executeServiceTest} call against the cached
     * plan.
     */
    private class SingleExecutionTestSuiteSession extends ServiceTestSuiteSession
    {
        private final PureSingleExecution execution;
        private SingleExecutionPlan plan;

        SingleExecutionTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, ServiceTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd, PureSingleExecution execution)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd);
            this.execution = execution;
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer) throws Exception
        {
            TestConnectionBuildParameters hints = computeHints(this.pureModel);
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport()));
            MutableList<PlanTransformer> planTransformers = ServiceTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
            PureSingleExecution testPureSingleExecution = shallowCopySingleExecution(this.execution);
            if (this.execution.runtime != null)
            {
                Pair<Runtime, List<Closeable>> runtimeWithCloseables = TestRuntimeBuilder.getTestRuntimeAndClosableResources(this.execution.runtime, this.protocolSuite.testData, this.pmcd, hints);
                testPureSingleExecution.runtime = runtimeWithCloseables.getOne();
                runtimeWithCloseables.getTwo().forEach(closeableConsumer);
            }
            else
            {
                MutableList<Closeable> closeables = Lists.mutable.empty();
                testPureSingleExecution.func.body.forEach(func -> func.accept(new TestValueSpecificationBuilder(closeables, this.protocolSuite.testData, this.pmcd)));
                closeables.forEach(closeableConsumer);
            }
            ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(testPureSingleExecution, null, this.pureModel, ServiceTestRunner.this.pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
            JavaHelper.compilePlan(singleExecutionPlan, Identity.getAnonymousIdentity());
            this.plan = singleExecutionPlan;
        }

        @Override
        protected TestResult runAtomicTest(ServiceTest atomicTest)
        {
            return runSingleExecTest(atomicTest, this.plan);
        }
    }

    /**
     * Multi-execution session for services configured with explicit
     * {@code executionParameters}. Builds one plan per env key referenced
     * by any test in the suite (so the session can serve any test the
     * caller picks). Each atomic test runs once per applicable env;
     * results are bundled into a {@link MultiExecutionServiceTestResult}.
     */
    private class MultiExecutionParametersTestSuiteSession extends ServiceTestSuiteSession
    {
        private final PureMultiExecution execution;
        private final MutableMap<String, SingleExecutionPlan> plansByKey = Maps.mutable.empty();

        MultiExecutionParametersTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, ServiceTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd, PureMultiExecution execution)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd);
            this.execution = execution;
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer) throws Exception
        {
            TestConnectionBuildParameters hints = computeHints(this.pureModel);
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport()));
            MutableList<PlanTransformer> planTransformers = ServiceTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

            MutableSet<String> envIdsInSuite = Sets.mutable.empty();
            for (Test test : this.protocolSuite.tests)
            {
                if (test instanceof ServiceTest)
                {
                    List<String> testKeys = ((ServiceTest) test).keys;
                    if (testKeys == null || testKeys.isEmpty())
                    {
                        ListIterate.collect(this.execution.executionParameters, p -> p.key, envIdsInSuite);
                    }
                    else
                    {
                        envIdsInSuite.addAll(testKeys);
                    }
                }
            }

            for (KeyedExecutionParameter param : this.execution.executionParameters)
            {
                if (envIdsInSuite.contains(param.key))
                {
                    PureSingleExecution pureSingleExecution = new PureSingleExecution();
                    pureSingleExecution.func = this.execution.func;
                    pureSingleExecution.mapping = param.mapping;
                    Pair<Runtime, List<Closeable>> runtimeWithCloseables = TestRuntimeBuilder.getTestRuntimeAndClosableResources(param.runtime, this.protocolSuite.testData, this.pmcd, hints);
                    pureSingleExecution.runtime = runtimeWithCloseables.getOne();
                    pureSingleExecution.executionOptions = param.executionOptions;
                    runtimeWithCloseables.getTwo().forEach(closeableConsumer);
                    ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(pureSingleExecution, null, this.pureModel, ServiceTestRunner.this.pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
                    SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
                    JavaHelper.compilePlan(singleExecutionPlan, Identity.getAnonymousIdentity());
                    this.plansByKey.put(param.key, singleExecutionPlan);
                }
            }
        }

        @Override
        protected TestResult runAtomicTest(ServiceTest atomicTest)
        {
            MultiExecutionServiceTestResult multiResult = new MultiExecutionServiceTestResult();
            multiResult.testable = testablePath(this.pureModel);
            multiResult.atomicTestId = atomicTest.id;
            multiResult.testSuiteId = getTestSuiteId();
            SetIterable<String> testKeys = ((atomicTest.keys == null) || atomicTest.keys.isEmpty()) ? Sets.immutable.empty() : Sets.mutable.withAll(atomicTest.keys);
            this.execution.executionParameters.forEach(param ->
            {
                if (testKeys.isEmpty() || testKeys.contains(param.key))
                {
                    SingleExecutionPlan plan = this.plansByKey.get(param.key);
                    if (plan != null)
                    {
                        multiResult.addTestResult(param.key, runSingleExecTest(atomicTest, plan));
                    }
                }
            });
            return multiResult;
        }
    }

    /**
     * Multi-execution session for services that reference an
     * executionEnvironment. Generates one composite plan covering every
     * key resolved from the suite's tests; each atomic test picks its own
     * plan from the composite by the key carried in its parameters.
     */
    private class MultiExecutionEnvironmentTestSuiteSession extends ServiceTestSuiteSession
    {
        private final PureMultiExecution execution;
        private CompositeExecutionPlan compositePlan;
        private Map<String, String> validKeyMap;

        MultiExecutionEnvironmentTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, ServiceTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd, PureMultiExecution execution)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd);
            this.execution = execution;
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer) throws Exception
        {
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport()));
            MutableList<PlanTransformer> planTransformers = ServiceTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
            String execKey = ((Root_meta_legend_service_metamodel_PureMultiExecution) ServiceTestRunner.this.pureService._execution())._executionKey();
            List<String> allTestIds = this.protocolSuite.tests.stream().map(t -> t.id).collect(Collectors.toList());
            this.validKeyMap = getAllValidKeysForExecEnv(execKey, this.protocolSuite, allTestIds);
            PureMultiExecution multiExecution = shallowCopyMultiExecution(this.execution);
            List<Closeable> tempCloseables = Lists.mutable.empty();
            multiExecution.func.body.forEach(valSpec -> valSpec.accept(new TestValueSpecificationBuilder(new ArrayList<>(this.validKeyMap.values()), tempCloseables, this.protocolSuite.testData, this.pmcd)));
            tempCloseables.forEach(closeableConsumer);
            this.compositePlan = ServicePlanGenerator.generateCompositeExecutionPlan(multiExecution, null, this.pureModel, ServiceTestRunner.this.pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            for (String key : Sets.mutable.withAll(this.validKeyMap.values()))
            {
                SingleExecutionPlan execPlan = this.compositePlan.executionPlans.get(key);
                if (execPlan != null)
                {
                    JavaHelper.compilePlan(execPlan, Identity.getAnonymousIdentity());
                }
            }
        }

        @Override
        protected TestResult runAtomicTest(ServiceTest atomicTest)
        {
            MultiExecutionServiceTestResult multiResult = new MultiExecutionServiceTestResult();
            multiResult.testable = testablePath(this.pureModel);
            multiResult.atomicTestId = atomicTest.id;
            multiResult.testSuiteId = getTestSuiteId();
            String key = this.validKeyMap.get(atomicTest.id);
            multiResult.addTestResult(key, runSingleExecTest(atomicTest, this.compositePlan.executionPlans.get(key)));
            return multiResult;
        }
    }
}
