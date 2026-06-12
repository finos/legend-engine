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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.HashingStrategies;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
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
import org.finos.legend.engine.protocol.pure.PureClientVersions;
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
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_metamodel_MappingTestSuite;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * {@link TestRunner} for Mapping test suites.
 *
 * <p><b>Plan management.</b> The execution plan for a test is determined by the
 * suite's query, the mapping, and the test connections (and therefore the
 * runtime) built from the test's store test data. When every test in the suite
 * yields equivalent connections (a common case - e.g., all tests reference the
 * same DataElement, or all bind ModelStore data, whose plans do not embed the
 * data content), one plan serves the whole suite: it is generated during
 * session initialization, so its cost is reported as suite setup rather than
 * disappearing into the first test run. Otherwise, plans are generated lazily as
 * tests run and cached, keyed on connection identity: a test reuses a cached plan
 * when its connections are the same instances as an earlier test's, so a suite
 * whose tests fall into a few connection groups still shares a plan within each
 * group rather than regenerating one per test. Connections are rebuilt for every
 * test execution in either mode: they are cheap, and some (notably ModelStore's)
 * pass the test's data to the execution through thread-local state bound when the
 * connection is built.
 *
 * <p><b>Thread safety.</b> A session's shared state is written only during
 * {@code initialize()} (synchronized by the session base class) and is
 * read-only afterward; the shared router extensions are pre-warmed there (see
 * {@code buildMappingContext}); the per-test path works entirely on local
 * state. Atomic tests of one session can therefore be run concurrently, within
 * the limits of the connection factories - {@code ModelStoreTestConnectionFactory}
 * in particular still mutates shared connection instances while building, which
 * is benign when concurrent tests bind the same model class and content type
 * (the usual case) but is not generally thread-safe.
 */
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
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        // This is a workaround for the fact that meta::pure::extension::Extension.serializerExtension(String[1]) is not thread safe
        String resolvedPureVersion = (this.pureVersion == null) ? PureClientVersions.production : this.pureVersion;
        routerExtensions.forEach(ext -> ext.serializerExtension(resolvedPureVersion, pureModel.getExecutionSupport()));
        return new MappingTestRunnerContext(compiledMappingTestSuite, mapping, pureModel, pureModelContextData, this.extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers), routerExtensions);
    }

    private TestResult executeMappingTest(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints, GeneratedPlan sharedPlan)
    {
        MutableList<Closeable> testOwnedCloseables = null;
        try
        {
            // Connections are built for every execution even when the plan is shared: some test
            // connections (notably ModelStore's) hand the test's data to the execution out of band,
            // through a thread-local stream bound when the connection is built, so each execution
            // needs connections freshly built on its own thread.
            Pair<MutableList<Connection>, MutableList<Closeable>> connections = buildTestConnections(resolveStoreTestData(mappingTest, context), context, hints);
            testOwnedCloseables = connections.getTwo();
            // No suite-wide shared plan: reuse a plan cached for these connection instances, or
            // generate and cache one. Tests whose connections match (by identity) share the plan.
            SingleExecutionPlan plan = (sharedPlan != null)
                    ? sharedPlan.plan
                    : context.getOrComputePlan(connections.getOne(), () -> generatePlan(buildRuntime(connections.getOne(), context), context, false)).plan;
            // execute assertion
            TestAssertion assertion = mappingTest.assertions.get(0);
            Result result = this.executor.executeWithArgs(PlanExecutor.withArgs().withPlan(plan).build());
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
            closeAll(testOwnedCloseables);
        }
    }

    private TestDebug debugMappingTest(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints, GeneratedPlan sharedPlan)
    {
        MutableList<Closeable> testOwnedCloseables = null;
        try
        {
            GeneratedPlan plan;
            if (sharedPlan != null)
            {
                plan = sharedPlan;
            }
            else
            {
                Pair<MutableList<Connection>, MutableList<Closeable>> connections = buildTestConnections(resolveStoreTestData(mappingTest, context), context, hints);
                testOwnedCloseables = connections.getTwo();
                plan = context.getOrComputePlan(connections.getOne(), () -> generatePlan(buildRuntime(connections.getOne(), context), context, true));
            }
            TestExecutionPlanDebug executionPlanDebug = new TestExecutionPlanDebug();
            executionPlanDebug.executionPlan = plan.plan;
            executionPlanDebug.debug = plan.debug;
            executionPlanDebug.atomicTestId = mappingTest.id;
            return executionPlanDebug;
        }
        catch (Exception e)
        {
            return TestResultHelper.newTestExecutionPlanDebugError(mappingTest.id, e);
        }
        finally
        {
            closeAll(testOwnedCloseables);
        }
    }

    /**
     * Resolve a test's store test data to {@code (store path, resolved data)} bindings. Data
     * element references are resolved to the referenced {@code DataElement}'s data instance,
     * so two tests referencing the same data element yield identical bindings.
     */
    private List<Pair<String, EmbeddedData>> resolveStoreTestData(MappingTest mappingTest, MappingTestRunnerContext context)
    {
        return ListIterate.collect(mappingTest.storeTestData, testData -> Tuples.pair(testData.store.path, EmbeddedDataHelper.resolveEmbeddedDataInPMCD(context.getPureModelContextData(), testData.data)));
    }

    private Pair<MutableList<Connection>, MutableList<Closeable>> buildTestConnections(List<Pair<String, EmbeddedData>> storeTestData, MappingTestRunnerContext context, TestConnectionBuildParameters hints)
    {
        MutableList<Connection> connections = Lists.mutable.empty();
        MutableList<Closeable> closeables = Lists.mutable.empty();
        storeTestData.forEach(pair ->
        {
            String storePath = pair.getOne();
            EmbeddedData embeddedData = pair.getTwo();
            for (ConnectionFactoryExtension factory : this.factories)
            {
                Optional<Pair<Connection, List<Closeable>>> optional = factory.tryBuildTestConnectionsForStore(context.getDataElementIndex(), resolveStore(context.getPureModelContextData(), storePath), embeddedData, hints);
                if ((optional != null) && optional.isPresent())
                {
                    Pair<Connection, List<Closeable>> connectionWithCloseables = optional.get();
                    connections.add(connectionWithCloseables.getOne());
                    closeables.addAll(connectionWithCloseables.getTwo());
                    return;
                }
            }
            throw new UnsupportedOperationException("Unsupported store type for:'" + storePath + "' mentioned while running the mapping tests");
        });
        return Tuples.pair(connections, closeables);
    }

    private Root_meta_core_runtime_Runtime buildRuntime(Iterable<? extends Connection> connections, MappingTestRunnerContext context)
    {
        Root_meta_core_runtime_Runtime runtime = new Root_meta_core_runtime_Runtime_Impl("");
        ConnectionFirstPassBuilder connectionVisitor = new ConnectionFirstPassBuilder(context.getPureModel().getContext());
        connections.forEach(conn ->
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store element = HelperRuntimeBuilder.getStore(conn.element, conn.elementSourceInformation, context.getPureModel().getContext());
            Root_meta_core_runtime_ConnectionStore connectionStore =
                    new Root_meta_core_runtime_ConnectionStore_Impl("")
                            ._connection(conn.accept(connectionVisitor))
                            ._element(element);
            runtime._connectionStoresAdd(connectionStore);
        });
        return runtime;
    }

    private GeneratedPlan generatePlan(Root_meta_core_runtime_Runtime runtime, MappingTestRunnerContext context, boolean debug)
    {
        // The context's router extensions are safe to share across concurrent generations
        // because buildMappingContext pre-warmed their per-version serializer extensions.
        if (debug)
        {
            PlanWithDebug plan = PlanGenerator.generateExecutionPlanDebug(context.getMetamodelTestSuite()._query(), this.pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers());
            return new GeneratedPlan(plan.plan, Arrays.asList(plan.debug));
        }
        return new GeneratedPlan(PlanGenerator.generateExecutionPlan(context.getMetamodelTestSuite()._query(), this.pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.getRouterExtensions(), context.getExecutionPlanTransformers()), null);
    }

    private MappingTestSuite getProtocolSuite(Root_meta_pure_test_TestSuite testSuite, PureModel pureModel, PureModelContextData data)
    {
        String testablePath = HelperModelBuilder.getElementFullPath(this.pureMapping, pureModel.getExecutionSupport());
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping) ListIterate.detect(data.getElements(), elt -> (elt instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping) && testablePath.equals(elt.getPath()));
        return ListIterate.detect(mapping.testSuites, ts -> ts.id.equals(testSuite._id()));
    }

    private static void closeAll(Iterable<? extends AutoCloseable> closeables)
    {
        if (closeables != null)
        {
            closeables.forEach(c ->
            {
                try
                {
                    c.close();
                }
                catch (Exception e)
                {
                    LOGGER.warn("Exception occurred closing closeable resource", e);
                }
            });
        }
    }

    private Store resolveStore(PureModelContextData pureModelContextData, String store)
    {
        return store.equals("ModelStore")
               ? new ModelStore()
               : (Store) ListIterate.detect(pureModelContextData.getElements(), x -> store.equals(x.getPath()));
    }

    /**
     * An execution plan together with its debug output ({@code null} unless generated in
     * debug mode). Immutable; a single instance is reused across tests whose connections
     * are the same instances.
     */
    static final class GeneratedPlan
    {
        final SingleExecutionPlan plan;
        final List<String> debug;

        GeneratedPlan(SingleExecutionPlan plan, List<String> debug)
        {
            this.plan = plan;
            this.debug = debug;
        }
    }

    private abstract class BaseMappingTestSuiteSession<TR> extends AbstractTestSuiteSessionWithResources<MappingTestSuite, MappingTest, TR>
    {
        private final boolean debug;
        MappingTestRunnerContext context;
        TestConnectionBuildParameters hints;
        /**
         * Non-null when one plan serves every test in the suite, in which case it is
         * generated during initialization (from connections built and closed there).
         * Null when the suite's tests do not all share one plan; each test then obtains
         * its plan from a connection-keyed plan cache as it runs, so tests with matching
         * connections still share. Either way connections are rebuilt per test execution.
         * Written only during the (synchronized) initialization, read-only afterward.
         */
        GeneratedPlan sharedPlan;

        private BaseMappingTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd, boolean debug)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, BaseMappingTestSuiteSession::getTestSuiteTests, BaseMappingTestSuiteSession::getAtomicTestId);
            this.debug = debug;
        }

        @Override
        protected void initialize(Consumer<? super AutoCloseable> closeableConsumer)
        {
            this.context = buildMappingContext(this.pureSuite, this.pureModel, this.pmcd);
            this.hints = TestReturnTypeHelper.isRelationReturnType(this.context.getMetamodelTestSuite()._query(), pureModel)
                         ? TestConnectionBuildParameters.newBuilder().withIsRelation(true).build()
                         : TestConnectionBuildParameters.NONE;
            this.sharedPlan = computeSharedPlan(closeableConsumer);
        }

        /**
         * A single plan to serve every test in the suite, or {@code null} when no such plan is
         * valid - in which case each test generates its own plan as it runs. A shared plan is
         * generated here from one set of connections that are closed at the end of initialization,
         * so it is valid only if it embeds no per-build connection state: that holds exactly when
         * the suite's test connections are stable across rebuilds. Stability is checked by
         * rebuilding every test's connections (the first test included, so a single-test suite is
         * checked too) and confirming each rebuild yields the same connection instances as the
         * plan's - the same connection-identity criterion the per-test path used historically.
         * ModelStore's singleton connections satisfy it; freshly-built service-store / MongoDB
         * connections, which carry a dynamically allocated local server address, do not.
         */
        private GeneratedPlan computeSharedPlan(Consumer<? super AutoCloseable> closeableConsumer)
        {
            Collection<String> atomicTestIds = getAtomicTestIds();
            if (atomicTestIds.isEmpty())
            {
                return null;
            }

            MutableList<Closeable> planCloseables = null;
            try
            {
                Pair<MutableList<Connection>, MutableList<Closeable>> planConnections = buildTestConnections(resolveStoreTestData(getAtomicTest(Iterate.getFirst(atomicTestIds)), this.context), this.context, this.hints);
                planCloseables = planConnections.getTwo();
                SetIterable<Connection> connectionSet = UnifiedSetWithHashingStrategy.newSet(HashingStrategies.identityStrategy(), planConnections.getOne());
                for (String atomicTestId : atomicTestIds)
                {
                    Pair<MutableList<Connection>, MutableList<Closeable>> rebuilt = buildTestConnections(resolveStoreTestData(getAtomicTest(atomicTestId), this.context), this.context, this.hints);
                    try
                    {
                        if ((connectionSet.size() != rebuilt.getOne().size()) || !rebuilt.getOne().allSatisfy(connectionSet::contains))
                        {
                            return null;
                        }
                    }
                    finally
                    {
                        closeAll(rebuilt.getTwo());
                    }
                }
                GeneratedPlan plan = generatePlan(buildRuntime(planConnections.getOne(), this.context), this.context, this.debug);
                if (planCloseables != null)
                {
                    planCloseables.forEach(closeableConsumer);
                    planCloseables = null;
                }
                return plan;
            }
            catch (Exception e)
            {
                LOGGER.warn("Unable to generate a shared plan for the suite; falling back to per-test plan generation", e);
                return null;
            }
            finally
            {
                closeAll(planCloseables);
            }
        }
    }

    private class MappingTestSuiteSession extends BaseMappingTestSuiteSession<TestResult>
    {
        private MappingTestSuiteSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, false);
        }

        @Override
        protected TestResult buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestError(this.context.getMapping().getPath(), getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestResult runAtomicTest(MappingTest atomicTest)
        {
            TestResult testResult = executeMappingTest(atomicTest, this.context, this.hints, this.sharedPlan);
            testResult.testable = this.context.getMapping().getPath();
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }
    }

    private class MappingTestSuiteDebugSession extends BaseMappingTestSuiteSession<TestDebug>
    {
        private MappingTestSuiteDebugSession(Root_meta_pure_test_TestSuite pureSuite, MappingTestSuite protocolSuite, PureModel pureModel, PureModelContextData pmcd)
        {
            super(pureSuite, protocolSuite, pureModel, pmcd, true);
        }

        @Override
        protected TestDebug buildErrorResult(String atomicTestId, Throwable t)
        {
            return TestResultHelper.newTestExecutionPlanDebugError(this.context.getMapping().getPath(), getTestSuiteId(), atomicTestId, t);
        }

        @Override
        protected TestDebug runAtomicTest(MappingTest atomicTest)
        {
            TestDebug testResult = debugMappingTest(atomicTest, this.context, this.hints, this.sharedPlan);
            testResult.testable = this.context.getMapping().getPath();
            testResult.testSuiteId = getTestSuiteId();
            return testResult;
        }
    }
}
