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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
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
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.EqualToJsonAssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.helper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_PureMultiExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Optional;
import java.util.ArrayList;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

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
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData data)
    {
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(pureModel));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

        Service service = ListIterate.detect(data.getElementsOfType(Service.class), ele -> ele.getPath().equals(getElementFullPath(pureService, pureModel.getExecutionSupport())));
        ServiceTestSuite suite = ListIterate.detect(service.testSuites, ts -> ts.id.equals(testSuite._id()));
        if (service.execution instanceof PureMultiExecution)
        {
            Map<String, MultiExecutionServiceTestResult> testResultsByTestId = Maps.mutable.empty();
            List<AtomicTest> atomicTestsInScope = ListIterate.select(suite.tests, t -> atomicTestIds.contains(t.id));
            for (AtomicTest test : atomicTestsInScope)
            {
                MultiExecutionServiceTestResult multiExecutionServiceTestResult = new MultiExecutionServiceTestResult();
                multiExecutionServiceTestResult.testable = getElementFullPath(pureService, pureModel.getExecutionSupport());
                multiExecutionServiceTestResult.atomicTestId = test.id;
                multiExecutionServiceTestResult.testSuiteId = suite.id;

                testResultsByTestId.put(test.id, multiExecutionServiceTestResult);
            }
            return executeMultiExecutionTestSuite((PureMultiExecution) service.execution, suite, atomicTestIds, pureModel, data, routerExtensions, planTransformers, testResultsByTestId);
        }
        else if (service.execution instanceof PureSingleExecution)
        {
            return executeSingleExecutionTestSuite((PureSingleExecution) service.execution, suite, atomicTestIds, pureModel, data, routerExtensions, planTransformers);
        }
        else
        {
            throw new UnsupportedOperationException("Execution type : " + service.execution.getClass().getSimpleName() + " not supported with ServiceTestRunner");
        }
    }

    private List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> executeMultiExecutionTestSuite(PureMultiExecution execution, ServiceTestSuite suite, List<String> testIds, PureModel pureModel, PureModelContextData data, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers, Map<String, MultiExecutionServiceTestResult> testResultsByTestId)
    {
        List<Closeable> closeables = null;
        PureMultiExecution multiExecution = shallowCopyMultiExecution(execution, data);
        String execkey;
        try
        {
            if (multiExecution.executionParameters != null && !multiExecution.executionParameters.isEmpty())
            {
                execkey = multiExecution.executionKey;
                closeables = multiExecution.executionParameters.stream()
                        .map(param ->
                        {
                            Pair<Runtime, List<Closeable>> closeable = TestRuntimeBuilder.getTestRuntimeAndClosableResources(param.runtime, suite.testData, data);
                            param.runtime = closeable.getOne();
                            return closeable.getTwo();
                        })
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());

            }
            else
            {
                execkey = ((Root_meta_legend_service_metamodel_PureMultiExecution) pureService._execution())._executionKey();
                List<Closeable> tempCloseables = Lists.mutable.empty();
                multiExecution.func.body.stream().forEach(valSpec -> valSpec.accept(new TestValueSpecificationBuilder(tempCloseables, suite.testData, data)));
                closeables = tempCloseables;
            }
            ExecutionPlan executionPlan = ServicePlanGenerator.generateCompositeExecutionPlan(multiExecution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            CompositeExecutionPlan compositeExecutionPlan = (CompositeExecutionPlan) executionPlan;
            String finalExeckey = execkey;
            for (Test test : suite.tests)
            {
                if (testIds.contains(test.id))
                {
                    List<String> keys = Lists.mutable.empty();
                    if (((ServiceTest) test).parameters != null)
                    {
                        Optional<ParameterValue> keyValue = ((ServiceTest) test).parameters.stream()
                                .filter(parameterValue -> parameterValue.name.equals(finalExeckey)).findFirst();
                        if (keyValue.isPresent())
                        {
                            keys.add(keyValue.get().value.accept(new PrimitiveValueSpecificationToObjectVisitor()).toString());
                        }
                    }
                    if (keys.isEmpty())
                    {
                        keys = ((ServiceTest) test).keys.isEmpty() ? multiExecution.executionParameters.stream().map(param -> param.key).collect(Collectors.toList()) : ((ServiceTest) test).keys;
                    }
                    keys.stream().forEach(key ->
                    {
                        try
                        {
                            SingleExecutionPlan execPlan = compositeExecutionPlan.executionPlans.get(key);
                            JavaHelper.compilePlan(execPlan, null);
                            org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeServiceTest((ServiceTest) test, execPlan);
                            testResult.testable = getElementFullPath(pureService, pureModel.getExecutionSupport());
                            testResult.testSuiteId = suite.id;
                            testResultsByTestId.get(test.id).addTestResult(key, testResult);
                        }
                        catch (Exception exception)
                        {
                            throw new RuntimeException("Exception occurred while executing service test suites.\n", exception);
                        }
                    });
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
                closeables.stream().forEach(closeable ->
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
        return new ArrayList<>(testResultsByTestId.values());
    }

    private List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> executeSingleExecutionTestSuite(PureSingleExecution execution, ServiceTestSuite suite, List<String> testIds, PureModel pureModel, PureModelContextData data, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        Pair<Runtime, List<Closeable>> runtimeWithCloseables = null;
        PureSingleExecution testPureSingleExecution = shallowCopySingleExecution(execution);
        try
        {
            if (execution.runtime != null)
            {
                runtimeWithCloseables = TestRuntimeBuilder.getTestRuntimeAndClosableResources(execution.runtime, suite.testData, data);
                Runtime testSuiteRuntime = runtimeWithCloseables.getOne();
                testPureSingleExecution.runtime = testSuiteRuntime;
            }
            else
            {
                MutableList<Closeable> closeables = Lists.mutable.empty();
                testPureSingleExecution.func.body.stream().forEach(func -> func.accept(new TestValueSpecificationBuilder(closeables, suite.testData, data)));
            }

            ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(testPureSingleExecution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
            JavaHelper.compilePlan(singleExecutionPlan, null);

            for (Test test : suite.tests)
            {
                if (testIds.contains(test.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeServiceTest((ServiceTest) test, singleExecutionPlan);
                    testResult.testable = getElementFullPath(pureService, pureModel.getExecutionSupport());
                    testResult.testSuiteId = suite.id;

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
            if (runtimeWithCloseables != null)
            {
                runtimeWithCloseables.getTwo().forEach(closeable ->
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
                    span.log("EXPECTED: " + ((EqualToJsonAssertFail)status).expected);
                    span.log("ACTUAL: " + ((EqualToJsonAssertFail)status).actual);
                }
                else if (status instanceof AssertFail)
                {
                    span.log("ASSERTION: " + assertion.id + " FAILED");
                    span.log("MESSAGE: " + ((AssertFail)status).message);
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
            TestError testError = new TestError();
            testError.atomicTestId = serviceTest.id;
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

    private static PureMultiExecution shallowCopyMultiExecution(PureMultiExecution pureMultiExecution, PureModelContextData data)
    {
        PureMultiExecution shallowCopy = new PureMultiExecution();
        shallowCopy.func = pureMultiExecution.func;
        shallowCopy.executionKey = pureMultiExecution.executionKey;
        if (pureMultiExecution.executionParameters != null && !pureMultiExecution.executionParameters.isEmpty())
        {
            shallowCopy.executionParameters = pureMultiExecution.executionParameters.stream().map(param ->
            {
                KeyedExecutionParameter keyedExecutionParameter = new KeyedExecutionParameter();
                keyedExecutionParameter.key = param.key;
                keyedExecutionParameter.mapping = param.mapping;
                keyedExecutionParameter.runtime = param.runtime;
                keyedExecutionParameter.executionOptions = param.executionOptions;
                return keyedExecutionParameter;
            }).collect(Collectors.toList());
        }
        return shallowCopy;
    }
}
