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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PackageableElementFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.generation.ServicePlanGenerator;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.language.pure.dsl.service.generation.extension.ServiceExecutionExtension;
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
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.*;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.finos.legend.engine.shared.javaCompiler.StringJavaSource;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Service;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_SingleExecutionTest;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_relational_relational_helperFunctions_helperFunctions;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Legend Execution Server: Service Test Runner");

    private final Pair<Service, Root_meta_legend_service_metamodel_Service> pureServicePairs;
    private final Pair<PureModelContextData, PureModel> pureModelPairs;
    private final ObjectMapper objectMapper;
    private final PlanExecutor executor;
    private final RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions;
    private final MutableList<PlanTransformer> transformers;
    private final String pureVersion;

    public ServiceTestRunner(Pair<Service, Root_meta_legend_service_metamodel_Service> pureServicePairs, Pair<PureModelContextData, PureModel> pureModelPairs, ObjectMapper objectMapper, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this.pureServicePairs = pureServicePairs;
        this.pureModelPairs = pureModelPairs;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.extensions = extensions;
        this.transformers = transformers;
        this.pureVersion = pureVersion;
    }

    public ServiceTestRunner(Service service, Pair<PureModelContextData, PureModel> pureModelPairs, PlanExecutor executor, RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, MutableList<PlanTransformer> transformers, String pureVersion)
    {
        this(Tuples.pair(service,(Root_meta_legend_service_metamodel_Service) service.accept(new PackageableElementFirstPassBuilder(pureModelPairs.getTwo().getContext(service)))), pureModelPairs, ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports(), executor, extensions, transformers, pureVersion);
    }

    public List<RichServiceTestResult> executeTests() throws IOException, JavaCompileException
    {
        Service service = pureServicePairs.getOne();
        Root_meta_legend_service_metamodel_Service pureService = pureServicePairs.getTwo();
        PureModelContextData pureModelContextData = pureModelPairs.getOne();
        PureModel pureModel = pureModelPairs.getTwo();
        Execution serviceExecution = service.execution;
        if (serviceExecution instanceof PureMultiExecution)
        {
            List<RichServiceTestResult> results = org.eclipse.collections.api.factory.Lists.mutable.empty();
            try (Scope scope = GlobalTracer.get().buildSpan("Generate Tests And Run For MultiExecution Service").startActive(true))
            {
                Map<String, Runtime> runtimeMap = ServiceTestGenerationHelper.buildMultiExecutionTestRuntime((PureMultiExecution)serviceExecution, (MultiExecutionTest) service.test, pureModelContextData, pureModel);
                Map<String, MutableList<String>> sqlStatementsByKey = Maps.mutable.empty();
                runtimeMap.forEach((key, runtime) ->
                {
                    MutableList<String> sql = extractSetUpSQLFromTestRuntime(runtime);
                    if (sql != null)
                    {
                        sqlStatementsByKey.put(key, sql);
                    }
                });
                CompositeExecutionPlan compositeExecutionPlan = ServiceTestGenerationHelper.buildCompositeExecutionTestPlan(service, runtimeMap, pureModel, pureVersion, PlanPlatform.JAVA, extensions, transformers);
                Map<String, SingleExecutionPlan> plansByKey = compositeExecutionPlan.executionPlans;
                for (SingleExecutionPlan plan : plansByKey.values())
                {
                    JavaHelper.compilePlan(plan, null);
                }

                for (KeyedSingleExecutionTest es : ((MultiExecutionTest) service.test).tests)
                {
                    SingleExecutionPlan executionPlan = plansByKey.get(es.key);
                    List<TestContainer> asserts = es.asserts;
                    RichIterable<? extends String> sqls = sqlStatementsByKey.get(es.key);
                    RichServiceTestResult richServiceTestResult = executeTestAsserts(executionPlan, asserts, sqls, scope);
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
                PureSingleExecution pureSingleExecution = (PureSingleExecution) service.execution;
                Runtime testRuntime = ServiceTestGenerationHelper.buildSingleExecutionTestRuntime((PureSingleExecution) service.execution, (SingleExecutionTest) service.test, pureModelContextData, pureModel);
                RichIterable<? extends String> sqlStatements = extractSetUpSQLFromTestRuntime(testRuntime);
                PureSingleExecution testPureSingleExecution = shallowCopySingleExecution(pureSingleExecution);
                testPureSingleExecution.runtime = testRuntime;
                ExecutionPlan executionPlan = ServicePlanGenerator.generateExecutionPlan(testPureSingleExecution, null, pureModel, pureVersion, PlanPlatform.JAVA, null, extensions, transformers);
                SingleExecutionPlan singleExecutionPlan = (SingleExecutionPlan) executionPlan;
                JavaHelper.compilePlan(singleExecutionPlan, null);
                List<TestContainer> asserts = ((SingleExecutionTest) service.test).asserts;
                return Collections.singletonList(executeTestAsserts(singleExecutionPlan, asserts, sqlStatements, scope));
            }
        }
        else {
            try (Scope scope = GlobalTracer.get().buildSpan("Generate Extra Service Execution Tests and Run").startActive(true))
            {
                MutableList<ServiceExecutionExtension> serviceExecutionExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ServiceExecutionExtension.class));
                Pair<ExecutionPlan, RichIterable<? extends  String>> testExecutor = getExtraServiceExecutionPlan(serviceExecutionExtensions, serviceExecution, ((Root_meta_legend_service_metamodel_SingleExecutionTest) pureService._test())._data());
                ExecutionPlan executionPlan = testExecutor.getOne();
                Assert.assertTrue(executionPlan instanceof SingleExecutionPlan, () -> "Only Single Execution Plan supported");
                List<TestContainer> containers = getExtraServiceTestContainers(serviceExecutionExtensions, service.test);
                return Collections.singletonList(executeTestAsserts((SingleExecutionPlan)executionPlan, containers, testExecutor.getTwo(), scope));
            }
        }
    }

    private Pair<ExecutionPlan, RichIterable<? extends String>> getExtraServiceExecutionPlan(MutableList<ServiceExecutionExtension> extensions, Execution execution, String testData)
    {
        return extensions
                .collect(f -> f.tryToBuildTestExecutorContext(execution, testData, this.objectMapper, this.pureModelPairs.getTwo(), this.extensions, this.transformers, this.pureVersion))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() -> new UnsupportedOperationException("Service execution class '" + execution.getClass().getName() + "' not supported yet"));
    }

    private List<TestContainer> getExtraServiceTestContainers(MutableList<ServiceExecutionExtension> extensions, ServiceTest test)
    {
        return extensions
                .collect(f -> f.tryToBuildTestAsserts(test, this.objectMapper, this.pureModelPairs.getTwo()))
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
        if (ExecutionNodeTDSResultHelper.isResultTDS(executionPlan.rootExecutionNode) || (executionPlan.rootExecutionNode.isResultPrimitiveType() && executionPlan.rootExecutionNode.getDataTypeResultType().equals("String")))
        {
            // Java
            String packageName = "org.finos.legend.tests.generated";
            String className = "TestSuite";
            Service service = pureServicePairs.getOne();
            PureModel pureModel = pureModelPairs.getTwo();
            String javaCode = ServiceTestGenerationHelper.generateJavaForAsserts(asserts, service, pureModel, packageName, className);
            Class<?> assertsClass;
            RichServiceTestResult testRun;
            try
            {
                assertsClass = compileJavaForAsserts(packageName, className, javaCode);
            }
            catch (JavaCompileException e)
            {
                throw new RuntimeException("Error compiling test asserts for " + service.getPath(), e);
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
                Map<String, org.finos.legend.engine.test.runner.shared.TestResult> results = org.eclipse.collections.api.factory.Maps.mutable.empty();
                Map<String, Exception> assertExceptions = org.eclipse.collections.api.factory.Maps.mutable.empty();
                for (Pair<TestContainer, Integer> tc : LazyIterate.zipWithIndex(asserts))
                {
                    // Build Param Map
                    Map<String, Result>  parameters = Maps.mutable.empty();
                    if (service.execution instanceof PureExecution)
                    {
                        parameters = ListIterate.zip(((PureExecution) service.execution).func.parameters, tc.getOne().parametersValues).toMap(
                                p -> p.getOne().name,
                                p -> p.getTwo() instanceof Collection
                                    ? new ConstantResult(
                                    ListIterate.collect(( (Collection) p.getTwo()).values, v -> primitiveInstanceValueCollector(v).getValue()))
                                    : primitiveInstanceValueCollector(p.getTwo()));
                    }

                    // Execute Plan
                    ExecutionState testExecutionState = new ExecutionState(parameters,
                            Lists.mutable.withAll(executionPlan.templateFunctions),
                            Lists.mutable.with(new RelationalStoreExecutionState(new RelationalStoreState(execScope == null ? -1 : execScope.getPort())), new InMemoryStoreExecutionState(new InMemoryStoreState()), new ServiceStoreExecutionState(new ServiceStoreState()))
                    );
                    Result result = executor.execute(executionPlan, testExecutionState, null, null);

                    org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result<Object> pureResult = result.accept(new ResultToPureResultVisitor());

                    // Execute Assert
                    String testName = ServiceTestGenerationHelper.getAssertMethodName(tc.getTwo());
                    scope.span().setTag(testName, resultToString(pureResult, pureModel.getExecutionSupport()));
                    org.finos.legend.engine.test.runner.shared.TestResult testResult;
                    try
                    {
                        Boolean assertResult = (Boolean) assertsClass.getMethod(testName, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result.class, ExecutionSupport.class).invoke(null, pureResult, pureModel.getExecutionSupport());
                        testResult = assertResult ? org.finos.legend.engine.test.runner.shared.TestResult.SUCCESS : org.finos.legend.engine.test.runner.shared.TestResult.FAILURE;
                        scope.span().setTag(testName + "_assert", assertResult);
                    }
                    catch (Exception e)
                    {
                        StringWriter out = new StringWriter();
                        PrintWriter writer = new PrintWriter(out);
                        e.printStackTrace(writer);
                        e.printStackTrace();
                        testResult = org.finos.legend.engine.test.runner.shared.TestResult.ERROR;
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
                throw new RuntimeException(e);
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
            return new RichServiceTestResult(pureServicePairs.getOne().getPath(),Collections.emptyMap(), Collections.emptyMap(), null,   executionPlan, "");
        }
    }

    private ConstantResult primitiveInstanceValueCollector(ValueSpecification instance)
    {
        return new ConstantResult(
        instance instanceof CBoolean
        ? ((CBoolean) instance).values.get(0)
        : instance instanceof CString
        ? ((CString)instance).values.get(0)
        : instance instanceof CInteger
        ? ((CInteger) instance).values.get(0)
        : instance instanceof CFloat
        ? ((CFloat) instance).values.get(0)
        : instance instanceof CDateTime
        ? ((CDateTime) instance).values.get(0)
        : instance instanceof CStrictDate
        ? ((CStrictDate) instance).values.get(0)
        : instance instanceof CStrictTime
        ? ((CStrictTime) instance).values.get(0)
        : instance instanceof EnumValue
        ? ((CString) instance).values.get(0)
        : null
        );
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
        else
        {
            throw new RuntimeException("To Code");
        }
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
            if(connection instanceof RelationalDatabaseConnection)
            {
                RelationalDatabaseConnection relationalDatabaseConnection = (RelationalDatabaseConnection) connection;
                if(relationalDatabaseConnection.datasourceSpecification instanceof LocalH2DatasourceSpecification)
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

}