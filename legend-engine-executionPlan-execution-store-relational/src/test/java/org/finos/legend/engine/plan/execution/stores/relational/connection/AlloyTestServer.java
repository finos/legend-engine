package org.finos.legend.engine.plan.execution.stores.relational.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.RelationalExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToJsonDefaultSerializer;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.pure.generated.core_relational_relational_router_router_extension;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import static org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope.buildTestExecutor;

public abstract class AlloyTestServer
{
    protected static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestRelationalExecutionStatistics.class);
    private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
    protected static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    protected static int serverPort;
    protected Server server;
    protected PlanExecutor planExecutor;
    protected RelationalExecutor testRelationalExecutor;


    @BeforeClass
    public static void setUpDriver()
    {
        try
        {
            Class.forName(DRIVER_CLASS_NAME);

            Enumeration<Driver> e = DriverManager.getDrivers();
            while (e.hasMoreElements())
            {
                Driver d = e.nextElement();
                if (!d.getClass().getName().equals(DRIVER_CLASS_NAME))
                {
                    try
                    {
                        DriverManager.deregisterDriver(d);
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    @Before
    public void setupServer() throws Exception
    {
        serverPort = DynamicPortGenerator.generatePort();
        server = AlloyH2Server.startServer(serverPort);
        insertTestData(serverPort);
        planExecutor = buildRelationalPlanExecutor();
        System.out.println("Finished setup");
    }

    @After
    public void tearDownServer() throws Exception
    {
        server.shutdown();
        server.stop();
        System.out.println("Teardown complete");
    }

    protected PlanExecutor buildRelationalPlanExecutor()
    {
        return PlanExecutor.newPlanExecutor(Relational.build(serverPort));
    }

    protected void insertTestData(int serverPort) throws SQLException
    {
        testRelationalExecutor = buildTestExecutor(serverPort);
        Connection testDBConnection = testRelationalExecutor.getConnectionManager().getTestDatabaseConnection();
        try (Statement statement = testDBConnection.createStatement())
        {
            insertTestData(statement);
        }
    }

    protected abstract void insertTestData(Statement statement) throws SQLException;

    protected String executePlan(SingleExecutionPlan singleExecutionPlan)
    {
        return  executePlan(singleExecutionPlan, Collections.emptyMap());
    }


    protected String executePlan(SingleExecutionPlan plan, Map<String, ?> params)
    {
        RelationalResult result = (RelationalResult)planExecutor.execute(plan, params, null);
        return result.flush(new RelationalResultToJsonDefaultSerializer(result));
    }

    protected SingleExecutionPlan buildPlan(String plan, String timeZone)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(plan);

        if (timeZone != null && !timeZone.isEmpty())
        {
            updateRuntimeWithTimeZone(contextData.getElementsOfType(PackageableRuntime.class).get(0), timeZone);
        }
        PureModel pureModel = Compiler.compile(contextData, null, null);
        Function fetchFunctionExpressions = contextData.getElementsOfType(Function.class).get(0);

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(((Lambda)fetchFunctionExpressions.body.get(0)).body, ((Lambda)fetchFunctionExpressions.body.get(0)).parameters, pureModel.getContext()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers
        );

    }


    protected SingleExecutionPlan buildPlan(String plan)
    {
        return buildPlan(plan, null);
    }

    private void updateRuntimeWithTimeZone(PackageableRuntime runtime, String timeZone)
    {
        ((DatabaseConnection)runtime.runtimeValue.connections.get(0).storeConnections.get(0).connection).timeZone = timeZone;
    }

}
