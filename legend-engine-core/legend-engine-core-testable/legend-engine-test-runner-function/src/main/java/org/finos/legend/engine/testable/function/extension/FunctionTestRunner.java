// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.testable.function.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.ModelStoreTestConnectionFactory;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;
import org.finos.legend.pure.generated.Root_meta_external_store_model_JsonModelConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_model_PureModelConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_model_XmlModelConnection;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class FunctionTestRunner implements TestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionTestRunner.class);
    private final ConcreteFunctionDefinition functionDefinition;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor executor;
    private final String pureVersion;

    private final MutableList<ConnectionFactoryExtension> connectionBuilders = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
    private List<Closeable> closeables = Lists.mutable.empty();
    private List<Pair<Root_meta_core_runtime_ConnectionStore, Root_meta_core_runtime_Connection>> storeConnectionsPairs = Lists.mutable.empty();

    public FunctionTestRunner(ConcreteFunctionDefinition functionDefinition, String pureVersion)
    {
        this.pureVersion = pureVersion;
        this.functionDefinition = functionDefinition;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Function Test should be executed in context of Mapping Test Suite only");
    }

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
        Assert.assertTrue(testSuite instanceof Root_meta_legend_function_metamodel_FunctionTestSuite, () -> "Function test suite expected in functions");
        Root_meta_legend_function_metamodel_FunctionTestSuite functionTestSuite = (Root_meta_legend_function_metamodel_FunctionTestSuite) testSuite;
        String testablePath = getElementFullPath(this.functionDefinition, pureModel.getExecutionSupport());
        try
        {
            // handle data
            Function protocolFunc = ListIterate.detect(pureModelContextData.getElementsOfType(Function.class), el -> el.getPath().equals(testablePath));
            FunctionTestSuite protocolSuite = ListIterate.detect(protocolFunc.tests, t -> t.id.equals(testSuite._id()));
            FunctionTestRunnerContext runnerContext = new FunctionTestRunnerContext(Tuples.pair(pureModelContextData, pureModel), Tuples.pair(protocolSuite, functionTestSuite), extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers),
                    new ConnectionFirstPassBuilder(pureModel.getContext()),
                    PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())));
            List<FunctionTest> functionTests = protocolSuite.tests.stream().filter(t -> t instanceof FunctionTest)
                            .map(t -> (FunctionTest) t).filter(t -> atomicTestIds.contains(t.id)).collect(Collectors.toList());
            // setup
            this.setup(runnerContext);
            // run tests
            for (FunctionTest functionTest: functionTests)
            {
                TestResult functionTestResult = executeFunctionTest(functionTest, runnerContext);
                functionTestResult.testable = testablePath;
                functionTestResult.testSuiteId = functionTestSuite._id();
                results.add(functionTestResult);
            }
            //
            this.tearDown();
        }
        catch (Exception e)
        {
            // this is to catch any error for the setup of the test suite. we return test error for each test run
            for (Root_meta_pure_test_AtomicTest testedError: functionTestSuite._tests())
            {
                if (atomicTestIds.contains(testedError._id()) && results.stream().noneMatch(t -> t.atomicTestId.equals(testedError._id())))
                {
                    TestError testError = new TestError();
                    testError.atomicTestId = testedError._id();
                    testError.error = e.toString();
                    results.add(testError);
                }
            }
        }
        return results;
    }

    private TestResult  executeFunctionTest(FunctionTest functionTest, FunctionTestRunnerContext context)
    {
        try
        {
            // build plan
            SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(this.functionDefinition, null, null, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers());
            // execute assertion
            TestAssertion assertion = functionTest.assertions.get(0);
            // add execute
            PlanExecutor.ExecuteArgsBuilder executeArgs = context.getExecuteBuilder().withPlan(executionPlan);
            Map<String, Object> parameters = Maps.mutable.empty();
            if (functionTest.parameters != null)
            {
                for (ParameterValue parameterValue : functionTest.parameters)
                {
                    parameters.put(parameterValue.name, parameterValue.value.accept(new PrimitiveValueSpecificationToObjectVisitor()));
                }
            }
            executeArgs.withParams(parameters);
            Result result = this.executor.executeWithArgs(executeArgs.build());
            AssertionStatus assertionResult = assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.RAW));
            TestExecuted testResult = new TestExecuted(Collections.singletonList(assertionResult));
            testResult.atomicTestId = functionTest.id;
            return testResult;
        }
        catch (Exception error)
        {
            TestError testError = new TestError();
            testError.atomicTestId = functionTest.id;
            testError.error = error.toString();
            return testError;
        }
    }

    private void setup(FunctionTestRunnerContext context)
    {
        Root_meta_legend_function_metamodel_FunctionTestSuite functionTestSuite = context.getTestSuite();
        FunctionTestSuite protocolFunctionSuite = context.getProtocolSuite();
        if (functionTestSuite._testData() == null || functionTestSuite._testData().isEmpty())
        {
            return;
        }
        if (protocolFunctionSuite.testData == null || protocolFunctionSuite.testData.isEmpty())
        {
            return;
        }
        org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime runtime = getRuntimesInFunction(context.getPureModel());
        if (runtime == null)
        {
            return;
        }
        Map<Root_meta_core_runtime_Connection, List<org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore>> connectionMap = Maps.mutable.empty();
        runtime._connectionStores().forEach(connectionStores ->
        {
                List<org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore> stores = connectionMap.get(connectionStores._connection());
                if (stores == null)
                {
                     stores = Lists.mutable.empty();
                     connectionMap.putIfAbsent(connectionStores._connection(), stores);
                }
                stores.add(connectionStores);
        });
        for (Map.Entry<Root_meta_core_runtime_Connection, List<org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore>> entry : connectionMap.entrySet())
        {

            Root_meta_core_runtime_Connection key = entry.getKey();
            List<org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore> values = entry.getValue();
            Map<Store, EmbeddedData> storeTestDataList = Maps.mutable.empty();
            // collect test data
            entry.getValue().forEach(connectionStore ->
            {
                Object element = connectionStore._element();
                if (element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store)
                {
                    org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store metamodelStore = (org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element;
                    String connectionStorePath = getElementFullPath(metamodelStore, context.getPureModel().getExecutionSupport());
                    Optional<StoreTestData> optionalStoreTestData = protocolFunctionSuite.testData.stream().filter(
                            pTestData ->
                            {
                                String testDataStorePath = getElementFullPath(StoreProviderCompilerHelper.getStoreFromStoreProviderPointers(pTestData.store, context.getPureModel().getContext()), context.getPureModel().getExecutionSupport());
                                return testDataStorePath.equals(connectionStorePath);
                            }).findFirst();
                    if (optionalStoreTestData.isPresent())
                    {
                        StoreTestData resolvedStoreTestData = optionalStoreTestData.get();
                        EmbeddedData testData = (resolvedStoreTestData.data instanceof DataElementReference)
                                ? EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement((DataElementReference) resolvedStoreTestData.data, context.getPureModelContextData())
                                : resolvedStoreTestData.data;
                        org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store store = StoreProviderCompilerHelper.getStoreFromStoreProviderPointers(resolvedStoreTestData.store, context.getPureModel().getContext());
                        Store protocolStore = this.resolveStore(context.getPureModelContextData(), getElementFullPath(store, context.getPureModel().getExecutionSupport()));
                        storeTestDataList.put(protocolStore, testData);
                    }
                }
            });
            if (!storeTestDataList.isEmpty())
            {
                Pair<Connection, List<Closeable>> closeableMockedConnections = this.buildTestConnection(context, key, storeTestDataList);
                Connection mockedConnection = closeableMockedConnections.getOne();
                Root_meta_core_runtime_Connection mockedCompileConnection = mockedConnection.accept(context.getConnectionVisitor());
                // we replace with mocked connection. We set back to original at cleanup
                for (Root_meta_core_runtime_ConnectionStore value : values)
                {
                    Root_meta_core_runtime_Connection realConnection = value._connection();
                    this.storeConnectionsPairs.add(Tuples.pair(value, realConnection));
                    value._connection(mockedCompileConnection);
                    this.closeables.addAll(closeableMockedConnections.getTwo());
                }
            }
        }
    }

    private Pair<Connection, List<Closeable>> buildTestConnection(FunctionTestRunnerContext context,Root_meta_core_runtime_Connection connection, Map<Store, EmbeddedData> storeEmbeddedDataMap)
    {
        if (storeEmbeddedDataMap.size() == 1 && connection instanceof Root_meta_external_store_model_PureModelConnection)
        {
            EmbeddedData embeddedData = storeEmbeddedDataMap.values().stream().findFirst().get();
            if (embeddedData instanceof ExternalFormatData)
            {
                ExternalFormatData externalFormatData = (ExternalFormatData) embeddedData;
                if (connection instanceof Root_meta_external_store_model_JsonModelConnection)
                {
                    String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_JsonModelConnection) connection)._class(), context.getPureModel().getExecutionSupport());
                    return new ModelStoreTestConnectionFactory().buildCloseableConnectionFromExternalFormat(externalFormatData, _class);
                }
                else if (connection instanceof Root_meta_external_store_model_XmlModelConnection)
                {
                    String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_XmlModelConnection) connection)._class(), context.getPureModel().getExecutionSupport());
                    return  new ModelStoreTestConnectionFactory().buildCloseableConnectionFromExternalFormat(externalFormatData, _class);
                }
            }
        }
        return this.connectionBuilders.collect(f -> f.tryBuildConnectionForStoreData(context.getDataElementIndex(), storeEmbeddedDataMap)).select(Objects::nonNull).select(Optional::isPresent)
                .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported test data for function test suite: " + context.getTestSuite()._id()));
    }

    private void tearDown()
    {
        if (this.closeables != null)
        {
            this.closeables.forEach(closeable ->
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
        // restore original connection value
        if (this.storeConnectionsPairs != null)
        {
            this.storeConnectionsPairs.forEach(storeConnectionsPairs ->
            {
                storeConnectionsPairs.getOne()._connection(storeConnectionsPairs.getTwo());
            });
        }
    }

    private Store resolveStore(PureModelContextData pureModelContextData, String store)
    {
        if (store.equals("ModelStore"))
        {
            return new ModelStore();
        }
        else
        {
            return ListIterate.detect(pureModelContextData.getElementsOfType(Store.class), x -> x.getPath().equals(store));
        }
    }

    public org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime getRuntimesInFunction(PureModel pureModel)
    {
            RichIterable<org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime> runtimes =  org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(this.functionDefinition, pureModel.getExecutionSupport());
            if (runtimes.isEmpty())
            {
                return null;
            }
            else if (runtimes.size() == 1)
            {
                return runtimes.getOnly();
            }
            else
            {
                throw new UnsupportedOperationException("Currently cannot test functions with more than one runtime present");
            }
    }
}
