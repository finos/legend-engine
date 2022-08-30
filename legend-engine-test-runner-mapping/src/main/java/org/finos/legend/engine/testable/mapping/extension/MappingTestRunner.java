// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.test.runner.mapping.extension;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.test.AtomicTestId;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertFail;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestFailed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestPassed;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.TestRunner;

import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.collections.api.tuple.Pair;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

public class MappingTestRunner implements TestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingTestRunner.class);
    private Mapping pureMapping;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final Root_meta_pure_runtime_Runtime_Impl runtime;
    private final PlanExecutor executor;
    private final String pureVersion;
    private final MutableList<ConnectionFactoryExtension> factories = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));

    public MappingTestRunner(Mapping pureMapping, String pureVersion)
    {
        this.pureMapping = pureMapping;
        this.pureVersion = pureVersion;
        this.executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        this.runtime = new Root_meta_pure_runtime_Runtime_Impl("");
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData pmcd)
    {
        throw new UnsupportedOperationException("Mapping Test should be executed in context of Mapping Test Suite only");
    }

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<AtomicTestId> atomicTestIds, PureModel pureModel, PureModelContextData pmcd)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = extensions.flatCollect(e -> e.getExtraExtensions(pureModel));
        MutableList<PlanTransformer> planTransformers = extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
        ConnectionVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection> connectionVisitor = new ConnectionFirstPassBuilder(pureModel.getContext());
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = ListIterate.detect(pmcd.getElementsOfType(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class), ele -> ele.getPath().equals(getElementFullPath(this.pureMapping, pureModel.getExecutionSupport())));
        MappingTestSuite suite = ListIterate.detect(mapping.testSuites, ts -> ts.id.equals(testSuite._id()));
        List<String> testIds = ListIterate.collect(atomicTestIds, testId -> testId.atomicTestId);
        List<Pair<Connection, List<Closeable>>> connections = buildTestConnections(pmcd, suite.storeTestDatas);
        connections.stream().forEach(conn -> this.runtime._connectionsAdd(conn.getOne().accept(connectionVisitor)));
        try
        {
            for (Test test : suite.tests)
            {
                if (testIds.contains(test.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeMappingTest((MappingTest) test, pureMapping, pureModel, planTransformers, routerExtensions);
                    testResult.testable = getElementFullPath(pureMapping, pureModel.getExecutionSupport());
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
            List<Closeable> runtimeCloseables = connections.stream().map(x -> x.getTwo()).flatMap(Collection::stream).collect(Collectors.toList());
            if (runtimeCloseables != null)
            {
                runtimeCloseables.stream().forEach(closeable ->
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

    private List<Pair<Connection, List<Closeable>>> buildTestConnections(PureModelContextData pmcd, List<StoreTestData> storeTestData)
    {
        List<Pair<String, EmbeddedData>> connectionInfo = storeTestData.stream().map(testData ->
        {
            EmbeddedData embeddedData = null;
            if (testData.data instanceof DataElementReference)
            {
                DataElement dataElement = Iterate.detect(pmcd.getElementsOfType(DataElement.class), e -> ((DataElementReference) testData.data).dataElement.equals(e.getPath()));
                embeddedData = dataElement.data;
            }
            else
            {
                embeddedData = testData.data;
            }
            return Tuples.pair(testData.store, embeddedData);
        }).collect(Collectors.toList());

        List<Pair<Connection, List<Closeable>>> connections = connectionInfo.stream().map(pair -> this.getTestConnectionFromFactories(pair.getOne(), pair.getTwo(), pmcd)).collect(Collectors.toList());
        return connections;
    }

    private TestResult executeMappingTest(MappingTest test, Mapping pureMapping, PureModel pureModel, MutableList<PlanTransformer> planTransformers, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions)
    {
        AtomicTestId atomicTestId = new AtomicTestId();
        atomicTestId.atomicTestId = test.id;
        try
        {
            LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(test.query, new CompileContext.Builder(pureModel).build());
            SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(pureLambda, pureMapping, this.runtime, null, pureModel,this.pureVersion, PlanPlatform.JAVA, null, routerExtensions, planTransformers);
            Result result = this.executor.execute(executionPlan);

            org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult;

            List<AssertionStatus> assertionStatusList = Lists.mutable.empty();
            for (TestAssertion assertion : test.assertions)
            {
                assertionStatusList.add(assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.PURE)));
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

    private Pair<Connection, List<Closeable>> getTestConnectionFromFactories(String testStore, EmbeddedData testData, PureModelContextData pmcd)
    {
        Store store;
        if (testStore.equals("ModelStore"))
        {
            store = new ModelStore();
        }
        else
        {
            store = ListIterate.detect(pmcd.getElementsOfType(Store.class), x -> x.getPath().equals(testStore));
        }
        return this.factories
                .collect(f -> f.tryBuildTestConnectionsForStore(store, testData, pmcd.getElementsOfType(DataElement.class)))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + testStore + "' mentioned while running the mapping tests"));
    }
}
