// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.testable.dataquality.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.RelationAccessorTestConnectionFactory;
import org.finos.legend.engine.generation.dataquality.DataQualityLambdaGenerator;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataqualityRelationValidation;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationValidationTest;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationValidationTestData;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationValidationTestSuite;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.function.FunctionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.AbstractTestSuiteSessionWithResources;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.finos.legend.engine.testable.helper.TestResultHelper;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidation;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationValidationTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class DataQualityRelationValidationTestRunner implements TestRunner
{
    private final Root_meta_external_dataquality_DataQualityRelationValidation pureElement;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final MutableList<ConnectionFactoryExtension> connectionBuilders =
            Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
    private final MutableList<RelationAccessorTestConnectionFactory> connectionAndDatabaseBuilders =
            Lists.mutable.withAll(ServiceLoader.load(RelationAccessorTestConnectionFactory.class));
    private final PlanExecutor executor;
    private final String pureVersion;

    public DataQualityRelationValidationTestRunner(Root_meta_external_dataquality_DataQualityRelationValidation pureElement, String pureVersion)
    {
        this.pureElement = pureElement;
        this.pureVersion = pureVersion;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("DataQualityRelationValidation Test should be executed in the context of a Test Suite only");
    }

    @Override
    public TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        if (!(testSuite instanceof Root_meta_external_dataquality_DataQualityRelationValidationTestSuite))
        {
            throw new EngineException("Expected DataQualityRelationValidationTestSuite. Found: " + testSuite.getClass().getSimpleName());
        }
        String testablePath = HelperModelBuilder.getElementFullPath(this.pureElement, pureModel.getExecutionSupport());
        DataqualityRelationValidation protocolElement = ListIterate.detect(data.getElementsOfType(DataqualityRelationValidation.class), e -> e.getPath().equals(testablePath));
        if (protocolElement == null)
        {
            throw new EngineException("Cannot find DataqualityRelationValidation '" + testablePath + "' in the model context");
        }
        DataQualityRelationValidationTestSuite protocolSuite = ListIterate.detect(protocolElement.testSuites, s -> s.id.equals(testSuite._id()));
        return new DataQualityRelationValidationTestSuiteSession(testSuite, protocolSuite, pureModel, data, testablePath);
    }

    private class DataQualityRelationValidationTestSuiteSession
            extends AbstractTestSuiteSessionWithResources<DataQualityRelationValidationTestSuite, DataQualityRelationValidationTest, TestResult>
    {
        private final String testablePath;
        private FunctionDefinition<?> effectiveLambda;

        DataQualityRelationValidationTestSuiteSession(
                Root_meta_pure_test_TestSuite pureSuite,
                DataQualityRelationValidationTestSuite protocolSuite,
                PureModel pureModel,
                PureModelContextData pmcd,
                String testablePath)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd,
                    DataQualityRelationValidationTestSuiteSession::getTestSuiteTests,
                    DataQualityRelationValidationTestSuiteSession::getAtomicTestId);
            this.testablePath = testablePath;
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer)
        {
            this.effectiveLambda = (FunctionDefinition<?>) DataQualityRelationValidationTestRunner.this.pureElement._query();
            DataQualityRelationValidationTestData testData = this.protocolSuite.testData;
            if (testData == null || testData.testData == null || testData.testData.isEmpty())
            {
                return;
            }
            MutableList<FunctionTestData> storeTestData = Lists.mutable.empty();
            MutableList<FunctionTestData> nonStoreRelationTestData = Lists.mutable.empty();
            for (FunctionTestData td : testData.testData)
            {
                try
                {
                    StoreProviderCompilerHelper.getStoreFromPackageableElementPointer(
                            td.packageableElementPointer, this.pureModel.getContext());
                    storeTestData.add(td);
                }
                catch (Exception e)
                {
                    nonStoreRelationTestData.add(td);
                }
            }
            if (storeTestData.notEmpty() && nonStoreRelationTestData.notEmpty())
            {
                throw new EngineException("Error in DataQualityRelationValidation test suite " + this.protocolSuite.id
                        + ". The combination of store and non-store relation test data is not supported");
            }
            if (storeTestData.notEmpty())
            {
                setupStoreTestData(storeTestData);
            }
            else
            {
                this.effectiveLambda = applyTestData(this.effectiveLambda, nonStoreRelationTestData, this.pureModel, this.pmcd);
            }
        }

        private void setupStoreTestData(List<FunctionTestData> storeTestData)
        {
            Root_meta_core_runtime_Runtime runtime = getRuntimeFromLambda(this.effectiveLambda, this.pureModel);
            if (runtime == null)
            {
                return;
            }
            MutableMap<Root_meta_core_runtime_Connection, MutableList<Root_meta_core_runtime_ConnectionStore>> connectionMap = Maps.mutable.empty();
            runtime._connectionStores().forEach(cs -> connectionMap.getIfAbsentPut(cs._connection(), Lists.mutable::empty).add(cs));
            List<Pair<Root_meta_core_runtime_ConnectionStore, Root_meta_core_runtime_Connection>> originalConnections = Lists.mutable.empty();
            ConnectionFirstPassBuilder connectionVisitor = new ConnectionFirstPassBuilder(this.pureModel.getContext());
            connectionMap.forEachKeyValue((connection, connectionStores) ->
            {
                MutableMap<Store, EmbeddedData> storeEmbeddedDataMap = Maps.mutable.empty();
                connectionStores.forEach(connectionStore ->
                {
                    Object element = connectionStore._element();
                    if (!(element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store))
                    {
                        return;
                    }
                    org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store metamodelStore =
                            (org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store) element;
                    String connectionStorePath = HelperModelBuilder.getElementFullPath(
                            metamodelStore, this.pureModel.getExecutionSupport());
                    Optional<FunctionTestData> matchingTestData = storeTestData.stream().filter(td ->
                    {
                        org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store tdStore =
                                StoreProviderCompilerHelper.getStoreFromPackageableElementPointer(
                                        td.packageableElementPointer, this.pureModel.getContext());
                        return connectionStorePath.equals(HelperModelBuilder.getElementFullPath(
                                tdStore, this.pureModel.getExecutionSupport()));
                    }).findFirst();
                    if (matchingTestData.isPresent())
                    {
                        FunctionTestData resolved = matchingTestData.get();
                        EmbeddedData data = resolved.data instanceof DataElementReference
                                ? EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement(
                                        (DataElementReference) resolved.data, this.pmcd)
                                : resolved.data;
                        Store protocolStore = resolveProtocolStore(this.pmcd, connectionStorePath);
                        if (protocolStore != null)
                        {
                            storeEmbeddedDataMap.put(protocolStore, data);
                        }
                    }
                });
                if (storeEmbeddedDataMap.isEmpty())
                {
                    return;
                }
                Pair<Connection, List<Closeable>> mockConnection = buildTestConnection(storeEmbeddedDataMap);
                Root_meta_core_runtime_Connection compiledMock = mockConnection.getOne().accept(connectionVisitor);
                connectionStores.forEach(cs ->
                {
                    originalConnections.add(Tuples.pair(cs, cs._connection()));
                    cs._connection(compiledMock);
                });
                if (mockConnection.getTwo() != null)
                {
                    registerCloseables(mockConnection.getTwo());
                }
            });
            // Restore the runtime's original connections when the suite session closes so we don't
            // leave the metamodel in a mutated state for other consumers of the same PureModel.
            registerCloseable(() -> originalConnections.forEach(p -> p.getOne()._connection(p.getTwo())));
        }

        private Pair<Connection, List<Closeable>> buildTestConnection(MutableMap<Store, EmbeddedData> storeEmbeddedDataMap)
        {
            TestConnectionBuildParameters hints = TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();
            for (ConnectionFactoryExtension factory : DataQualityRelationValidationTestRunner.this.connectionBuilders)
            {
                Optional<Pair<Connection, List<Closeable>>> optional = factory.tryBuildConnectionForStoreData(
                        Collections.emptyMap(), storeEmbeddedDataMap, hints);
                if (optional != null && optional.isPresent())
                {
                    return optional.get();
                }
            }
            throw new EngineException("No ConnectionFactoryExtension could build a test connection for the "
                    + "DataQualityRelationValidation test suite " + this.protocolSuite.id);
        }

        private Store resolveProtocolStore(PureModelContextData pmcd, String path)
        {
            return ListIterate.detect(pmcd.getElementsOfType(Store.class), s -> s.getPath().equals(path));
        }

        @Override
        protected TestResult buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestError(this.testablePath, getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestResult runAtomicTest(DataQualityRelationValidationTest atomicTest)
        {
            TestResult testResult = executeValidationTest(atomicTest);
            testResult.testable = this.testablePath;
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }

        private TestResult executeValidationTest(DataQualityRelationValidationTest atomicTest)
        {
            try
            {
                LambdaFunction<?> breaksLambda = buildBreaksLambda();
                SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(
                        breaksLambda,
                        null,
                        null,
                        null,
                        this.pureModel,
                        DataQualityRelationValidationTestRunner.this.pureVersion,
                        PlanPlatform.JAVA,
                        null,
                        PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport())),
                        DataQualityRelationValidationTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

                Result result = DataQualityRelationValidationTestRunner.this.executor.execute(plan);
                TestAssertion assertion = atomicTest.assertions.get(0);
                AssertionStatus assertionStatus = assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.RAW));
                TestExecuted testResult = new TestExecuted(Collections.singletonList(assertionStatus));
                testResult.atomicTestId = atomicTest.id;
                return testResult;
            }
            catch (Exception e)
            {
                return TestResultHelper.newTestError(atomicTest.id, e);
            }
        }

        @SuppressWarnings("unchecked")
        private LambdaFunction<?> buildBreaksLambda()
        {
            Root_meta_external_dataquality_DataQualityRelationValidation element =
                    DataQualityRelationValidationTestRunner.this.pureElement;
            LambdaFunction<?> originalQuery = (LambdaFunction<?>) element._query();
            LambdaFunction<?> queryForGeneration =
                    (this.effectiveLambda == null || this.effectiveLambda == originalQuery)
                            ? originalQuery
                            : (LambdaFunction<?>) this.effectiveLambda;
            boolean swapped = queryForGeneration != originalQuery;
            if (swapped)
            {
                element._query(queryForGeneration);
            }
            try
            {
                return DataQualityLambdaGenerator.generateLambda(
                        this.pureModel, element, null, false, null, false, false, null);
            }
            finally
            {
                if (swapped)
                {
                    element._query(originalQuery);
                }
            }
        }
    }

    private static Root_meta_core_runtime_Runtime getRuntimeFromLambda(FunctionDefinition<?> lambda, PureModel pureModel)
    {
        RichIterable<? extends Root_meta_core_runtime_Runtime> runtimes =
                core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(
                        lambda, pureModel.getExecutionSupport());
        switch (runtimes.size())
        {
            case 0:
                return null;
            case 1:
                return runtimes.getAny();
            default:
                throw new EngineException("Cannot run DataQualityRelationValidation tests when the query references more than one runtime");
        }
    }

    private FunctionDefinition<?> applyTestData(FunctionDefinition<?> lambda, List<FunctionTestData> testData, PureModel pureModel, PureModelContextData pmcd)
    {
        MutableMap<PackageableElement, RelationElementsData> relationData = Maps.mutable.empty();
        for (FunctionTestData td : testData)
        {
            EmbeddedData data = td.data instanceof DataElementReference
                    ? EmbeddedDataCompilerHelper.getEmbeddedDataFromDataElement((DataElementReference) td.data, pmcd)
                    : td.data;
            if (data instanceof RelationElementsData)
            {
                PackageableElement element = pureModel.getPackageableElement(td.packageableElementPointer.path);
                relationData.put(element, (RelationElementsData) data);
            }
        }
        if (relationData.isEmpty())
        {
            return lambda;
        }
        MutableList<FunctionDefinition<?>> rewritten = this.connectionAndDatabaseBuilders
                .collect(f -> f.rewriteFunctionForTestDataExecution(lambda, relationData, pureModel));
        rewritten.removeIf(Objects::isNull);
        if (rewritten.isEmpty())
        {
            throw new EngineException("No RelationAccessorTestConnectionFactory could handle the test data for the DataQualityRelationValidation lambda");
        }
        if (rewritten.size() > 1)
        {
            throw new EngineException("Multiple RelationAccessorTestConnectionFactory implementations rewrote the DataQualityRelationValidation lambda; expected exactly one match");
        }
        return rewritten.get(0);
    }
}
