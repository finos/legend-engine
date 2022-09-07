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

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;
import static org.finos.legend.engine.testable.service.extension.TestRuntimeBuilder.getTestRuntimeAndClosableResources;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.generation.ServicePlanGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureInlineExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;

import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.service.assertion.ServiceTestAssertionEvaluator;
import org.finos.legend.engine.testable.service.helper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServiceTestRunner implements TestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTestRunner.class);

    private Root_meta_legend_service_metamodel_Service pureService;

    private MutableList<PlanGeneratorExtension> extensions;
    private PlanExecutor planExecutor;

    private String pureVersion;

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

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<AtomicTestId> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(pureModel));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

        Service service = ListIterate.detect(data.getElementsOfType(Service.class), ele -> ele.getPath().equals(getElementFullPath(pureService, pureModel.getExecutionSupport())));
        ServiceTestSuite suite = ListIterate.detect(service.testSuites, ts -> ts.id.equals(testSuite._id()));
        List<String> testIds = ListIterate.collect(atomicTestIds, testId -> testId.atomicTestId);

        if (service.execution instanceof PureMultiExecution)
        {
            Map<String, MultiExecutionServiceTestResult> testResultsByTestId = Maps.mutable.empty();
            for (AtomicTest test : suite.tests)
            {
                MultiExecutionServiceTestResult multiExecutionServiceTestResult = new MultiExecutionServiceTestResult();
                multiExecutionServiceTestResult.testable = getElementFullPath(pureService, pureModel.getExecutionSupport());
                multiExecutionServiceTestResult.atomicTestId = new AtomicTestId();
                multiExecutionServiceTestResult.atomicTestId.atomicTestId = test.id;
                multiExecutionServiceTestResult.atomicTestId.testSuiteId = suite.id;

                testResultsByTestId.put(test.id, multiExecutionServiceTestResult);
            }

            for (KeyedExecutionParameter param : ((PureMultiExecution) service.execution).executionParameters)
            {
                PureSingleExecution pureSingleExecution = new PureSingleExecution();
                pureSingleExecution.func = ((PureMultiExecution) service.execution).func;
                pureSingleExecution.mapping = param.mapping;
                pureSingleExecution.runtime = param.runtime;
                pureSingleExecution.executionOptions = param.executionOptions;

                List<TestResult> testResultsForKey = executeServiceTestSuite(pureSingleExecution, suite, data, testIds, pureModel, routerExtensions, planTransformers);
                Map<String, TestResult> testResultsForKeyById = Iterate.groupByUniqueKey(testResultsForKey, e -> e.atomicTestId.atomicTestId);

                testResultsForKeyById.forEach((key, value) -> testResultsByTestId.get(key).addTestResult(param.key, value));
            }

            return new ArrayList<>(testResultsByTestId.values());
        }
        else if (service.execution instanceof PureSingleExecution)
        {
            return executeServiceTestSuite((PureSingleExecution) service.execution, suite, data, testIds, pureModel, routerExtensions, planTransformers);
        }
        else if (service.execution instanceof PureInlineExecution)
        {
            return executeServiceTestSuite((PureInlineExecution) service.execution, suite, data, testIds, pureModel, routerExtensions, planTransformers);
        }
        else
        {
            throw new UnsupportedOperationException("Execution type : " + service.execution.getClass().getSimpleName() + " not supported with ServiceTestRunner");
        }
    }

    private List<TestResult> executeServiceTestSuite(PureExecution execution, ServiceTestSuite suite, PureModelContextData data, List<String> testIds, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        List<Closeable> closeables = Lists.mutable.empty();

        if (execution instanceof PureSingleExecution)
        {
            Pair<EngineRuntime, List<Closeable>> runtimeWithCloseables = getTestRuntimeAndClosableResources(((PureSingleExecution) execution).runtime, suite.testData, data);
            Runtime testSuiteRuntime = runtimeWithCloseables.getOne();
            PureSingleExecution testPureSingleExecution = shallowCopySingleExecution((PureSingleExecution) execution);
            testPureSingleExecution.runtime = testSuiteRuntime;
            executeServiceExecutionTestSuite(testPureSingleExecution, suite, testIds, pureModel, routerExtensions, planTransformers, runtimeWithCloseables.getTwo(), results);
        }
        else if (execution instanceof PureInlineExecution)
        {
            PureInlineExecution testPureInlineExecution = shallowCopyInlineExecution((PureInlineExecution) execution);
            closeables = updateInlineExecutionFunctionAndFetchCloseables(testPureInlineExecution.func, suite.testData, data);
            executeServiceExecutionTestSuite(testPureInlineExecution, suite, testIds, pureModel, routerExtensions, planTransformers, closeables, results);
        }
        else
        {
            throw new UnsupportedOperationException("Execution type : " + execution.getClass().getSimpleName() + " not supported with ServiceTestRunner");
        }

        return results;
    }

    private List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> executeServiceExecutionTestSuite(PureExecution execution, ServiceTestSuite suite, List<String> testIds, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers, List<Closeable> closeables, List<TestResult> results)
    {
        try
        {
            ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(execution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
            JavaHelper.compilePlan(singleExecutionPlan, null);

            for (Test test : suite.tests)
            {
                if (testIds.contains(test.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeServiceTest((ServiceTest) test, singleExecutionPlan);
                    testResult.testable = getElementFullPath(pureService, pureModel.getExecutionSupport());
                    testResult.atomicTestId.testSuiteId = suite.id;

                    results.add(testResult);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception occurred executing service test suites.\n", e);
        }
        finally
        {
            if (closeables != null)
            {
                closeables.forEach(closeable ->
                {
                    try
                    {
                        closeable.close();
                    }
                    catch (IOException e)
                    {
                        LOGGER.warn("Exception occurred closing closeable resource" + e);
                    }
                });
            }
        }

        return results;
    }

    private org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult executeServiceTest(ServiceTest serviceTest, SingleExecutionPlan executionPlan)
    {
        AtomicTestId atomicTestId = new AtomicTestId();
        atomicTestId.atomicTestId = serviceTest.id;

        SerializationFormat testSerializationFormat = getSerializationFormatForTest(serviceTest);

        try
        {
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

            org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult;

            List<AssertionStatus> assertionStatusList = Lists.mutable.empty();
            for (TestAssertion assertion : serviceTest.assertions)
            {
                AssertionStatus status = assertion.accept(new ServiceTestAssertionEvaluator(result, testSerializationFormat));
                if (status == null)
                {
                    throw new RuntimeException("Can't evaluate the test assertion: '" + assertion.id + "'");
                }
                assertionStatusList.add(status);
                if (!isResultReusable)
                {
                    result = this.planExecutor.execute(executionPlan, parameters);
                }
            }

            List<AssertFail> failedAsserts = ListIterate.selectInstancesOf(assertionStatusList, AssertFail.class);

            if (failedAsserts.isEmpty())
            {
                testResult = new TestPassed();
                testResult.atomicTestId = atomicTestId;
            }
            else
            {
                TestFailed testFailed = new TestFailed();
                testFailed.assertStatuses = assertionStatusList;

                testResult = testFailed;
                testResult.atomicTestId = atomicTestId;
            }

            return testResult;
        }
        catch (Exception e)
        {
            TestError testError = new TestError();
            testError.atomicTestId = atomicTestId;
            testError.error = e.toString();

            return testError;
        }
    }

    private static SerializationFormat getSerializationFormatForTest(ServiceTest serviceTest)
    {
        if (serviceTest.serializationFormat == null)
        {
            return SerializationFormat.defaultFormat;
        }
        else
        {
            try
            {
                return SerializationFormat.valueOf(serviceTest.serializationFormat);
            }
            catch (IllegalArgumentException exception)
            {
                throw new UnsupportedOperationException("Unsupported serialization format '" + serviceTest.serializationFormat
                        + "'." + "Supported formats are:" + Stream.of(SerializationFormat.values()).map(SerializationFormat::name).collect(Collectors.joining(",")));
            }
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

    private static PureInlineExecution shallowCopyInlineExecution(PureInlineExecution pureSingleExecution)
    {
        PureInlineExecution shallowCopy = new PureInlineExecution();
        shallowCopy.func = pureSingleExecution.func;
        return shallowCopy;
    }

    private List<Closeable> updateInlineExecutionFunctionAndFetchCloseables(Lambda lambda, TestData testData, PureModelContextData pureModelContextData)
    {
        List<Closeable> closeables = Lists.mutable.empty();
        lambda.body = ListIterate.collect(lambda.body, vs -> vs.accept(new TestValueSpecificationBuilder(closeables, testData, pureModelContextData)));
        return closeables;
    }
}
