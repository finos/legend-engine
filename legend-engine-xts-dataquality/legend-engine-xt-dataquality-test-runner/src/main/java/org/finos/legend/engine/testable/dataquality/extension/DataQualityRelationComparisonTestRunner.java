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
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.generation.dataquality.DataQualityReconLambdaGenerator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.core.EmbeddedDataCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.handlers.StoreProviderCompilerHelper;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.test.RelationAccessorTestConnectionFactory;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRelationComparison;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationComparisonTest;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationComparisonTestData;
import org.finos.legend.engine.protocol.dataquality.metamodel.testable.DataQualityRelationComparisonTestSuite;
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
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationComparison;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_DataQualityRelationComparisonTestSuite;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_MD5HashStrategy;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_ReconStrategy;
import org.finos.legend.pure.generated.Root_meta_external_dataquality_datarecon_DataQualityReconInput;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.generated.core_dataquality_generation_datarecon;
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

public class DataQualityRelationComparisonTestRunner implements TestRunner
{
    private final Root_meta_external_dataquality_DataQualityRelationComparison pureElement;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final MutableList<ConnectionFactoryExtension> connectionBuilders =
            Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
    private final MutableList<RelationAccessorTestConnectionFactory> connectionAndDatabaseBuilders =
            Lists.mutable.withAll(ServiceLoader.load(RelationAccessorTestConnectionFactory.class));
    private final PlanExecutor executor;
    private final String pureVersion;

    public DataQualityRelationComparisonTestRunner(Root_meta_external_dataquality_DataQualityRelationComparison pureElement, String pureVersion)
    {
        this.pureElement = pureElement;
        this.pureVersion = pureVersion;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData data)
    {
        throw new UnsupportedOperationException("DataQualityRelationComparison Test should be executed in the context of a Test Suite only");
    }

    @Override
    public TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        if (!(testSuite instanceof Root_meta_external_dataquality_DataQualityRelationComparisonTestSuite))
        {
            throw new EngineException("Expected DataQualityRelationComparisonTestSuite. Found: " + testSuite.getClass().getSimpleName());
        }
        String testablePath = HelperModelBuilder.getElementFullPath(this.pureElement, pureModel.getExecutionSupport());
        DataQualityRelationComparison protocolElement = ListIterate.detect(data.getElementsOfType(DataQualityRelationComparison.class), e -> e.getPath().equals(testablePath));
        if (protocolElement == null)
        {
            throw new EngineException("Cannot find DataQualityRelationComparison '" + testablePath + "' in the model context");
        }
        DataQualityRelationComparisonTestSuite protocolSuite = ListIterate.detect(protocolElement.testSuites, s -> s.id.equals(testSuite._id()));
        return new DataQualityRelationComparisonTestSuiteSession(testSuite, protocolSuite, pureModel, data, testablePath);
    }

    private class DataQualityRelationComparisonTestSuiteSession
            extends AbstractTestSuiteSessionWithResources<DataQualityRelationComparisonTestSuite, DataQualityRelationComparisonTest, TestResult>
    {
        private final String testablePath;
        private LambdaFunction<?> effectiveReconLambda;

        DataQualityRelationComparisonTestSuiteSession(
                Root_meta_pure_test_TestSuite pureSuite,
                DataQualityRelationComparisonTestSuite protocolSuite,
                PureModel pureModel,
                PureModelContextData pmcd,
                String testablePath)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd,
                    DataQualityRelationComparisonTestSuiteSession::getTestSuiteTests,
                    DataQualityRelationComparisonTestSuiteSession::getAtomicTestId);
            this.testablePath = testablePath;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer)
        {
            LambdaFunction<?> reconLambda = generateReconLambda(
                    (LambdaFunction<?>) DataQualityRelationComparisonTestRunner.this.pureElement._source(),
                    (LambdaFunction<?>) DataQualityRelationComparisonTestRunner.this.pureElement._target());

            DataQualityRelationComparisonTestData testData = this.protocolSuite.testData;
            if (testData == null || testData.testData == null || testData.testData.isEmpty())
            {
                this.effectiveReconLambda = reconLambda;
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
                throw new EngineException("Error in DataQualityRelationComparison test suite " + this.protocolSuite.id
                        + ". The combination of store and non-store relation test data is not supported");
            }
            if (storeTestData.notEmpty())
            {
                mockConnectionsOnReconLambda(reconLambda, storeTestData);
                this.effectiveReconLambda = reconLambda;
            }
            else
            {
                MutableMap<PackageableElement, RelationElementsData> relationData =
                        buildRelationTestData(nonStoreRelationTestData, this.pureModel, this.pmcd);
                this.effectiveReconLambda = relationData.isEmpty()
                        ? reconLambda
                        : (LambdaFunction<?>) rewriteLambda((FunctionDefinition<?>) reconLambda, relationData, this.pureModel);
            }
        }

        private void mockConnectionsOnReconLambda(LambdaFunction<?> reconLambda, List<FunctionTestData> storeTestData)
        {
            RichIterable<? extends Root_meta_core_runtime_Runtime> runtimes =
                    core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_extractRuntimesFromFunctionDefinition_FunctionDefinition_1__Runtime_MANY_(
                            reconLambda, this.pureModel.getExecutionSupport());
            if (runtimes.isEmpty())
            {
                // Neither side uses ->from(runtime) so there is nothing to mock; the plan will run
                // against the raw stores as-is.
                return;
            }
            MutableMap<Root_meta_core_runtime_Connection, MutableList<Root_meta_core_runtime_ConnectionStore>> connectionMap = Maps.mutable.empty();
            runtimes.forEach(runtime ->
                    runtime._connectionStores().forEach(cs ->
                            connectionMap.getIfAbsentPut(cs._connection(), Lists.mutable::empty).add(cs)));
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
            registerCloseable(() -> originalConnections.forEach(p -> p.getOne()._connection(p.getTwo())));
        }

        private Pair<Connection, List<Closeable>> buildTestConnection(MutableMap<Store, EmbeddedData> storeEmbeddedDataMap)
        {
            TestConnectionBuildParameters hints = TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();
            for (ConnectionFactoryExtension factory : DataQualityRelationComparisonTestRunner.this.connectionBuilders)
            {
                Optional<Pair<Connection, List<Closeable>>> optional = factory.tryBuildConnectionForStoreData(
                        Collections.emptyMap(), storeEmbeddedDataMap, hints);
                if (optional != null && optional.isPresent())
                {
                    return optional.get();
                }
            }
            throw new EngineException("No ConnectionFactoryExtension could build a test connection for the "
                    + "DataQualityRelationComparison test suite " + this.protocolSuite.id);
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
        protected TestResult runAtomicTest(DataQualityRelationComparisonTest atomicTest)
        {
            TestResult testResult = executeComparisonTest(atomicTest);
            testResult.testable = this.testablePath;
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }

        private TestResult executeComparisonTest(DataQualityRelationComparisonTest atomicTest)
        {
            try
            {
                SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(
                        this.effectiveReconLambda,
                        null,
                        null,
                        null,
                        this.pureModel,
                        DataQualityRelationComparisonTestRunner.this.pureVersion,
                        PlanPlatform.JAVA,
                        null,
                        PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(this.pureModel.getExecutionSupport())),
                        DataQualityRelationComparisonTestRunner.this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

                Result result = DataQualityRelationComparisonTestRunner.this.executor.execute(plan);
                // DataQualityRelationComparison tests are constrained (at compile time) to a
                // single assertion per atomic test. See DataQualityCompilerExtension for the
                // enforcement.
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

        private LambdaFunction<?> generateReconLambda(LambdaFunction<?> source, LambdaFunction<?> target)
        {
            Root_meta_external_dataquality_DataQualityRelationComparison element = DataQualityRelationComparisonTestRunner.this.pureElement;
            Root_meta_external_dataquality_ReconStrategy strategy = element._strategy();
            boolean aggregatedHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy && ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._aggregatedHash();
            String sourceHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy ? ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._sourceHashColumn() : null;
            String targetHash = strategy instanceof Root_meta_external_dataquality_MD5HashStrategy ? ((Root_meta_external_dataquality_MD5HashStrategy) strategy)._targetHashColumn() : null;

            Root_meta_external_dataquality_datarecon_DataQualityReconInput reconInput =
                    core_dataquality_generation_datarecon.Root_meta_external_dataquality_datarecon_createReconInput_LambdaFunction_1__LambdaFunction_1__String_MANY__Boolean_1__String_MANY__String_$0_1$__String_$0_1$__Boolean_1__Integer_$0_1$__Boolean_1__Boolean_1__DataQualityReconInput_1_(
                            source,
                            target,
                            element._keys(),
                            aggregatedHash,
                            element._columnsToCompare(),
                            sourceHash,
                            targetHash,
                            true,
                            null,
                            false,
                            false,
                            this.pureModel.getExecutionSupport()
                    );
            return DataQualityReconLambdaGenerator.generateLambda(this.pureModel, reconInput);
        }
    }

    private MutableMap<PackageableElement, RelationElementsData> buildRelationTestData(List<FunctionTestData> testData, PureModel pureModel, PureModelContextData pmcd)
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
        return relationData;
    }

    private FunctionDefinition<?> rewriteLambda(FunctionDefinition<?> lambda, MutableMap<PackageableElement, RelationElementsData> relationData, PureModel pureModel)
    {
        if (relationData.isEmpty())
        {
            return lambda;
        }
        MutableList<FunctionDefinition<?>> rewritten = this.connectionAndDatabaseBuilders
                .collect(f -> f.rewriteFunctionForTestDataExecution(lambda, relationData, pureModel));
        rewritten.removeIf(Objects::isNull);
        if (rewritten.isEmpty())
        {
            throw new EngineException("No RelationAccessorTestConnectionFactory could handle the test data for the DataQualityRelationComparison lambda");
        }
        if (rewritten.size() > 1)
        {
            throw new EngineException("Multiple RelationAccessorTestConnectionFactory implementations rewrote the DataQualityRelationComparison lambda; expected exactly one match");
        }
        return rewritten.get(0);
    }
}
