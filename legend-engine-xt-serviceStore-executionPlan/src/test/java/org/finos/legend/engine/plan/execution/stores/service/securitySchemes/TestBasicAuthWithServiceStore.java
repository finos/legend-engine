package org.finos.legend.engine.plan.execution.stores.service.securitySchemes;

import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestBasicAuthWithServiceStore extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("securitySchemes");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "    authSpecs: [\n" +
                        "        oauth     : OauthTokenGenerationSpecification\n" +
                        "              {\n" +
                        "                   grantType                   : 'ClientCredentials';\n"+
                        "                   clientId                    : 'testClientID';\n" +
                        "                   clientSecretVaultReference  : 'ref';\n" +
                        "                   authServerUrl               : 'dummy.com';\n" +
                        "              },\n" +
                        "       http : UsernamePasswordSpecification\n" +
                        "             {\n" +
                        "                   username : 'username';\n" +
                        "                   password : 'password';\n" +
                        "             }\n" +
                        "    ];\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/securitySchemes/testGrammar2.pure") + "\n\n" + serviceStoreConnection;
    }

    @Test
    public void serviceStoreSimpleExample()
    {

        PureModelContextData data = PureGrammarParser.newInstance().parseModel(pureGrammar);

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar);
        String result = executePlan(plan);
        System.out.println(result);
    }



}
