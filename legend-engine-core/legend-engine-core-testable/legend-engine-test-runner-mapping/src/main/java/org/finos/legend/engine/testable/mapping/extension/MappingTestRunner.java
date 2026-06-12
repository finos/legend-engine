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
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedDataHelper;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelEmbeddedTestData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelInstanceTestData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelTestData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
 * disappearing into the first test run. Otherwise, each test generates its own
 * plan as it runs, so plans are only generated for tests that actually run.
 * Connections are rebuilt for every test execution in either mode: they are
 * cheap, and some (notably ModelStore's) pass the test's data to the execution
 * through thread-local state bound when the connection is built.
 *
 * <p><b>Thread safety.</b> A session's shared state is written only during
 * {@code initialize()} (synchronized by the session base class) and is
 * read-only afterwards; the shared router extensions are pre-warmed there (see
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
        List<Pair<Connection, List<Closeable>>> testOwnedConnections = null;
        try
        {
            // Connections are built for every execution even when the plan is shared: some test
            // connections (notably ModelStore's) hand the test's data to the execution out of band,
            // through a thread-local stream bound when the connection is built, so each execution
            // needs connections freshly built on its own thread.
            testOwnedConnections = buildTestConnections(resolveStoreTestData(mappingTest, context), context, hints);
            SingleExecutionPlan plan = (sharedPlan != null) ? sharedPlan.plan : generatePlan(buildRuntime(testOwnedConnections, context), context, false).plan;
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
            closeConnections(testOwnedConnections);
        }
    }

    private TestDebug debugMappingTest(MappingTest mappingTest, MappingTestRunnerContext context, TestConnectionBuildParameters hints, GeneratedPlan sharedPlan)
    {
        List<Pair<Connection, List<Closeable>>> testOwnedConnections = null;
        try
        {
            GeneratedPlan plan;
            if (sharedPlan != null)
            {
                plan = sharedPlan;
            }
            else
            {
                testOwnedConnections = buildTestConnections(resolveStoreTestData(mappingTest, context), context, hints);
                plan = generatePlan(buildRuntime(testOwnedConnections, context), context, true);
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
            closeConnections(testOwnedConnections);
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

    /**
     * Whether two tests' resolved store test data yield equivalent connections, meaning the
     * execution plan built from one is valid for the other. Store paths are compared by
     * equality. {@code ModelStoreData} is compared by the {@code (model class, content type)}
     * its entries bind (mirroring how {@code ModelStoreTestConnectionFactory} resolves them):
     * the model connections it builds do not embed the data — the data reaches the execution
     * out of band, through a thread-local stream — so the plan does not depend on the data
     * content. All other data is compared by identity of the resolved instances, which holds
     * when tests reference the same data element (the common case). Deliberately conservative:
     * anything it cannot see through compares as different, falling back to per-test plan
     * generation, which is always correct.
     */
    private static boolean isSameStoreTestData(List<Pair<String, EmbeddedData>> left, List<Pair<String, EmbeddedData>> right, Map<String, DataElement> dataElementIndex)
    {
        if (left.size() != right.size())
        {
            return false;
        }
        for (int i = 0; i < left.size(); i++)
        {
            if (!left.get(i).getOne().equals(right.get(i).getOne()) || !isSameData(left.get(i).getTwo(), right.get(i).getTwo(), dataElementIndex))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isSameData(EmbeddedData left, EmbeddedData right, Map<String, DataElement> dataElementIndex)
    {
        if (left == right)
        {
            return true;
        }
        if ((left instanceof ModelStoreData) && (right instanceof ModelStoreData))
        {
            List<ModelTestData> leftData = ((ModelStoreData) left).modelData;
            List<ModelTestData> rightData = ((ModelStoreData) right).modelData;
            if (leftData.size() != rightData.size())
            {
                return false;
            }
            for (int i = 0; i < leftData.size(); i++)
            {
                if (!leftData.get(i).model.equals(rightData.get(i).model))
                {
                    return false;
                }
                String leftContentType = resolveModelDataContentType(leftData.get(i), dataElementIndex);
                if ((leftContentType == null) || !leftContentType.equals(resolveModelDataContentType(rightData.get(i), dataElementIndex)))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * The content type of the data a {@code ModelTestData} entry binds its model to, after
     * resolving any data element reference, or {@code null} if it cannot be determined. The
     * content type decides which kind of model connection {@code ModelStoreTestConnectionFactory}
     * builds (JSON vs XML), and that — together with the model class — is all of the entry
     * that ends up in the plan.
     */
    private static String resolveModelDataContentType(ModelTestData data, Map<String, DataElement> dataElementIndex)
    {
        EmbeddedData resolved = null;
        if (data instanceof ModelEmbeddedTestData)
        {
            resolved = EmbeddedDataHelper.resolveDataElement(dataElementIndex, ((ModelEmbeddedTestData) data).data);
        }
        else if (data instanceof ModelInstanceTestData)
        {
            ValueSpecification instances = ((ModelInstanceTestData) data).instances;
            if (instances instanceof PackageableElementPtr)
            {
                DataElement dataElement = dataElementIndex.get(((PackageableElementPtr) instances).fullPath);
                resolved = (dataElement == null) ? null : dataElement.data;
            }
        }
        return (resolved instanceof ExternalFormatData) ? ((ExternalFormatData) resolved).contentType : null;
    }

    private List<Pair<Connection, List<Closeable>>> buildTestConnections(List<Pair<String, EmbeddedData>> storeTestData, MappingTestRunnerContext context, TestConnectionBuildParameters hints)
    {
        return storeTestData.stream()
                .map(pair -> this.factories.collect(f -> f.tryBuildTestConnectionsForStore(context.getDataElementIndex(), resolveStore(context.getPureModelContextData(), pair.getOne()), pair.getTwo(), hints)).select(Objects::nonNull).select(Optional::isPresent)
                        .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + pair.getOne() + "' mentioned while running the mapping tests"))).collect(Collectors.toList());
    }

    private Root_meta_core_runtime_Runtime buildRuntime(List<Pair<Connection, List<Closeable>>> connections, MappingTestRunnerContext context)
    {
        Root_meta_core_runtime_Runtime runtime = new Root_meta_core_runtime_Runtime_Impl("");
        ConnectionFirstPassBuilder connectionVisitor = new ConnectionFirstPassBuilder(context.getPureModel().getContext());
        connections.forEach(connection ->
        {
            Connection conn = connection.getOne();
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

    /**
     * An execution plan together with its debug output ({@code null} unless generated in
     * debug mode). Immutable.
     */
    private static final class GeneratedPlan
    {
        final SingleExecutionPlan plan;
        final List<String> debug;

        private GeneratedPlan(SingleExecutionPlan plan, List<String> debug)
        {
            this.plan = plan;
            this.debug = debug;
        }
    }

    private void closeConnections(List<Pair<Connection, List<Closeable>>> connections)
    {
        if (connections != null)
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
        private final boolean debug;
        MappingTestRunnerContext context;
        TestConnectionBuildParameters hints;
        /**
         * Non-null when one plan serves every test in the suite, in which case it is
         * generated during initialization (from connections built and closed there).
         * Null when tests' store test data is not provably equivalent; each test then
         * generates its own plan as it runs. Either way connections are rebuilt per
         * test execution. Written only during the (synchronized) initialization,
         * read-only afterwards.
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

            boolean isRelation = TestReturnTypeHelper.isRelationReturnType(this.context.getMetamodelTestSuite()._query(), pureModel);
            this.hints = isRelation ? TestConnectionBuildParameters.newBuilder().withIsRelation(true).build() : TestConnectionBuildParameters.NONE;

            List<Pair<String, EmbeddedData>> sharedStoreTestData = computeSharedStoreTestData();
            if (sharedStoreTestData != null)
            {
                // These connections exist only to generate the shared plan, and are closed as
                // soon as it is generated: every test execution builds its own connections (see
                // executeMappingTest), so nothing here needs to outlive initialization. Closing
                // also clears any thread-local state connection building bound to this thread.
                List<Pair<Connection, List<Closeable>>> connections = buildTestConnections(sharedStoreTestData, this.context, this.hints);
                try
                {
                    this.sharedPlan = generatePlan(buildRuntime(connections, this.context), this.context, this.debug);
                }
                finally
                {
                    closeConnections(connections);
                }
            }
        }

        /**
         * The store test data shared by every test in the suite, or {@code null} if the
         * tests' resolved store test data differ (or the suite has no tests, or the data
         * cannot be compared), in which case no single plan can serve them all.
         */
        private List<Pair<String, EmbeddedData>> computeSharedStoreTestData()
        {
            try
            {
                List<Pair<String, EmbeddedData>> shared = null;
                for (String atomicTestId : getAtomicTestIds())
                {
                    List<Pair<String, EmbeddedData>> storeTestData = resolveStoreTestData(getAtomicTest(atomicTestId), this.context);
                    if (shared == null)
                    {
                        shared = storeTestData;
                    }
                    else if (!isSameStoreTestData(shared, storeTestData, this.context.getDataElementIndex()))
                    {
                        return null;
                    }
                }
                return shared;
            }
            catch (Exception e)
            {
                LOGGER.warn("Unable to compare store test data for plan sharing; falling back to per-test plan generation", e);
                return null;
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
