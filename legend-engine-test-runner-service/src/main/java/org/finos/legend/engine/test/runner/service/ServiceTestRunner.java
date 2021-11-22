// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.test.runner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.generation.ServicePlanGenerator;
import org.finos.legend.engine.language.pure.dsl.service.generation.extension.ServiceExecutionExtension;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemoryStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemoryStoreState;
import org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreState;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStoreState;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedSingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PureList;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;
import org.finos.legend.engine.test.runner.shared.TestResult;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionTest;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_relational_relational_helperFunctions_helperFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Class;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public class ServiceTestRunner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTestRunner.class);

    private final Service service;
    private final Root_meta_legend_service_metamodel_Service pureService;
    private final PureModelContextData pureModelContextData;
    private final PureModel pureModel;
    private final ObjectMapper objectMapper;
    private final PlanExecutor executor;
    private final RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions;
    private final MutableList<PlanTransformer> transformers;
    private final String pureVersion;

    public ServiceTestRunner(Service service, Root_meta_legend_service_metamodel_Service pureService, PureModelContextData pureModelContextData, PureModel pureModel, ObjectMapper objectMapper, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this.service = service;
        this.pureService = (pureService == null) ? findPureService(service, pureModel) : pureService;
        this.pureModelContextData = pureModelContextData;
        this.pureModel = pureModel;
        this.objectMapper = (objectMapper == null) ? ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports() : objectMapper;
        this.executor = executor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.pureVersion = pureVersion;
    }

    @Deprecated
    public ServiceTestRunner(Pair<Service, Root_meta_legend_service_metamodel_Service> pureServicePairs, Pair<PureModelContextData, PureModel> pureModelPairs, ObjectMapper objectMapper, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this(pureServicePairs.getOne(), pureServicePairs.getTwo(), pureModelPairs.getOne(), pureModelPairs.getTwo(), objectMapper, executor, extensions, transformers, pureVersion);
    }

    @Deprecated
    public ServiceTestRunner(Service service, Pair<PureModelContextData, PureModel> pureModelPairs, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this(service, null, pureModelPairs.getOne(), pureModelPairs.getTwo(), null, executor, extensions, transformers, pureVersion);
    }

    public List<RichServiceTestResult> executeTests() throws IOException, JavaCompileException
    {
        Execution serviceExecution = this.service.execution;
        if (serviceExecution instanceof PureMultiExecution)
        {
            List<RichServiceTestResult> results = Lists.mutable.empty();
            try (Scope scope = GlobalTracer.get().buildSpan("Generate Tests And Run For MultiExecution Service").startActive(true))
            {
                MutableMap<String, KeyedExecutionParameter> executionsByKey = Iterate.groupByUniqueKey(((PureMultiExecution) serviceExecution).executionParameters, e -> e.key);
                for (KeyedSingleExecutionTest es : ((MultiExecutionTest) service.test).tests)
                {
                    List<TestContainer> asserts = es.asserts;
                    KeyedExecutionParameter e = executionsByKey.get(es.key);

                    PureMultiExecution pureExecution = (PureMultiExecution) service.execution;
                    PureSingleExecution pureSingleExecution = new PureSingleExecution();
                    pureSingleExecution.func = pureExecution.func;
                    pureSingleExecution.mapping = e.mapping;
                    pureSingleExecution.runtime = e.runtime;
                    pureSingleExecution.executionOptions = e.executionOptions;

                    String noAssertMessage = "No test assert found for key - " + es.key + "!!";
                    RichServiceTestResult richServiceTestResult = executeSingleExecutionTest(pureSingleExecution, es.data, asserts, noAssertMessage, pureModelContextData, pureModel, scope);
                    richServiceTestResult.setOptionalMultiExecutionKey(es.key);
                    results.add(richServiceTestResult);
                }
                return results;
            }
        }
        else if (serviceExecution instanceof PureSingleExecution)
        {
            try (Scope scope = GlobalTracer.get().buildSpan("Generate Single Pure Tests And Run").startActive(true))
            {
                List<TestContainer> asserts = ((SingleExecutionTest) service.test).asserts;
                String noAssertMessage = "No test assert found !!";
                return Collections.singletonList(executeSingleExecutionTest((PureSingleExecution) service.execution, ((SingleExecutionTest) service.test).data, asserts, noAssertMessage, pureModelContextData, pureModel, scope));
            }
        }
        else
        {
            try (Scope scope = GlobalTracer.get().buildSpan("Generate Extra Service Execution Tests and Run").startActive(true))
            {
                MutableList<ServiceExecutionExtension> serviceExecutionExtensions = Lists.mutable.withAll(ServiceLoader.load(ServiceExecutionExtension.class));
                Pair<ExecutionPlan, RichIterable<? extends String>> testExecutor = getExtraServiceExecutionPlan(serviceExecutionExtensions, serviceExecution, ((Root_meta_legend_service_metamodel_SingleExecutionTest) this.pureService._test())._data());
                ExecutionPlan executionPlan = testExecutor.getOne();
                Assert.assertTrue(executionPlan instanceof SingleExecutionPlan, () -> "Only Single Execution Plan supported");
                List<TestContainer> containers = getExtraServiceTestContainers(serviceExecutionExtensions, service.test);
                return Collections.singletonList(executeTestAsserts((SingleExecutionPlan) executionPlan, containers, testExecutor.getTwo(), scope));
            }
        }
    }

    private RichServiceTestResult executeSingleExecutionTest(PureSingleExecution execution, String testData, List<TestContainer> asserts, String noAssertMessage, PureModelContextData pureModelContextData, PureModel pureModel, Scope scope) throws IOException, JavaCompileException
    {
        if (asserts == null || asserts.isEmpty())
        {
            scope.span().log(noAssertMessage);
            return new RichServiceTestResult(service.getPath(), Collections.emptyMap(), Collections.emptyMap(), null, null, null);
        }
        else
        {
            Runtime testRuntime = ServiceTestGenerationHelper.buildTestRuntime(execution.runtime, execution.mapping, testData, pureModelContextData, pureModel);
            RichIterable<? extends String> sqlStatements = extractSetUpSQLFromTestRuntime(testRuntime);
            PureSingleExecution testPureSingleExecution = shallowCopySingleExecution(execution);
            testPureSingleExecution.runtime = testRuntime;
            ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(testPureSingleExecution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, extensions, transformers);
            SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
            JavaHelper.compilePlan(singleExecutionPlan, null);
            return executeTestAsserts(singleExecutionPlan, asserts, sqlStatements, scope);
        }
    }

    private Pair<ExecutionPlan, RichIterable<? extends String>> getExtraServiceExecutionPlan(MutableList<ServiceExecutionExtension> extensions, Execution execution, String testData)
    {
        return extensions
                .collect(f -> f.tryToBuildTestExecutorContext(execution, testData, this.objectMapper, this.pureModel, this.extensions, this.transformers, this.pureVersion))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() -> new UnsupportedOperationException("Service execution class '" + execution.getClass().getName() + "' not supported yet"));
    }

    private List<TestContainer> getExtraServiceTestContainers(MutableList<ServiceExecutionExtension> extensions, ServiceTest test)
    {
        return extensions
                .collect(f -> f.tryToBuildTestAsserts(test, this.objectMapper, this.pureModel))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() -> new UnsupportedOperationException("Service test class '" + test.getClass().getName() + "' not supported yet"));
    }

    private static PureSingleExecution shallowCopySingleExecution(PureSingleExecution pureSingleExecution)
    {
        PureSingleExecution shallowCopy = new PureSingleExecution();
        shallowCopy.func = pureSingleExecution.func;
        shallowCopy.mapping = pureSingleExecution.mapping;
        shallowCopy.runtime = pureSingleExecution.runtime;
        return shallowCopy;
    }

    private RichServiceTestResult executeTestAsserts(SingleExecutionPlan executionPlan, List<TestContainer> asserts, RichIterable<? extends String> sqlStatements, Scope scope) throws IOException
    {
        if (ExecutionNodeTDSResultHelper.isResultTDS(executionPlan.rootExecutionNode) || (executionPlan.rootExecutionNode.isResultPrimitiveType() && "String".equals(executionPlan.rootExecutionNode.getDataTypeResultType())))
        {
            // Java
            String packageName = "org.finos.legend.tests.generated";
            String className = "TestSuite";
            String javaCode = ServiceTestGenerationHelper.generateJavaForAsserts(asserts, this.service, this.pureModel, packageName, className);
            Class<?> assertsClass;
            RichServiceTestResult testRun;
            try
            {
                assertsClass = compileJavaForAsserts(packageName, className, javaCode);
            }
            catch (JavaCompileException e)
            {
                throw new RuntimeException("Error compiling test asserts for " + this.service.getPath(), e);
            }

            scope.span().log("Java asserts generated and compiled");

            TestExecutionScope execScope = null;
            try
            {
                // Setup test database if needed
                if (sqlStatements != null)
                {
                    execScope = TestExecutionScope.setupTestServer(sqlStatements, scope);
                }

                // Run tests
                Map<String, TestResult> results = Maps.mutable.empty();
                Map<String, Exception> assertExceptions = Maps.mutable.empty();
                for (Pair<TestContainer, Integer> tc : LazyIterate.zipWithIndex(asserts))
                {
                    // Build Param Map
                    Map<String, Result> parameters = Maps.mutable.empty();
                    if (this.service.execution instanceof PureExecution)
                    {
                        parameters = ListIterate.zip(((PureExecution) this.service.execution).func.parameters, tc.getOne().parametersValues).toMap(
                                p -> p.getOne().name,
                                p -> p.getTwo() instanceof Collection   // Condition evoked in case of studio-flow
                                    ? new ConstantResult(
                                    ListIterate.collect(( (Collection) p.getTwo()).values, v -> v.accept(new ValueSpecificationToResultVisitor())))
                                    : p.getTwo() instanceof PureList   // Condition evoked in case of pureIDE-flow
                                    ? new ConstantResult(
                                    ListIterate.collect(( (PureList) p.getTwo()).values, v -> v.accept(new ValueSpecificationToResultVisitor())))
                                    : p.getTwo().accept(new ValueSpecificationToResultVisitor()));
                    }

                    // Execute Plan
                    ExecutionState testExecutionState = new ExecutionState(parameters,
                            Lists.mutable.withAll(executionPlan.templateFunctions),
                            Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(execScope == null ? -1 : execScope.getPort())), new InMemoryStoreExecutionState(new InMemoryStoreState()), new ServiceStoreExecutionState(new ServiceStoreState()))
                    );
                    Result result = this.executor.execute(executionPlan, testExecutionState, null, null);

                    org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result<Object> pureResult = result.accept(new ResultToPureResultVisitor());

                    // Execute Assert
                    String testName = ServiceTestGenerationHelper.getAssertMethodName(tc.getTwo());
                    scope.span().setTag(testName, resultToString(pureResult, this.pureModel.getExecutionSupport()));
                    TestResult testResult;
                    try
                    {
                        Boolean assertResult = (Boolean) assertsClass.getMethod(testName, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result.class, ExecutionSupport.class).invoke(null, pureResult, pureModel.getExecutionSupport());
                        testResult = assertResult ? TestResult.SUCCESS : TestResult.FAILURE;
                        scope.span().setTag(testName + "_assert", assertResult);
                    }
                    catch (Exception e)
                    {
                        StringWriter out = new StringWriter();
                        PrintWriter writer = new PrintWriter(out);
                        e.printStackTrace(writer);
                        e.printStackTrace();
                        testResult = TestResult.ERROR;
                        assertExceptions.put(testName, e);
                        scope.span().setTag(testName + "_assert", out.toString());
                    }
                    results.put(testName, testResult);
                }

                testRun = new RichServiceTestResult(service.getPath(), results, assertExceptions, null, executionPlan, javaCode);
                scope.span().log("Finished running tests " + results);
            }
            catch (Exception e)
            {
                LOGGER.error("Error running tests", e);
                throw (e instanceof RuntimeException) ? (RuntimeException) e : new RuntimeException(e);
            }
            finally
            {
                if (execScope != null)
                {
                    execScope.close();
                }
            }

            return testRun;
        }
        else
        {
            return new RichServiceTestResult(this.service.getPath(), Collections.emptyMap(), Collections.emptyMap(), null, executionPlan, "");
        }
    }

    private static Class<?> compileJavaForAsserts(String packageName, String className, String javaCode) throws JavaCompileException
    {
        EngineJavaCompiler compiler = new EngineJavaCompiler();
        compiler.compile(Collections.singletonList(StringJavaSource.newStringJavaSource(packageName, className, javaCode, false)));
        try
        {
            return compiler.getClassLoader().loadClass(packageName + "." + className);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Error finding " + packageName + "." + className + " after compiling:\n" + javaCode, e);
        }
    }


    private static String resultToString(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result<Object> pureResult, ExecutionSupport executionSupport)
    {
        Object value = pureResult._values().getAny();
        if (value instanceof org.finos.legend.pure.generated.Root_meta_pure_tds_TabularDataSet)
        {
            return core_relational_relational_helperFunctions_helperFunctions.Root_meta_csv_toCSV_TabularDataSet_1__Boolean_1__String_1_((org.finos.legend.pure.generated.Root_meta_pure_tds_TabularDataSet) value, true, executionSupport);
        }
        if (value instanceof String)
        {
            return (String) value;
        }
        throw new RuntimeException("To Code");
    }


    MutableList<String> extractSetUpSQLFromTestRuntime(Runtime runtime)
    {
        if (runtime instanceof LegacyRuntime)
        {
            return extractSetUpSQLFromConnections(((LegacyRuntime) runtime).connections);
        }
        if (runtime instanceof EngineRuntime)
        {
            return extractSetUpSQLFromConnections(LazyIterate.flatCollect(((EngineRuntime) runtime).connections, sc -> sc.storeConnections).collect(ic -> ic.connection));
        }
        return null;
    }

    private MutableList<String> extractSetUpSQLFromConnections(Iterable<? extends org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection> connections)
    {
        MutableList<String> results = null;
        for (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection connection : connections)
        {
            if (connection instanceof RelationalDatabaseConnection)
            {
                RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
                if (relationalDatabaseConnection.datasourceSpecification instanceof LocalH2DatasourceSpecification)
                {
                    LocalH2DatasourceSpecification localH2DatasourceSpecification = (LocalH2DatasourceSpecification) relationalDatabaseConnection.datasourceSpecification;
                    if (localH2DatasourceSpecification.testDataSetupSqls != null)
                    {
                        if (results == null)
                        {
                            results = Lists.mutable.withAll(localH2DatasourceSpecification.testDataSetupSqls);
                        }
                        else
                        {
                            results.addAll(localH2DatasourceSpecification.testDataSetupSqls);
                        }
                        localH2DatasourceSpecification.testDataSetupCsv = null;
                        localH2DatasourceSpecification.testDataSetupSqls = null;
                    }
                }
            }
        }
        return results;
    }

    private static Root_meta_legend_service_metamodel_Service findPureService(Service service, PureModel pureModel)
    {
        PackageableElement foundElement = pureModel.getPackageableElement(service.getPath());
        if (!(foundElement instanceof Root_meta_legend_service_metamodel_Service))
        {
            throw new RuntimeException("Could not find service '" + service.getPath() + "' in Pure model");
        }
        return (Root_meta_legend_service_metamodel_Service) foundElement;
    }
}