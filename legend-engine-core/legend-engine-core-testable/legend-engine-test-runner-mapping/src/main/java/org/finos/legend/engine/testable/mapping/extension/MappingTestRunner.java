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

package org.finos.legend.engine.testable.mapping.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.*;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
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
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;
import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder.getStore;

public class MappingTestRunner implements TestRunner
{

    private static final Logger LOGGER = LoggerFactory.getLogger(MappingTestRunner.class);
    private Mapping pureMapping;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor executor;
    private final String pureVersion;
    private final MutableList<ConnectionFactoryExtension> factories = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));

    public MappingTestRunner(Mapping pureMapping, String pureVersion)
    {
        this.pureMapping = pureMapping;
        this.pureVersion = pureVersion;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData pmcd)
    {
        throw new UnsupportedOperationException("Mapping Test should be executed in context of Mapping Test Suite only");
    }

    @Override
    public List<TestResult> executeTestSuite(Root_meta_pure_test_TestSuite testSuite, List<String> atomicTestIds, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        String testablePath = getElementFullPath(this.pureMapping, pureModel.getExecutionSupport());
        Assert.assertTrue(testSuite instanceof Root_meta_pure_mapping_metamodel_MappingTestSuite, () -> "Test Suite in Mapping expected to be of type Mapping Test Suite");
        Root_meta_pure_mapping_metamodel_MappingTestSuite compiledMappingTestSuite = (Root_meta_pure_mapping_metamodel_MappingTestSuite) testSuite;
        try
        {
            org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = ListIterate.detect(pureModelContextData.getElementsOfType(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class), ele -> ele.getPath().equals(testablePath));
            MappingTestSuite mappingTestSuite = ListIterate.detect(mapping.testSuites, ts -> ts.id.equals(testSuite._id()));
            MappingTestRunnerContext context = new MappingTestRunnerContext(compiledMappingTestSuite, mapping, pureModel, pureModelContextData, extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers), new ConnectionFirstPassBuilder(pureModel.getContext()), PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())));
            // build plan, executor args
            List<MappingTest> mappingTests = mappingTestSuite.tests.stream().filter(t -> t instanceof MappingTest).map(t -> (MappingTest)t).collect(Collectors.toList());
            // running of each test is catchable and put under a test error
            for (MappingTest mappingTest : mappingTests)
            {
                if (atomicTestIds.contains(mappingTest.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeMappingTest(mappingTest, context);
                    testResult.testable = testablePath;
                    testResult.testSuiteId = compiledMappingTestSuite._id();
                    results.add(testResult);
                }
            }

        }
        catch (Exception e)
        {
            // this is to catch any error for the setup of the test suite. we return test error for each test run
            for (Root_meta_pure_test_AtomicTest testedError: compiledMappingTestSuite._tests())
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

    private TestResult executeMappingTest(MappingTest mappingTest,  MappingTestRunnerContext context)
    {
        List<Pair<Connection, List<Closeable>>> connections = Lists.mutable.empty();
        try
        {
            Root_meta_core_runtime_Runtime_Impl runtime = new Root_meta_core_runtime_Runtime_Impl("");
            List<Pair<String, EmbeddedData>> connectionInfo = mappingTest.storeTestData.stream().map(testData -> Tuples.pair(testData.store, EmbeddedDataHelper.resolveEmbeddedDataInPMCD(context.getPureModelContextData(), testData.data))).collect(Collectors.toList());
            connections = connectionInfo.stream()
                    .map(pair -> this.factories.collect(f -> f.tryBuildTestConnectionsForStore(context.getDataElementIndex(), resolveStore(context.getPureModelContextData(), pair.getOne()), pair.getTwo())).select(Objects::nonNull).select(Optional::isPresent)
                            .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + pair.getOne() + "' mentioned while running the mapping tests"))).collect(Collectors.toList());
            connections.forEach(connection ->
            {
                Connection conn = connection.getOne();
                org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store element = getStore(conn.element, conn.elementSourceInformation, context.getPureModel().getContext());
                Root_meta_core_runtime_ConnectionStore connectionStore =
                        new Root_meta_core_runtime_ConnectionStore_Impl("")
                                ._connection(conn.accept(context.getConnectionVisitor()))
                                ._element(element);
                runtime._connectionStoresAdd(connectionStore);
            });
            handleGenerationOfPlan(connections.stream().map(Pair::getOne).collect(Collectors.toList()), runtime, context);
            // execute assertion
            TestAssertion assertion = mappingTest.assertions.get(0);
            PlanExecutor.ExecuteArgs executeArgs = context.getExecuteBuilder().build();
            Result result = this.executor.executeWithArgs(executeArgs);
            AssertionStatus assertionResult = assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.RAW));
            TestExecuted testResult = new TestExecuted(Collections.singletonList(assertionResult));
            testResult.atomicTestId = mappingTest.id;
            return testResult;
        }
        catch (Exception e)
        {
            TestError testError = new TestError();
            testError.atomicTestId = mappingTest.id;
            testError.error = e.toString();
            return testError;
        }
        finally
        {
            this.closeConnections(connections);
        }
    }

    private void handleGenerationOfPlan(List<Connection> incomingConnections, Root_meta_core_runtime_Runtime_Impl runtime, MappingTestRunnerContext context)
    {
        SingleExecutionPlan executionPlan = context.getPlan();
        boolean reusePlan = false;
        if (context.getConnections() != null)
        {
            List<Connection> cachedConnections = context.getConnections();
            if (cachedConnections.size() == incomingConnections.size())
            {
                reusePlan = incomingConnections.stream().allMatch(incomingConnection -> cachedConnections.stream().anyMatch(cachedConn -> cachedConn == incomingConnection));
            }
        }
        if (executionPlan == null || !reusePlan)
        {
            executionPlan = PlanGenerator.generateExecutionPlan(context.getMetamodelTestSuite()._query(), pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers());
            context.withPlan(executionPlan);
        }
        // set new connections
        context.withConnections(incomingConnections);
    }

    private void closeConnections(List<Pair<Connection, List<Closeable>>> connections)
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

}
