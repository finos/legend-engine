package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.*;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestFinCloudConnectionRoundtrip {

    @Test
    public void testConnectionPropertiesPropagatedToCompiledGraph()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Connection\n" +
                        "AwsFinCloudConnection meta::mySimpleConnection\n" +
                        "{\n" +
                        "  store: store::Store;\n" +
                        "  datasetId: 'AWS';\n" +
                        "  authenticationStrategy: awsOAuth\n" +
                        "  {\n" +
                        "    secretArn: 'name';\n" +
                        "    discoveryUrl: 'name';\n" +
                        "  };\n" +
                        "  apiUrl: 'test';\n" +
                "}\n");
        Root_meta_external_persistence_aws_metamodel_connection_AwsFinCloudConnection connection = (Root_meta_external_persistence_aws_metamodel_connection_AwsFinCloudConnection) compiledGraph.getTwo().getConnection("meta::mySimpleConnection", SourceInformation.getUnknownSourceInformation());
        String secretArn = ((Root_meta_pure_alloy_connections_alloy_authentication_AwsOAuthAuthenticationStrategy_Impl) connection._authenticationStrategy())._secretArn();

        Assert.assertEquals("name", secretArn);

    }


}
