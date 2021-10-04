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


import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_relational_relational_helperFunctions_helperFunctions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.CompositeExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.url.DataProtocolHandler;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.valuespecification.ValueSpecificationProcessor;

import java.util.*;
import java.util.function.Function;
import javax.ws.rs.core.MediaType;

public class ServiceTestGenerationHelper
{

    public static CompositeExecutionPlan buildCompositeExecutionTestPlan(Service service, Map<String, Runtime> testRuntimes, PureModel pureModel, String pureVersion, PlanPlatform platform,RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        if (!(service.execution instanceof PureMultiExecution))
        {
            throw new IllegalArgumentException("Must be " + PureMultiExecution.class.getSimpleName() + ", got: " + service.execution.getClass());
        }
        PureMultiExecution pureExecution = (PureMultiExecution) service.execution;
        LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(pureExecution.func.body, pureExecution.func.parameters, pureModel.getContext(service));
        CompileContext compileContext = pureModel.getContext(service);

        List<String> keys = Lists.mutable.empty();
        Map<String, SingleExecutionPlan> plans = Maps.mutable.empty();
        LazyIterate.select(pureExecution.executionParameters, es -> testRuntimes.containsKey(es.key))
                .forEach(es ->
                {
                    keys.add(es.key);
                    Mapping pureMapping = pureModel.getMapping(es.mapping);
                    org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime pureRuntime = HelperRuntimeBuilder.buildPureRuntime(testRuntimes.get(es.key), compileContext);
                    SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(lambda, pureMapping, pureRuntime, null, pureModel, pureVersion, platform, null, extensions, transformers);
                    plans.put(es.key, plan);
                });
        return new CompositeExecutionPlan(plans, pureExecution.executionKey, keys);
    }

    public static Runtime buildSingleExecutionTestRuntime(PureSingleExecution pureSingleExecution, SingleExecutionTest singleExecutionTest, PureModelContextData pureModelContextData, PureModel pureModel)
    {
        return buildTestRuntime(pureSingleExecution.runtime, pureSingleExecution.mapping, singleExecutionTest.data, pureModelContextData, pureModel);
    }

    public static Map<String, Runtime> buildMultiExecutionTestRuntime(PureMultiExecution pureMultiExecution,MultiExecutionTest multiExecutionTest,  PureModelContextData pureModelContextData, PureModel pureModel)
    {
        MutableMap<String, KeyedExecutionParameter> executionsByKey = Iterate.groupByUniqueKey(pureMultiExecution.executionParameters, e -> e.key);
        return Iterate.toMap(multiExecutionTest.tests,
                t -> t.key,
                t ->
                {
                    KeyedExecutionParameter e = executionsByKey.get(t.key);
                    return buildTestRuntime(e.runtime, e.mapping, t.data, pureModelContextData, pureModel);
                });
    }

    private static Runtime buildTestRuntime(Runtime runtime, String mappingPath, String testData, PureModelContextData pureModelContextData, PureModel pureModel)
    {
        if (doesNotContainRelational(runtime, pureModel) || (hasMultipleConnection(runtime, pureModel) && !hasModelChainConnection(runtime,pureModelContextData)))
        {
            EngineRuntime engineRuntime = resolveRuntime(runtime, pureModelContextData);
            return buildMultipleConnectionRuntime(mappingPath, testData, engineRuntime, pureModelContextData,pureModel);
        }
        return buildRelationalTestRuntime(runtime, mappingPath, testData, pureModel);
    }

    private static Runtime buildMultipleConnectionRuntime(String mappingPath, String testData, EngineRuntime engineRuntime, PureModelContextData pureModelContextData, PureModel pureModel)
    {
        EngineRuntime runtime = new EngineRuntime();
        PackageableElementPointer mappingPointer = new PackageableElementPointer();
        mappingPointer.type = PackageableElementType.MAPPING;
        mappingPointer.path = mappingPath;
        runtime.mappings.add(mappingPointer);
        engineRuntime.mappings = runtime.mappings;
        Function<String, String> testDataAccessor = getTestDataAccessor(testData,engineRuntime);
        runtime.connections = ListIterate.collect(engineRuntime.connections, c->buildTestConnection(c,testDataAccessor,pureModelContextData,engineRuntime,mappingPath,pureModel,testData));
        return runtime;
    }

    private static StoreConnections buildTestConnection(StoreConnections storeConnections, Function<String, String> testDataAccessor, PureModelContextData pureModelContextData, EngineRuntime runtime, String mappingPath, PureModel pureModel, String testData)
    {
        StoreConnections newStoreConnections = new StoreConnections();
        newStoreConnections.store = storeConnections.store;
        newStoreConnections.storeConnections =ListIterate.collect(storeConnections.storeConnections,s-> newTestIdentifiedConnection(s,storeConnections,testDataAccessor,pureModelContextData,runtime,mappingPath,pureModel,testData));
        return newStoreConnections;
    }

    private static IdentifiedConnection newTestIdentifiedConnection(IdentifiedConnection identifiedConnection, StoreConnections storeConnections, Function<String, String> testDataAccessor, PureModelContextData pureModelContextData, EngineRuntime runtime, String mappingPath, PureModel pureModel, String testData) {
        Connection connection = identifiedConnection.connection;
        IdentifiedConnection newIdentifiedConnection = new IdentifiedConnection();
        newIdentifiedConnection.id = identifiedConnection.id;
        String idTestDataAccessorResult = testDataAccessor.apply(identifiedConnection.id);
        identifiedConnection.connection = getTestConnection(connection, runtime, mappingPath, Tuples.pair(pureModelContextData, pureModel), storeConnections, testDataAccessor, testData, idTestDataAccessorResult);
        return identifiedConnection;
    }

    private static boolean hasMultipleConnection(Runtime runtime, PureModel pureModel)
    {
        if (runtime instanceof EngineRuntime)
        {
            List<StoreConnections> storeConnections = ((EngineRuntime) runtime).connections;
            return (storeConnections != null) && !storeConnections.isEmpty() && storeConnections.size()>1;
        }
        if (runtime instanceof LegacyRuntime)
        {
            List<Connection> connections = ((LegacyRuntime) runtime).connections;
            return (connections != null) && !connections.isEmpty() && connections.size()>1 ;
        }
        if (runtime instanceof RuntimePointer)
        {
            RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection> connections = pureModel.getRuntime(((RuntimePointer) runtime).runtime)._connections();
            return (connections != null) && connections.size()>1;
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }

    private static boolean hasModelChainConnection(Runtime runtime, PureModelContextData pureModelContextData)
    {
        EngineRuntime engineRuntime = resolveRuntime(runtime,pureModelContextData);
        List<StoreConnections> storeConnections = engineRuntime.connections;
        for(StoreConnections s: storeConnections)
        {
            List<IdentifiedConnection> identifiedConnection = s.storeConnections;
            for(IdentifiedConnection ic : identifiedConnection)
            {
                if(ic.connection instanceof ModelChainConnection || (("ModelStore").equals(s.store.path) && ic.connection instanceof ConnectionPointer))
                    return true;
            }
        }
        return false;
    }

    private static EngineRuntime resolveRuntime(Runtime runtime, PureModelContextData pureModelContextData)
    {
        if (runtime instanceof EngineRuntime)
        {
            return (EngineRuntime) runtime;
        }
        if (runtime instanceof LegacyRuntime)
        {
            return ((LegacyRuntime) runtime).toEngineRuntime();
        }
        if (runtime instanceof RuntimePointer)
        {
            String runtimeFullPath = ((RuntimePointer) runtime).runtime;
            PackageableElement found = Iterate.detect(pureModelContextData.getElements(), e -> runtimeFullPath.equals(e.getPath()));
            if (!(found instanceof PackageableRuntime))
            {
                throw new RuntimeException("Can't find runtime '" + runtimeFullPath + "'");
            }
            return ((PackageableRuntime) found).runtimeValue;
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }

    private static Function<String, String> getTestDataAccessor(String testData, EngineRuntime runtime)
    {
        if (runtime.connections.size() > 1)
        {
            try
            {
                Map<String, String> testDataMap = ObjectMapperFactory.getNewStandardObjectMapper().readValue(testData, new TypeReference<Map<String, String>>()
                {
                });
                return testDataMap::get;
            }
            catch (JsonProcessingException e)
            {
                throw new IllegalArgumentException("Unable to deserialize test data for multiple connections from JSON to a Map<String,String>: " + testData);
            }
        }
        else
        {
            return id -> testData;
        }
    }

    private static Connection getTestConnection(Connection connection, Runtime parentRuntime, String mappingPath,  Pair<PureModelContextData, PureModel> pureModelPairs,StoreConnections parentStoreConnection, Function<String, String> testDataAccessor,  String testData, String idTestDataAccessorResult)
    {
        Optional<Connection> optionalConnection = getNullableTestConnection(connection, parentRuntime, mappingPath, pureModelPairs, parentStoreConnection,testDataAccessor,  null, idTestDataAccessorResult);
        if(optionalConnection.isPresent()){
            return optionalConnection.get();
        }
        else
        {
            throw new UnsupportedOperationException();
        }
    }

    private static Optional<Connection> getNullableTestConnection(Connection connection, Runtime parentRuntime, String mappingPath,  Pair<PureModelContextData, PureModel> pureModelPairs,StoreConnections parentStoreConnection, Function<String, String> testDataAccessor,  String testData, String idTestDataAccessorResult)
    {
        if (connection instanceof ConnectionPointer)
        {
            String connectionFullPath = ((ConnectionPointer) connection).connection;
            PackageableElement found = Iterate.detect(pureModelPairs.getOne().getElements(), e -> connectionFullPath.equals(e.getPath()));
            if (!(found instanceof PackageableConnection))
            {
                throw new RuntimeException("Can't find connection '" + connectionFullPath + "'");
            }
            connection = ((PackageableConnection) found).connectionValue;
        }
        if (connection instanceof JsonModelConnection)
        {
            JsonModelConnection conn = (JsonModelConnection) connection;
            JsonModelConnection testJsonModelConnection = new JsonModelConnection();
            testJsonModelConnection._class = conn._class;
            testJsonModelConnection.element = conn.element != null ? conn.element : (parentStoreConnection.store != null ? parentStoreConnection.store.path : null);
            String executorId = conn.url.split(":")[1];
            String connectionTestData = resolveTestData(executorId, idTestDataAccessorResult, testDataAccessor, testData);
            if(idTestDataAccessorResult != null){
                ((JsonModelConnection) connection).url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_JSON + ";base64," + Base64.getEncoder().encodeToString(idTestDataAccessorResult.getBytes());
            }
            testJsonModelConnection.url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_JSON + ";base64," + Base64.getEncoder().encodeToString(connectionTestData.getBytes());
            return Optional.of(conn);
        }
        else if (connection instanceof XmlModelConnection)
        {
            XmlModelConnection conn = (XmlModelConnection) connection;
            XmlModelConnection testXmlModelConnection = new XmlModelConnection();
            testXmlModelConnection._class = conn._class;
            testXmlModelConnection.element = conn.element != null ? conn.element : (parentStoreConnection.store != null ? parentStoreConnection.store.path : null);
            String executorId = conn.url.split(":")[1];
            String connectionTestData = resolveTestData(executorId, idTestDataAccessorResult, testDataAccessor, testData);
            if(idTestDataAccessorResult != null)
            {
                ((XmlModelConnection) connection).url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_XML + ";base64," + Base64.getEncoder().encodeToString(idTestDataAccessorResult.getBytes());
            }
            testXmlModelConnection.url = DataProtocolHandler.DATA_PROTOCOL_NAME + ":" + MediaType.APPLICATION_XML + ";base64," + Base64.getEncoder().encodeToString(connectionTestData.getBytes());
            return Optional.of(testXmlModelConnection);
        }
        else if(connection instanceof  ModelChainConnection)
        {
            return  Optional.of(connection);
        }
        else if(connection instanceof DatabaseConnection)
        {
            List<String> sql = getSql(parentRuntime, mappingPath, idTestDataAccessorResult , pureModelPairs.getTwo());
            return  Optional.of(newRelationalConnection(connection, idTestDataAccessorResult, sql));
        }
        String element = connection.element != null ? connection.element : (parentStoreConnection.store != null ? parentStoreConnection.store.path : null);
        Connection testConnectionFromFactories = getTestConnectionFromFactories(connection, idTestDataAccessorResult == null ? testData : idTestDataAccessorResult, element);
        return testConnectionFromFactories != null ? Optional.of(testConnectionFromFactories) : Optional.empty();
    }

    private static Connection getTestConnectionFromFactories(Connection connection, String testData, String element)
    {
        MutableList<ConnectionFactoryExtension> factories = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
        return  factories.asLazy()
            .collect(f -> f.tryBuildFromConnection(connection, testData, element))
            .select(Optional::isPresent)
            .collect(Optional::get)
            .getFirst();
    }



    private static String resolveTestData(String executorId,  String idConnectionTestData, Function<String, String> testDataAccessor,  String testData)
    {
        if(idConnectionTestData != null){
            return idConnectionTestData;
        }
        if(testDataAccessor != null)
        {
            String testDataAccessorString = testDataAccessor.apply(executorId);
            if(testDataAccessorString != null)
            {
                return testDataAccessorString;
            }
        }
        return testData;
    }

    private static boolean doesNotContainRelational(Runtime runtime, PureModel pureModel)
    {
        if (runtime instanceof EngineRuntime)
        {
            List<StoreConnections> storeConnections = ((EngineRuntime) runtime).connections;

            return (storeConnections.size() == 1) && Iterate.allSatisfy(storeConnections.get(0).storeConnections, identifiedConnection -> !(identifiedConnection.connection instanceof DatabaseConnection) && !(identifiedConnection.connection instanceof ConnectionPointer));
        }
        if (runtime instanceof LegacyRuntime)
        {
            List<Connection> connections = ((LegacyRuntime) runtime).connections;
            return (connections != null) && (connections.size() == 1) && !((connections.get(0) instanceof DatabaseConnection));
        }
        if (runtime instanceof RuntimePointer)
        {
            List<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection> connections = pureModel.getRuntime(((RuntimePointer) runtime).runtime)._connections().toList();
            return (connections != null) && (connections.size() == 1) && !(connections.get(0) instanceof org.finos.legend.pure.m3.coreinstance.meta.relational.runtime.DatabaseConnection);
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }
    // Relational

    private static Runtime buildRelationalTestRuntime(Runtime runtime, String mappingPath, String testData, PureModel pureModel)
    {
        List<String> sql = getSql(runtime, mappingPath, testData, pureModel);
        return buildRelationalTestRuntime(runtime, mappingPath, testData, sql);
    }

    private static MutableList<String> getSql(Runtime runtime, String mappingPath, String testData, PureModel pureModel)
    {
        return Lists.mutable.withAll(core_relational_relational_helperFunctions_helperFunctions.Root_meta_relational_functions_database_setUpData_String_1__Mapping_MANY__Runtime_1__String_MANY_(
                testData,
                Lists.immutable.with(pureModel.getMapping(mappingPath)),
                HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext()),
                pureModel.getExecutionSupport()));
    }

    private static Runtime buildRelationalTestRuntime(Runtime runtime, String mappingPath, String testDataCsv, List<String> sql)
    {
        if (runtime instanceof LegacyRuntime)
        {
            LegacyRuntime newRuntime = new LegacyRuntime();
            newRuntime.connections = ListIterate.collect(((LegacyRuntime) runtime).connections, c -> newRelationalConnection(c, testDataCsv, sql));
            return newRuntime;
        }
        if (runtime instanceof EngineRuntime)
        {
            EngineRuntime testRuntime = new EngineRuntime();
            PackageableElementPointer mappingPointer = new PackageableElementPointer();
            mappingPointer.type = PackageableElementType.MAPPING;
            mappingPointer.path = mappingPath;
            testRuntime.mappings.add(mappingPointer);
            testRuntime.connections = ListIterate.collect(((EngineRuntime) runtime).connections, c -> "ModelStore".equals(c.store.path) ? c : newRelationalStoreConnections(c, testDataCsv, sql));
            return testRuntime;
        }
        if (runtime instanceof RuntimePointer)
        {
            return runtime;
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }

    private static StoreConnections newRelationalStoreConnections(StoreConnections storeConnections, String testDataCsv, List<String> sql)
    {
        StoreConnections newStoreConnections = new StoreConnections();
        newStoreConnections.store = storeConnections.store;
        newStoreConnections.storeConnections = ListIterate.collect(storeConnections.storeConnections, c -> newRelationalIdentifiedConnection(c, testDataCsv, sql));
        return newStoreConnections;
    }

    private static IdentifiedConnection newRelationalIdentifiedConnection(IdentifiedConnection identifiedConnection, String testDataCsv, List<String> sql)
    {
        Connection newConnection = newRelationalConnection(identifiedConnection.connection, testDataCsv, sql);
        if (newConnection == identifiedConnection.connection)
        {
            return identifiedConnection;
        }

        IdentifiedConnection newIdentifiedConnection = new IdentifiedConnection();
        newIdentifiedConnection.id = identifiedConnection.id;
        newIdentifiedConnection.connection = newConnection;
        return newIdentifiedConnection;
    }

    private static Connection newRelationalConnection(Connection connection, String testDataCsv, List<String> sql)
    {
        if (connection instanceof ModelChainConnection)
        {
            return connection;
        }
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection();
        relationalDatabaseConnection.type = DatabaseType.H2;
        relationalDatabaseConnection.element = connection.element;
        if (connection instanceof DatabaseConnection)
        {
            relationalDatabaseConnection.timeZone = ((DatabaseConnection) connection).timeZone;
        }

        LocalH2DatasourceSpecification localH2DatasourceSpecification = new LocalH2DatasourceSpecification();
        localH2DatasourceSpecification.testDataSetupCsv = testDataCsv;
        localH2DatasourceSpecification.testDataSetupSqls = sql;
        relationalDatabaseConnection.datasourceSpecification = localH2DatasourceSpecification;
        relationalDatabaseConnection.authenticationStrategy = new TestDatabaseAuthenticationStrategy();
        return relationalDatabaseConnection;
    }

    // Java

    public static String generateJavaForAsserts(List<TestContainer> asserts, Service service, PureModel pureModel, String packageName, String className)
    {
        CompileContext compileContext = pureModel.getContext(service);
        ProcessorSupport processorSupport = pureModel.getExecutionSupport().getProcessorSupport();
        ProcessorContext processorContext = new ProcessorContext(processorSupport, false);
        processorContext.setInLineAllLambda(true);
        return "package " + packageName + ";\n" +
                "\n" +
                "import org.eclipse.collections.api.RichIterable;\n" +
                "import org.eclipse.collections.api.list.ListIterable;\n" +
                "import org.eclipse.collections.impl.factory.Lists;\n" +
                "import org.eclipse.collections.api.map.MutableMap;\n" +
                "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                "import org.eclipse.collections.impl.factory.Maps;\n" +
                "import org.finos.legend.pure.generated.*;\n" +
                "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;\n" +
                "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
                "import java.util.Map;" +
                "" +
                "public class " + className + "\n" +
                "{\n" +
                "    private static Map localLambdas = UnifiedMap.newMap();\n" +
                LazyIterate.collect(asserts, tc -> (InstanceValue) tc._assert.accept(new ValueSpecificationBuilder(compileContext, Lists.mutable.empty(), new ProcessingContext(""))))
                        .collect(vs -> ValueSpecificationProcessor.createFunctionForLambda(null, (CoreInstance) vs._values().getAny(), processorSupport, processorContext))
                        .zipWithIndex()
                        .collect(tuple ->
                                "    public static boolean " + getAssertMethodName(tuple.getTwo()) + "(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Result _res, ExecutionSupport es)\n" +
                                        "    {\n" +
                                        "        return " + tuple.getOne() + ".value(_res, es);\n" +
                                        "    }\n")
                        .makeString("", "\n", "}");
    }

    public static String getAssertMethodName(int assertNumber)
    {
        return "test" + assertNumber;
    }
}

