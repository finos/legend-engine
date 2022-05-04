package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AwsPKAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_DelegatedKerberosAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AwsOAuthAuthenticationStrategy_Impl;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestRelationalConnectionCompilationRoundtrip
{
    @Test
    public void testConnectionPropertiesPropagatedToCompiledGraph()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: true;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: DelegatedKerberos\n" +
                "  {\n" +
                "    serverPrincipal: 'dummyPrincipal';" +
                "  };\n" +
                "}\n");
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection connection = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) compiledGraph.getTwo().getConnection("simple::H2Connection", SourceInformation.getUnknownSourceInformation());
        String serverPrincipal = ((Root_meta_pure_alloy_connections_alloy_authentication_DelegatedKerberosAuthenticationStrategy_Impl) connection._authenticationStrategy())._serverPrincipal();
        Boolean quoteIdentifiers = connection._quoteIdentifiers();

        Assert.assertTrue(quoteIdentifiers);
        Assert.assertEquals("dummyPrincipal", serverPrincipal);

        //added new
        Pair<PureModelContextData, PureModel> compiledGraph2 = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: SqlServer;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: AwsOAuth\n" +
                "  {\n" +
                "    secretArn: 'name';\n" +
                "    discoveryUrl: 'name';\n" +
                "  };\n" +
                "}\n");
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection connection2 = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) compiledGraph2.getTwo().getConnection("simple::Connection", SourceInformation.getUnknownSourceInformation());
        String secretArn = ((Root_meta_pure_alloy_connections_alloy_authentication_AwsOAuthAuthenticationStrategy_Impl) connection2._authenticationStrategy())._secretArn();

        Assert.assertEquals("name", secretArn);

        //added new
        Pair<PureModelContextData, PureModel> compiledGraph3 = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: SqlServer;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: AwsPK\n" +
                "  {\n" +
                "    secretArn: 'name';\n" +
                "    user: 'name';\n" +
                "  };\n" +
                "}\n");
        Root_meta_pure_alloy_connections_RelationalDatabaseConnection connection3 = (Root_meta_pure_alloy_connections_RelationalDatabaseConnection) compiledGraph3.getTwo().getConnection("simple::Connection", SourceInformation.getUnknownSourceInformation());
        String user = ((Root_meta_pure_alloy_connections_alloy_authentication_AwsPKAuthenticationStrategy_Impl) connection3._authenticationStrategy())._user();

        Assert.assertEquals("name", user);

        test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::StaticConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: SqlServer;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: DelegatedKerberos\n" +
                "  {\n" +
                "    serverPrincipal: 'dummyPrincipal';\n" +
                "  };\n" +
                "}\n");

        test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::StaticConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: SqlServer;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    baseVaultReference: 'value';\n" +
                "    userNameVaultReference: 'value';\n" +
                "    passwordVaultReference: 'value';\n" +
                "  };\n" +
                "}\n");
    }


}
