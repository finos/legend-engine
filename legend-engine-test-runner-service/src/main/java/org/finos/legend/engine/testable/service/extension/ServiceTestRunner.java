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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
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
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
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
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.service.assertion.ServiceTestAssertionEvaluator;
import org.finos.legend.engine.testable.service.connection.TestConnectionBuilder;
import org.finos.legend.engine.testable.service.helper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.testable.service.result.MultiExecutionServiceTestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

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

                List<TestResult> testResultsForKey = executeSingleExecutionTestSuite(pureSingleExecution, suite, testIds, pureModel, data, routerExtensions, planTransformers);
                Map<String, TestResult> testResultsForKeyById = Iterate.groupByUniqueKey(testResultsForKey, e -> e.atomicTestId.atomicTestId);

                testResultsForKeyById.forEach((key, value) -> testResultsByTestId.get(key).addTestResult(param.key, value));
            }

            return new ArrayList<>(testResultsByTestId.values());
        }
        else if (service.execution instanceof PureSingleExecution)
        {
            return executeSingleExecutionTestSuite((PureSingleExecution) service.execution, suite, testIds, pureModel, data, routerExtensions, planTransformers);
        }
        else
        {
            throw new UnsupportedOperationException("Execution type : " + service.execution.getClass().getSimpleName() + " not supported with ServiceTestRunner");
        }
    }

    private List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> executeSingleExecutionTestSuite(PureSingleExecution execution, ServiceTestSuite suite, List<String> testIds, PureModel pureModel, PureModelContextData data, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, MutableList<PlanTransformer> planTransformers)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        Pair<Runtime, List<Closeable>> runtimeWithCloseables = null;

        try
        {
            runtimeWithCloseables = getTestRuntimeAndClosableResources(execution.runtime, suite.testData, data);
            Runtime testSuiteRuntime = runtimeWithCloseables.getOne();
            PureSingleExecution testPureSingleExecution = shallowCopySingleExecution(execution);
            testPureSingleExecution.runtime = testSuiteRuntime;

            ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(testPureSingleExecution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
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
            throw new RuntimeException("Exception occurred executing service test suites.\n" + e);
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

    private Pair<Runtime, List<Closeable>> getTestRuntimeAndClosableResources(Runtime runtime, TestData testData, PureModelContextData pureModelContextData)
    {
        List<Closeable> closeables = Lists.mutable.empty();
        EngineRuntime engineRuntime = resolveRuntime(runtime, pureModelContextData);

        EngineRuntime testRuntime = new EngineRuntime();
        testRuntime.mappings = engineRuntime.mappings;
        testRuntime.connections = Lists.mutable.empty();

        for (StoreConnections storeConnections : engineRuntime.connections)
        {
            StoreConnections testStoreConnections = new StoreConnections();
            testStoreConnections.store = storeConnections.store;
            testStoreConnections.storeConnections = Lists.mutable.empty();

            for (IdentifiedConnection identifiedConnection : storeConnections.storeConnections)
            {
                ConnectionTestData connectionTestData = ListIterate.detect(testData.connectionsTestData, connectionData -> connectionData.id.equals(identifiedConnection.id));

                EmbeddedData embeddedData = null;
                if (connectionTestData != null)
                {
                    if (connectionTestData.data instanceof DataElementReference)
                    {
                        DataElement dataElement = Iterate.detect(pureModelContextData.getElementsOfType(DataElement.class), e -> ((DataElementReference) connectionTestData.data).dataElement.equals(e.getPath()));
                        embeddedData = dataElement.data;
                    }
                    else
                    {
                        embeddedData = connectionTestData.data;
                    }
                }

                Pair<Connection, List<Closeable>> connectionWithCloseables = identifiedConnection.connection.accept(new TestConnectionBuilder(embeddedData, pureModelContextData));

                closeables.addAll(connectionWithCloseables.getTwo());

                IdentifiedConnection testIdentifiedConnection = new IdentifiedConnection();
                testIdentifiedConnection.id = identifiedConnection.id;
                testIdentifiedConnection.connection = connectionWithCloseables.getOne();

                testStoreConnections.storeConnections.add(testIdentifiedConnection);
            }

            testRuntime.connections.add(testStoreConnections);
        }

        return Tuples.pair(testRuntime, closeables);
    }

    private org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult executeServiceTest(ServiceTest serviceTest, SingleExecutionPlan executionPlan)
    {
        AtomicTestId atomicTestId = new AtomicTestId();
        atomicTestId.atomicTestId = serviceTest.id;

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
                result = new ConstantResult(((StreamingResult) result).flush(((StreamingResult) result).getSerializer(SerializationFormat.RAW)));
            }

            org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult;

            List<AssertionStatus> assertionStatusList = Lists.mutable.empty();
            for (TestAssertion assertion : serviceTest.assertions)
            {
                assertionStatusList.add(assertion.accept(new ServiceTestAssertionEvaluator(result)));
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

    private static PureSingleExecution shallowCopySingleExecution(PureSingleExecution pureSingleExecution)
    {
        PureSingleExecution shallowCopy = new PureSingleExecution();
        shallowCopy.func = pureSingleExecution.func;
        shallowCopy.mapping = pureSingleExecution.mapping;
        shallowCopy.runtime = pureSingleExecution.runtime;
        return shallowCopy;
    }

    private static EngineRuntime resolveRuntime(Runtime runtime, PureModelContextData pureModelContextData)
    {
        if (runtime instanceof EngineRuntime)
        {
            return (EngineRuntime) runtime;
        }
        if (runtime instanceof LegacyRuntime)
        {
            return ((LegacyRuntime) runtime).toEngineRuntime();
        }
        if (runtime instanceof RuntimePointer)
        {
            String runtimeFullPath = ((RuntimePointer) runtime).runtime;
            PackageableElement found = Iterate.detect(pureModelContextData.getElements(), e -> runtimeFullPath.equals(e.getPath()));
            if (!(found instanceof PackageableRuntime))
            {
                throw new RuntimeException("Can't find runtime '" + runtimeFullPath + "'");
            }
            return ((PackageableRuntime) found).runtimeValue;
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }
}
