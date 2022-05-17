package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_DelegatedKerberosAuthenticationStrategy_Impl;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestS3ConnectionCompilationRoundtrip {

    @Test
    public void testConnectionPropertiesPropagatedToCompiledGraph()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Connection\n" +
                        "AwsS3Connection meta::mySimpleConnection\n" +
                        "{\n" +
                        "  store: store::Store;\n" +
                        "  partition: AWS;\n" +
                        "  region: 'US';\n" +
                        "  bucket: 'abc';\n" +
                        "  key: 'xyz';\n" +
                        "}\n");
        Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection connection = (Root_meta_external_persistence_aws_metamodel_connection_AwsS3Connection) compiledGraph.getTwo().getConnection("meta::mySimpleConnection", SourceInformation.getUnknownSourceInformation());

        String bucket = connection._bucket();

        Assert.assertEquals(bucket, "abc");

        String key = connection._key();

        Assert.assertEquals(key, "xyz");
    }
}
