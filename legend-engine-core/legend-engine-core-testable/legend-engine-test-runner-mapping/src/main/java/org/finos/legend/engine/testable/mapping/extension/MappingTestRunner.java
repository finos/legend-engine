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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.PlanWithDebug;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedDataHelper;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecutionPlanDebug;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.AbstractTestSuiteSessionWithResources;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.engine.testable.extension.TestSuiteSession;
import org.finos.legend.engine.testable.helper.TestResultHelper;
import org.finos.legend.engine.testable.helper.TestReturnTypeHelper;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore;
import org.finos.legend.pure.generated.Root_meta_core_runtime_ConnectionStore_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MappingTestRunner implements TestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingTestRunner.class);

    private final Mapping pureMapping;
    private final MutableList<PlanGeneratorExtension> extensions;
    private final PlanExecutor executor;
    private final String pureVersion;
    private final MutableList<ConnectionFactoryExtension> factories;

    public MappingTestRunner(Mapping pureMapping, String pureVersion)
    {
        this.pureMapping = pureMapping;
        this.pureVersion = pureVersion;
        this.executor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
        this.extensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        this.factories = Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
    }

    @Override
    public TestSuiteSession<TestResult> openTestSuiteSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        return new MappingTestSuiteSession(testSuite, getProtocolSuite(testSuite, pureModel, data), pureModel, data);
    }

    @Override
    public boolean supportsDebug()
    {
        return true;
    }

    @Override
    public TestSuiteSession<TestDebug> openTestSuiteDebugSession(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        return new MappingTestSuiteDebugSession(testSuite, getProtocolSuite(testSuite, pureModel, data), pureModel, data);
    }

    @Override
    public TestResult executeAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData pmcd)
    {
        throw new UnsupportedOperationException("Mapping Test should be executed in context of Mapping Test Suite only");
    }

    @Override
    public TestDebug debugAtomicTest(Root_meta_pure_test_AtomicTest atomicTest, PureModel pureModel, PureModelContextData pmcd)
    {
        throw new UnsupportedOperationException("Mapping Test should be executed in context of Mapping Test Suite only");
    }

    private MappingTestRunnerContext buildMappingContext(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData pureModelContextData)
    {
        String testablePath = HelperModelBuilder.getElementFullPath(this.pureMapping, pureModel.getExecutionSupport());
        Assert.assertTrue(testSuite instanceof Root_meta_pure_mapping_metamodel_MappingTestSuite, () -> "Test Suite in Mapping expected to be of type Mapping Test Suite");
        Root_meta_pure_mapping_metamodel_MappingTestSuite compiledMappingTestSuite = (Root_meta_pure_mapping_metamodel_MappingTestSuite) testSuite;
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = ListIterate.detect(pureModelContextData.getElementsOfType(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class), ele -> ele.getPath().equals(testablePath));
        return new MappingTestRunnerContext(compiledMappingTestSuite, mapping, pureModel, pureModelContextData, extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers), new ConnectionFirstPassBuilder(pureModel.getContext()), PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport())));
    }

    private TestResult executeMappingTest(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints)
    {
        List<Pair<Connection, List<Closeable>>> connections = Lists.mutable.empty();
        try
        {
            connections = generateExecutionPlan(mappingTest, context, hints, false);
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
            return TestResultHelper.newTestError(mappingTest.id, e);
        }
        finally
        {
            this.closeConnections(connections);
        }
    }

    private TestDebug debugMappingTest(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints)
    {
        List<Pair<Connection, List<Closeable>>> connections = Lists.mutable.empty();

        try
        {
            TestExecutionPlanDebug executionPlanDebug = new TestExecutionPlanDebug();
            connections = generateExecutionPlan(mappingTest, context, hints, true);
            executionPlanDebug.executionPlan = context.getPlan();
            executionPlanDebug.debug = context.getDebug();
            executionPlanDebug.atomicTestId = mappingTest.id;
            return executionPlanDebug;
        }
        catch (Exception e)
        {
            return TestResultHelper.newTestExecutionPlanDebugError(mappingTest.id, e);
        }
        finally
        {
            this.closeConnections(connections);
        }
    }

    private List<Pair<Connection, List<Closeable>>> generateExecutionPlan(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints, boolean debug)
    {

        Root_meta_core_runtime_Runtime runtime = new Root_meta_core_runtime_Runtime_Impl("");
        List<Pair<String, EmbeddedData>> connectionInfo = mappingTest.storeTestData.stream().map(testData -> Tuples.pair(testData.store.path, EmbeddedDataHelper.resolveEmbeddedDataInPMCD(context.getPureModelContextData(), testData.data))).collect(Collectors.toList());
        List<Pair<Connection, List<Closeable>>> connections = connectionInfo.stream()
                .map(pair -> this.factories.collect(f -> f.tryBuildTestConnectionsForStore(context.getDataElementIndex(), resolveStore(context.getPureModelContextData(), pair.getOne()), pair.getTwo(), hints)).select(Objects::nonNull).select(Optional::isPresent)
                        .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + pair.getOne() + "' mentioned while running the mapping tests"))).collect(Collectors.toList());
        connections.forEach(connection ->
        {
            Connection conn = connection.getOne();
            org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store element = HelperRuntimeBuilder.getStore(conn.element, conn.elementSourceInformation, context.getPureModel().getContext());
            Root_meta_core_runtime_ConnectionStore connectionStore =
                    new Root_meta_core_runtime_ConnectionStore_Impl("")
                            ._connection(conn.accept(context.getConnectionVisitor()))
                            ._element(element);
            runtime._connectionStoresAdd(connectionStore);
        });
        handleGenerationOfPlan(connections.stream().map(Pair::getOne).collect(Collectors.toList()), runtime, context, debug);
        return connections;
    }

    private void handleGenerationOfPlan(List<Connection> incomingConnections, Root_meta_core_runtime_Runtime runtime, MappingTestRunnerContext context, boolean debug)
    {
        if (context.getPlan() == null || !shouldReusePlan(incomingConnections, context))
        {
            SingleExecutionPlan executionPlan;
            List<String> debugger;
            if (debug)
            {
                PlanWithDebug plan = PlanGenerator.generateExecutionPlanDebug(context.getMetamodelTestSuite()._query(), this.pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers());
                executionPlan = plan.plan;
                debugger = Arrays.asList(plan.debug);
            }
            else
            {
                executionPlan = PlanGenerator.generateExecutionPlan(context.getMetamodelTestSuite()._query(), this.pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers());
                debugger = null;
            }
            context.withPlan(executionPlan, debugger);
        }
        // set new connections
        context.withConnections(incomingConnections);
    }

    private boolean shouldReusePlan(List<Connection> incomingConnections, MappingTestRunnerContext context)
    {
        List<Connection> cachedConnections = context.getConnections();
        return (cachedConnections != null) &&
                (cachedConnections.size() == incomingConnections.size()) &&
                incomingConnections.stream().allMatch(incomingConnection -> cachedConnections.stream().anyMatch(cachedConn -> cachedConn == incomingConnection));
    }

    private void closeConnections(List<Pair<Connection, List<Closeable>>> connections)
    {
        connections.forEach(p -> p.getTwo().forEach(closeable ->
        {
            try
            {
                closeable.close();
            }
            catch (Exception e)
            {
                LOGGER.warn("Exception occurred closing closeable resource", e);
            }
        }));
    }

    private Store resolveStore(PureModelContextData pureModelContextData, String store)
    {
        return store.equals("ModelStore")
               ? new ModelStore()
               : (Store) ListIterate.detect(pureModelContextData.getElements(), x -> store.equals(x.getPath()));
    }

    private MappingTestSuite getProtocolSuite(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        String testablePath = HelperModelBuilder.getElementFullPath(this.pureMapping, pureModel.getExecutionSupport());
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping) ListIterate.detect(data.getElements(), elt -> (elt instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping) && testablePath.equals(elt.getPath()));
        return ListIterate.detect(mapping.testSuites, ts -> ts.id.equals(testSuite._id()));
    }

    private abstract class BaseMappingTestSuiteSession<TR> extends AbstractTestSuiteSessionWithResources<MappingTestSuite, MappingTest, TR>
    {
        MappingTestRunnerContext context;
        TestConnectionBuildParameters hints;

        private BaseMappingTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, BaseMappingTestSuiteSession::getTestSuiteTests, BaseMappingTestSuiteSession::getAtomicTestId);
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer)
        {
            this.context = buildMappingContext(this.pureSuite, this.pureModel, this.pmcd);

            boolean isRelation = TestReturnTypeHelper.isRelationReturnType(this.context.getMetamodelTestSuite()._query(), pureModel);
            this.hints = isRelation ? TestConnectionBuildParameters.newBuilder().withIsRelation(true).build() : TestConnectionBuildParameters.NONE;
        }
    }

    private class MappingTestSuiteSession extends BaseMappingTestSuiteSession<TestResult>
    {
        private MappingTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd);
        }

        @Override
        protected TestResult buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestError(this.context.getMapping().getPath(), getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestResult runAtomicTest(MappingTest atomicTest)
        {
            TestResult testResult = executeMappingTest(atomicTest, this.context, this.hints);
            testResult.testable = this.context.getMapping().getPath();
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }
    }

    private class MappingTestSuiteDebugSession extends BaseMappingTestSuiteSession<TestDebug>
    {
        private MappingTestSuiteDebugSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd);
        }

        @Override
        protected TestDebug buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestExecutionPlanDebugError(this.context.getMapping().getPath(), getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestDebug runAtomicTest(MappingTest atomicTest)
        {
            TestDebug testResult = debugMappingTest(atomicTest, this.context, this.hints);
            testResult.testable = this.context.getMapping().getPath();
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }
    }
}
