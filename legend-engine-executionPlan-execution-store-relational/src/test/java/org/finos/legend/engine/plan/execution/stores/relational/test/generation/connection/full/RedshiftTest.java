package org.finos.legend.engine.plan.execution.stores.relational.test.generation.connection.full;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
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
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.pure.generated.core_relational_relational_router_router_extension;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import static org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope.buildTestExecutor;

public class RedshiftTest
{
    private static PlanExecutor planExecutor;

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "  birthTime: DateTime[0..1];\n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table PERSON (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100),\n" +
            "    birthTime TIMESTAMP\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person: Relational\n" +
            "  {\n" +
            "    ~primaryKey\n" +
            "    (\n" +
            "      [test::DB]PERSON.fullName\n" +
            "    )\n" +
            "    ~mainTable [test::DB]PERSON\n" +
            "    fullName: [test::DB]PERSON.fullName,\n" +
            "    birthTime: [test::DB]PERSON.birthTime\n" +
            "  }\n" +
            ")\n\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: Redshift;\n" +
            "          specification: Redshift\n" +
            "          {\n" +
            "            clusterID: 'clusterID';\n" +
            "            clusterName: 'clusterName';\n" +
            "            name: 'dev';\n" +
            "            port: 5439;\n" +
            "            region: 'region';\n" +
            "          };\n" +
            "          auth: UserPassword\n" +
            "          {\n" +
            "            userName: 'userName';\n" +
            "            passwordVaultReference: 'password';\n" +
            "          };\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";
    @BeforeClass
    public static void setUp()
    {
        try
        {
            Class.forName("com.amazon.redshift.jdbc42.Driver");

            Enumeration<Driver> e = DriverManager.getDrivers();
            while (e.hasMoreElements())
            {
                Driver d = e.nextElement();
                if (!d.getClass().getName().equals("com.amazon.redshift.jdbc42.Driver"))
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

            Properties properties = new Properties();
            properties.load(new FileInputStream("../legend-engine-server/src/test/resources/org/finos/legend/engine/server/test/redshift.properties"));
            Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));

            int port = DynamicPortGenerator.generatePort();
            planExecutor = PlanExecutor.newPlanExecutor(Relational.build(port));
            System.out.println("Finished setup");
        }
        catch (Exception ignored)
        {
        }
    }

    @Test
    public void testInExecutionWithStringList()
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {names:String[*] | test::Person.all()\n" +
                "                        ->filter(p:test::Person[1] | $p.fullName->in($names))\n" +
                "                        ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("names", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("names", Lists.mutable.with("P1"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("names", Lists.mutable.with("P1", "P2"));

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".fullName in ('P1','P2')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithIntegerList()
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {nameLengths:Integer[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.fullName->length()->in($nameLengths))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("nameLengths", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("nameLengths", Lists.mutable.with(2));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("nameLengths", Lists.mutable.with(2, 3));

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]},{\"values\": [\"P3\"]},{\"values\": [\"P4\"]},{\"values\": [\"P5\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where char_length(\\\"root\\\".fullName) in (2,3)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P10\"]},{\"values\": [\"P2\"]},{\"values\": [\"P3\"]},{\"values\": [\"P4\"]},{\"values\": [\"P5\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithDateTimeList()
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {birthTime:DateTime[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.birthTime->in($birthTime))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, false);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("birthTime", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-12 20:00:00"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-12 20:00:00", "2020-12-13 20:00:00"));

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (Timestamp'2020-12-12 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (Timestamp'2020-12-12 20:00:00',Timestamp'2020-12-13 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    @Test
    public void testInExecutionWithDateTimeListAndTimeZone()
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): Any[1]\n" +
                "{\n" +
                "  {birthTime:DateTime[*] | test::Person.all()\n" +
                "                               ->filter(p:test::Person[1] | $p.birthTime->in($birthTime))\n" +
                "                               ->project([x | $x.fullName], ['fullName'])}\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction, true);
        Map<String, ?> paramWithEmptyList = Maps.mutable.with("birthTime", Lists.mutable.empty());
        Map<String, ?> paramWithSingleValue = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-13 03:00:00"));
        Map<String, ?> paramWithMultipleValues = Maps.mutable.with("birthTime", Lists.mutable.with("2020-12-13 03:00:00", "2020-12-14 03:00:00"));

        String expectedResWithEmptyList = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (null)\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : []}}";
        String expectedResWithSingleValue = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (Timestamp'2020-12-12 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]}]}}";
        String expectedResWithMultipleValues = "{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"fullName\",\"type\":\"String\",\"relationalType\":\"VARCHAR(100)\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select \\\"root\\\".fullName as \\\"fullName\\\" from PERSON as \\\"root\\\" where \\\"root\\\".birthTime in (Timestamp'2020-12-12 20:00:00',Timestamp'2020-12-13 20:00:00')\"}], \"result\" : {\"columns\" : [\"fullName\"], \"rows\" : [{\"values\": [\"P1\"]},{\"values\": [\"P2\"]}]}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, paramWithEmptyList));
        Assert.assertEquals(expectedResWithSingleValue, executePlan(plan, paramWithSingleValue));
        Assert.assertEquals(expectedResWithMultipleValues, executePlan(plan, paramWithMultipleValues));
    }

    private String executePlan(SingleExecutionPlan plan, Map<String, ?> params)
    {
        RelationalResult result = (RelationalResult) planExecutor.execute(plan, params, null);
        return result.flush(new RelationalResultToJsonDefaultSerializer(result));
    }

    private SingleExecutionPlan buildPlanForFetchFunction(String fetchFunction, boolean withTimeZone)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction);
        if(withTimeZone)
        {
            updateRuntimeWithArizonaTimeZone(contextData.getElementsOfType(PackageableRuntime.class).get(0));
        }
        PureModel pureModel = Compiler.compile(contextData, null, null);
        Function fetchFunctionExpressions = contextData.getElementsOfType(Function.class).get(0);

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(((Lambda) fetchFunctionExpressions.body.get(0)).body, ((Lambda) fetchFunctionExpressions.body.get(0)).parameters, pureModel.getContext()),
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

    private static void updateRuntimeWithArizonaTimeZone(PackageableRuntime runtime)
    {
        ((DatabaseConnection) runtime.runtimeValue.connections.get(0).storeConnections.get(0).connection).timeZone = "US/Arizona";
    }

}
