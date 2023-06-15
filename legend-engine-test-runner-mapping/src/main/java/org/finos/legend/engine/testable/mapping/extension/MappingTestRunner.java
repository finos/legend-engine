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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedDataHelper;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.Store;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.ModelStore;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.status.AssertionStatus;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestError;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestExecuted;
import org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult;
import org.finos.legend.engine.testable.assertion.TestAssertionEvaluator;
import org.finos.legend.engine.testable.extension.TestRunner;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_AtomicTest;
import org.finos.legend.pure.generated.Root_meta_pure_test_TestSuite;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder.getElementFullPath;

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
        this.executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
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
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping mapping = ListIterate.detect(
            pureModelContextData.getElementsOfType(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping.class),
            ele -> ele.getPath().equals(getElementFullPath(this.pureMapping, pureModel.getExecutionSupport())));
        MappingTestSuite suite = ListIterate.detect(mapping.testSuites, ts -> ts.id.equals(testSuite._id()));
        MappingTestRunnerContext context = new MappingTestRunnerContext(testSuite, mapping, pureModel, pureModelContextData, extensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers),
            new ConnectionFirstPassBuilder(pureModel.getContext()), extensions.flatCollect(e -> e.getExtraExtensions(pureModel)));
        if (suite instanceof MappingDataTestSuite)
        {
            MappingDataTestSuite mappingDataTestSuite = (MappingDataTestSuite) suite;
            return executeMappingDataTestSuite(mappingDataTestSuite, atomicTestIds, context);
        }
        else if (suite instanceof MappingFunctionTestSuite)
        {
            MappingFunctionTestSuite mappingFunctionTestSuite = (MappingFunctionTestSuite) suite;
            return executeMappingFuncTestSuite(mappingFunctionTestSuite, atomicTestIds, context);
        }
        throw new UnsupportedOperationException("Mapping Test suite type '" + suite.getClass().getName()  + "' not supported");
    }

    public List<TestResult> executeMappingDataTestSuite(MappingDataTestSuite mappingDataTestSuite, List<String> atomicTestIds, MappingTestRunnerContext context)
    {
        PureModel pureModel = context.getPureModel();
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        List<Pair<Connection, List<Closeable>>> connections = buildTestConnections(mappingDataTestSuite.storeTestData, context);
        Root_meta_pure_runtime_Runtime_Impl runtime = new Root_meta_pure_runtime_Runtime_Impl("");
        connections.stream().forEach(conn -> runtime._connectionsAdd(conn.getOne().accept(context.connectionVisitor)));
        List<MappingFunctionTest> functionTests = this.validateMappingSuiteTests(mappingDataTestSuite, MappingFunctionTest.class);
        try
        {
            for (MappingFunctionTest mappingFunctionTest : functionTests)
            {
                if (atomicTestIds.contains(mappingFunctionTest.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeMappingFuncTest(mappingFunctionTest, runtime, context);
                    testResult.testable = getElementFullPath(pureMapping, pureModel.getExecutionSupport());
                    testResult.testSuiteId = mappingDataTestSuite.id;
                    results.add(testResult);
                }
            }
        }
        catch (Exception exception)
        {
            throw new RuntimeException("Exception occurred executing service test suites.\n" + exception);
        }
        finally
        {
            this.closeConnections(connections);
        }
        return results;
    }

    public List<TestResult> executeMappingFuncTestSuite(MappingFunctionTestSuite mappingFunctionTestSuite, List<String> atomicTestIds, MappingTestRunnerContext context)
    {
        List<org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult> results = Lists.mutable.empty();
        try
        {
            PureModel pureModel = context.getPureModel();
            // build plan, executor args
            LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(mappingFunctionTestSuite.func, new CompileContext.Builder(pureModel).build());
            List<MappingDataTest> dataTests = this.validateMappingSuiteTests(mappingFunctionTestSuite, MappingDataTest.class);
            Root_meta_pure_runtime_Runtime_Impl runtime = buildRuntimeForMultiInputTestSuite(dataTests, context);
            SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(pureLambda, pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null,
                    context.routerExtensions, context.executionPlanTransformers);
            context.getExecuteBuilder().withPlan(executionPlan);
            for (MappingDataTest dataTest : dataTests)
            {
                if (atomicTestIds.contains(dataTest.id))
                {
                    org.finos.legend.engine.protocol.pure.v1.model.test.result.TestResult testResult = executeMappingDataTest(dataTest, context);
                    testResult.testable = getElementFullPath(pureMapping, pureModel.getExecutionSupport());
                    testResult.testSuiteId = mappingFunctionTestSuite.id;
                    results.add(testResult);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception occurred executing service test suites.\n" + e);
        }
        return results;
    }

    public Root_meta_pure_runtime_Runtime_Impl buildRuntimeForMultiInputTestSuite(List<MappingDataTest> dataTests,  MappingTestRunnerContext context)
    {
        Root_meta_pure_runtime_Runtime_Impl runtime = new Root_meta_pure_runtime_Runtime_Impl("");
        if (dataTests.size() > 0)
        {
            MappingDataTest mappingDataTest = dataTests.get(0);
            List<Pair<String, EmbeddedData>> connectionInfo = mappingDataTest.storeTestData.stream().map(testData -> Tuples.pair(testData.store, EmbeddedDataHelper.resolveEmbeddedData(context.getPureModelContextData(), testData.data))).collect(Collectors.toList());
            List<Pair<Connection, List<Closeable>>> connections = connectionInfo.stream()
                    .map(pair -> this.factories.collect(f -> f.tryBuildTestConnectionsForStoreWithMultiInputs(context.dataElements, resolveStore(context.getPureModelContextData(), pair.getOne()), pair.getTwo())).select(Objects::nonNull).select(Optional::isPresent)
                    .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + pair.getOne() + "' mentioned while running the mapping tests"))).collect(Collectors.toList());
            connections.stream().forEach(conn -> runtime._connectionsAdd(conn.getOne().accept(context.connectionVisitor)));
        }
        return runtime;

    }

    private TestResult executeMappingDataTest(MappingDataTest dataTest, MappingTestRunnerContext context)
    {
        List<Pair<Connection, List<Closeable>>> connections = Lists.mutable.empty();
        try
        {
            Optional<InputStream> inputStream = buildInputStreamIfPossible(dataTest, context);
            if (!inputStream.isPresent())
            {
                throw new UnsupportedOperationException("Unable to build input stream with data from mapping data test " + "'" + dataTest.id + "'");
            }
            inputStream.ifPresent(stream ->  context.getExecuteBuilder().withInputAsStream(stream));
            return executeTestAssertions(dataTest,context.getExecuteBuilder());
        }
        catch (Exception e)
        {
            TestError testError = new TestError();
            testError.atomicTestId = dataTest.id;
            testError.error = e.toString();
            return testError;
        }
        finally
        {
            this.closeConnections(connections);
        }
    }

    private Optional<InputStream> buildInputStreamIfPossible(MappingDataTest test, MappingTestRunnerContext context)
    {
        if (test.storeTestData.size() == 1)
        {
            StoreTestData data = test.storeTestData.get(0);
            return this.factories.collect(f -> f.tryBuildInputStreamForStore(context.getPureModelContextData(), resolveStore(context.getPureModelContextData(), data.store), data.data)).select(Objects::nonNull).select(Optional::isPresent)
                    .collect(Optional::get).getFirstOptional();
        }
        return Optional.empty();
    }

    private <T extends MappingTest> List<T> validateMappingSuiteTests(MappingTestSuite suite, java.lang.Class<T> cls)
    {
        List<T> tests = Lists.mutable.empty();
        suite.tests.forEach(test ->
        {
           if (cls.isInstance(test))
           {
              tests.add((T) test);
           }
           else
           {
               throw new UnsupportedOperationException("Mapping Suite of type '" + suite.getClass().getName() + "' expects test to be of type '" +  cls.getName() + "'. Test '" + test.id + "' non comaptibale");
           }
        });
        return tests;
    }

    private List<Pair<Connection, List<Closeable>>> buildTestConnections(List<StoreTestData> mappingStoreTestData, MappingTestRunnerContext context)
    {
        PureModelContextData pureModelContextData = context.getPureModelContextData();
        List<Pair<String, EmbeddedData>> connectionInfo = mappingStoreTestData.stream().map(testData -> Tuples.pair(testData.store, EmbeddedDataHelper.resolveEmbeddedData(pureModelContextData, testData.data))).collect(Collectors.toList());
        List<Pair<Connection, List<Closeable>>> connections = connectionInfo.stream().map(pair -> this.factories.collect(f -> f.tryBuildTestConnectionsForStore(context.dataElements, resolveStore(pureModelContextData, pair.getOne()), pair.getTwo())).select(Objects::nonNull).select(Optional::isPresent)
                .collect(Optional::get).getFirstOptional().orElseThrow(() -> new UnsupportedOperationException("Unsupported store type for:'" + pair.getOne() + "' mentioned while running the mapping tests"))).collect(Collectors.toList());
        return connections;
    }

    private TestResult executeMappingFuncTest(MappingFunctionTest test, Root_meta_pure_runtime_Runtime_Impl runtime, MappingTestRunnerContext context)
    {
        try
        {
            PureModel pureModel = context.getPureModel();
            LambdaFunction<?> pureLambda = HelperValueSpecificationBuilder.buildLambda(test.func, new CompileContext.Builder(pureModel).build());
            return executeTestAssertions(test, runtime, pureLambda, context);
        }
        catch (Exception e)
        {
            TestError testError = new TestError();
            testError.atomicTestId = test.id;
            testError.error = e.toString();
            return testError;
        }
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

    private TestResult executeTestAssertions(MappingTest mappingTest, Root_meta_pure_runtime_Runtime_Impl runtime, LambdaFunction<?> pureLambda, MappingTestRunnerContext context)
    {
        SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(pureLambda, pureMapping, runtime, null, context.getPureModel(), this.pureVersion, PlanPlatform.JAVA, null, context.routerExtensions, context.executionPlanTransformers);
        return executeTestAssertions(mappingTest, context.getExecuteBuilder().withPlan(executionPlan));

    }

    private TestResult executeTestAssertions(MappingTest mappingTest,PlanExecutor.ExecuteArgsBuilder executeArgsBuilder)
    {
        try
        {
            PlanExecutor.ExecuteArgs executeArgs = executeArgsBuilder.build();
            List<AssertionStatus> assertionStatusList = Lists.mutable.empty();
            Result result = this.executor.executeWithArgs(executeArgs);
            if (result instanceof StreamingResult)
            {
                // We want to read streaming results only once
                result = new ConstantResult(((StreamingResult) result).flush(((StreamingResult) result).getSerializer(SerializationFormat.PURE)));
            }
            for (TestAssertion assertion : mappingTest.assertions)
            {
                assertionStatusList.add(assertion.accept(new TestAssertionEvaluator(result, SerializationFormat.PURE)));
            }
            TestExecuted testResult = new TestExecuted(assertionStatusList);
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
