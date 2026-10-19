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
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionSecondPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.DataResolverHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperFunctionBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.ModelStoreTestConnectionFactory;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.RelationAccessorTestConnectionFactory;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.planHelper.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.testable.assertion.RelationAssertionColumnTypes;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.AbstractTestSuiteSessionWithResources;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.finos.legend.engine.testable.helper.TestExecutionContextHelper;
import org.finos.legend.engine.testable.helper.TestResultHelper;
import org.finos.legend.engine.testable.helper.TestReturnTypeHelper;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_external_store_model_JsonModelConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_model_PureModelConnection;
import org.finos.legend.pure.generated.Root_meta_external_store_model_XmlModelConnection;
import org.finos.legend.pure.generated.Root_meta_legend_function_metamodel_FunctionTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class FunctionTestRunner implements TestRunner
{
    private final ConcreteFunctionDefinition<?> functionDefinition;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor executor;
    private final String pureVersion;

    private final MutableList<ConnectionFactoryExtension> connectionBuilders = Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
    private final MutableList<RelationAccessorTestConnectionFactory> connectionAndDatabaseBuilders = Lists.mutable.withAll(ServiceLoader.load(RelationAccessorTestConnectionFactory.class));

    public FunctionTestRunner(ConcreteFunctionDefinition<?> functionDefinition, String pureVersion)
    {
        this.pureVersion = pureVersion;
        this.functionDefinition = functionDefinition;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("Function Test should be executed in context of Function Test Suite only");
    }

    @Override
    public TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        Assert.assertTrue(testSuite instanceof Root_meta_legend_function_metamodel_FunctionTestSuite, () -> "Function test suite expected in functions");
        String testablePath = HelperModelBuilder.getElementFullPath(this.functionDefinition, pureModel.getExecutionSupport());
        Function protocolFunc = ListIterate.detect(data.getElementsOfType(Function.class), el -> el.getPath().equals(testablePath));
        FunctionTestSuite protocolSuite = ListIterate.detect(protocolFunc.tests, t -> t.id.equals(testSuite._id()));
        return new FunctionTestSuiteSession(testSuite, protocolSuite, pureModel, data, testablePath);
    }

    private Store resolveStore(PureModelContextData pureModelContextData, String store)
    {
        return "ModelStore".equals(store)
               ? new ModelStore()
               : ListIterate.detect(pureModelContextData.getElementsOfType(Store.class), x -> x.getPath().equals(store));
    }

    private Root_meta_core_runtime_Runtime getRuntimesInFunction(PureModel pureModel)
    {
        RichIterable<? extends Root_meta_core_runtime_Runtime> runtimes = org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(this.functionDefinition, pureModel.getExecutionSupport());
        switch (runtimes.size())
        {
            case 0:
            {
                return null;
            }
            case 1:
            {
                return runtimes.getAny();
            }
            default:
            {
                throw new UnsupportedOperationException("Currently cannot test functions with more than one runtime present");
            }
        }
    }

    private class FunctionTestSuiteSession extends AbstractTestSuiteSessionWithResources<FunctionTestSuite, FunctionTest, TestResult>
    {
        private final String testablePath;
        private FunctionTestRunnerContext context;
        private FunctionDefinition<?> modifiedFunctionDefinition;
        private final TestConnectionBuildParameters hints;
        private final RelationAssertionColumnTypes relationAssertionColumnTypes;
        private final List<Pair<Root_meta_core_runtime_ConnectionStore, Root_meta_core_runtime_Connection>> storeConnectionsPairs = Lists.mutable.empty();

        FunctionTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, FunctionTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd, String testablePath)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, FunctionTestSuiteSession::getTestSuiteTests, FunctionTestSuiteSession::getAtomicTestId);
            this.testablePath = testablePath;
            if (TestReturnTypeHelper.isRelationReturnType(FunctionTestRunner.this.functionDefinition, pureModel))
            {
                this.hints = TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();
                this.relationAssertionColumnTypes = RelationAssertionColumnTypes.of(TestReturnTypeHelper.getRelationReturnColumns(FunctionTestRunner.this.functionDefinition, pureModel));
            }
            else
            {
                this.hints = TestConnectionBuildParameters.NONE;
                this.relationAssertionColumnTypes = RelationAssertionColumnTypes.NONE;
            }
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer)
        {
            Root_meta_legend_function_metamodel_FunctionTestSuite functionTestSuite = (Root_meta_legend_function_metamodel_FunctionTestSuite) this.pureSuite;
            this.context = new FunctionTestRunnerContext(
                    Tuples.pair(this.pmcd, this.pureModel),
                    Tuples.pair(this.protocolSuite, functionTestSuite),
                    FunctionTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers),
                    new ConnectionFirstPassBuilder(this.pureModel.getContext()),
                    PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport())));
            registerCloseable(() -> this.storeConnectionsPairs.forEach(p -> p.getOne()._connection(p.getTwo())));
            setup();
        }

        @Override
        protected TestResult buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestError(this.testablePath, getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestResult runAtomicTest(FunctionTest atomicTest)
        {
            TestResult testResult = executeFunctionTest(atomicTest);
            testResult.testable = this.testablePath;
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }

        private TestResult executeFunctionTest(FunctionTest functionTest)
        {
            try
            {
                FunctionDefinition<?> effectiveFunction = this.modifiedFunctionDefinition != null ? this.modifiedFunctionDefinition : FunctionTestRunner.this.functionDefinition;
                SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(effectiveFunction, null, null, TestExecutionContextHelper.executionContextFor(this.hints, this.context.getPureModel()), this.context.getPureModel(), FunctionTestRunner.this.pureVersion, PlanPlatform.JAVA, null, this.context.getRouterExtensions(), this.context.getExecutionPlanTransformers());
                TestAssertion assertion = functionTest.assertions.get(0);
                PlanExecutor.ExecuteArgsBuilder executeArgs = this.context.getExecuteBuilder().withPlan(executionPlan);
                MutableMap<String, Object> parameters = Maps.mutable.empty();
                if (functionTest.parameters != null)
                {
                    PrimitiveValueSpecificationToObjectVisitor visitor = new PrimitiveValueSpecificationToObjectVisitor();
                    functionTest.parameters.forEach(pv -> parameters.put(pv.name, pv.value.accept(visitor)));
                }
                executeArgs.withParams(parameters);
                Result result = FunctionTestRunner.this.executor.executeWithArgs(executeArgs.build());
                AssertionStatus assertionResult = assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.RAW, this.relationAssertionColumnTypes));
                TestExecuted testResult = new TestExecuted(Collections.singletonList(assertionResult));
                testResult.atomicTestId = functionTest.id;
                return testResult;
            }
            catch (Exception error)
            {
                return TestResultHelper.newTestError(functionTest.id, error);
            }
        }

        private void setup()
        {
            FunctionTestSuite protocolFunctionSuite = this.context.getProtocolSuite();
            if (protocolFunctionSuite.testData == null || protocolFunctionSuite.testData.isEmpty())
            {
                return;
            }
            Root_meta_core_runtime_Runtime runtime = getRuntimesInFunction(this.context.getPureModel());
            MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, EmbeddedData> resolvedTestData = new DataResolverHelper().resolveDataFromDataResolversToProtocol(protocolFunctionSuite.testData, this.context.getPureModel().getContext(), this.context.getPureModelContextData(), HelperFunctionBuilder::resolveFunctionTestDataElement);
            MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store, EmbeddedData> storeTestData = Maps.mutable.empty();
            MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, EmbeddedData> nonStoreRelationTestData = Maps.mutable.empty();
            resolvedTestData.forEachKeyValue((element, data) ->
            {
                if (element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store)
                {
                    storeTestData.put((org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element, data);
                }
                else
                {
                    nonStoreRelationTestData.put(element, data);
                }
            });
            if (storeTestData.notEmpty() && nonStoreRelationTestData.notEmpty())
            {
                throw new IllegalStateException("Error in function testSuite " + this.context.getTestSuite()._id() + ". The combination of store and non-store relation test data is not supported");
            }
            if (storeTestData.notEmpty())
            {
                setupStoreTestData(storeTestData, runtime);
            }
            else if (nonStoreRelationTestData.notEmpty())
            {
                setupNonStoreRelationTestData(nonStoreRelationTestData);
            }
        }

        private void setupNonStoreRelationTestData(MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, EmbeddedData> nonStoreRelationTestData)
        {
            MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement, RelationElementsData> relationData = Maps.mutable.empty();
            nonStoreRelationTestData.forEachKeyValue((element, data) ->
            {
                if (data instanceof RelationElementsData)
                {
                    relationData.put(element, (RelationElementsData) data);
                }
            });
            MutableList<FunctionDefinition<?>> modifiedFunctions = FunctionTestRunner.this.connectionAndDatabaseBuilders.collect(f -> f.rewriteFunctionForTestDataExecution(FunctionTestRunner.this.functionDefinition, relationData, this.context.getPureModel()));
            modifiedFunctions.removeIf(Objects::isNull);
            if (modifiedFunctions.size() > 1)
            {
                throw new IllegalStateException("Error in function testSuite " + this.context.getTestSuite()._id() + ". The combination of accessors used is not supported");
            }
            if (modifiedFunctions.isEmpty())
            {
                throw new IllegalStateException("Error in function testSuite " + this.context.getTestSuite()._id() + ". Unsupported accessors type");
            }
            this.modifiedFunctionDefinition = modifiedFunctions.get(0);
        }

        private void setupStoreTestData(MutableMap<org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store, EmbeddedData> functionTestData, Root_meta_core_runtime_Runtime runtime)
        {
            if (runtime == null)
            {
                return;
            }
            MutableMap<String, EmbeddedData> testDataByStorePath = Maps.mutable.empty();
            functionTestData.forEachKeyValue((store, data) -> testDataByStorePath.put(HelperModelBuilder.getElementFullPath(store, this.context.getPureModel().getExecutionSupport()), data));
            MutableMap<Root_meta_core_runtime_Connection, MutableList<Root_meta_core_runtime_ConnectionStore>> connectionMap = Maps.mutable.empty();
            runtime._connectionStores().forEach(cStores -> connectionMap.getIfAbsentPut(cStores._connection(), Lists.mutable::empty).add(cStores));
            connectionMap.forEachKeyValue((connection, connectionStores) ->
            {
                MutableMap<Store, EmbeddedData> storeTestDataList = Maps.mutable.empty();
                connectionStores.forEach(connectionStore ->
                {
                    Object element = connectionStore._element();
                    if (element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store)
                    {
                        String connectionStorePath = HelperModelBuilder.getElementFullPath((org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element, this.context.getPureModel().getExecutionSupport());
                        EmbeddedData testData = testDataByStorePath.get(connectionStorePath);
                        if (testData != null)
                        {
                            storeTestDataList.put(resolveStore(this.context.getPureModelContextData(), connectionStorePath), testData);
                        }
                    }
                });
                if (storeTestDataList.notEmpty())
                {
                    Pair<Connection, List<Closeable>> closeableMockedConnections = buildTestConnection(connection, storeTestDataList);
                    Connection mockedConnection = closeableMockedConnections.getOne();
                    Root_meta_core_runtime_Connection mockedCompileConnection = mockedConnection.accept(this.context.getConnectionVisitor());
                    mockedConnection.accept(new ConnectionSecondPassBuilder(this.context.getPureModel().getContext(), mockedCompileConnection));
                    connectionStores.forEach(connectionStore ->
                    {
                        Root_meta_core_runtime_Connection realConnection = connectionStore._connection();
                        this.storeConnectionsPairs.add(Tuples.pair(connectionStore, realConnection));
                        connectionStore._connection(mockedCompileConnection);
                        registerCloseables(closeableMockedConnections.getTwo());
                    });
                }
            });
        }

        private Pair<Connection, List<Closeable>> buildTestConnection(Root_meta_core_runtime_Connection connection, MutableMap<Store, EmbeddedData> storeEmbeddedDataMap)
        {
            if (storeEmbeddedDataMap.size() == 1 && connection instanceof Root_meta_external_store_model_PureModelConnection)
            {
                EmbeddedData embeddedData = storeEmbeddedDataMap.getAny();
                if (embeddedData instanceof ExternalFormatData)
                {
                    ExternalFormatData externalFormatData = (ExternalFormatData) embeddedData;
                    if (connection instanceof Root_meta_external_store_model_JsonModelConnection)
                    {
                        String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_JsonModelConnection) connection)._class(), this.context.getPureModel().getExecutionSupport());
                        return new ModelStoreTestConnectionFactory().buildCloseableConnectionFromExternalFormat(externalFormatData, _class);
                    }
                    else if (connection instanceof Root_meta_external_store_model_XmlModelConnection)
                    {
                        String _class = HelperModelBuilder.getElementFullPath(((Root_meta_external_store_model_XmlModelConnection) connection)._class(), this.context.getPureModel().getExecutionSupport());
                        return new ModelStoreTestConnectionFactory().buildCloseableConnectionFromExternalFormat(externalFormatData, _class);
                    }
                }
            }
            for (ConnectionFactoryExtension factory : FunctionTestRunner.this.connectionBuilders)
            {
                Optional<Pair<Connection, List<Closeable>>> optional = factory.tryBuildConnectionForStoreData(this.context.getDataElementIndex(), storeEmbeddedDataMap, this.hints);
                if ((optional != null) && optional.isPresent())
                {
                    return optional.get();
                }
            }
            throw new UnsupportedOperationException("Unsupported test data for function test suite: " + this.context.getTestSuite()._id());
        }
    }
}
