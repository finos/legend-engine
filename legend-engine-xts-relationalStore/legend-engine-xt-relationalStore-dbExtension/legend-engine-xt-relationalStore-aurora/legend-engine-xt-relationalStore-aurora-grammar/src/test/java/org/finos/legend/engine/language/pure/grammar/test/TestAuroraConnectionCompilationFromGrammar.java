// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test;

import net.javacrumbs.jsonunit.JsonAssert;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_runtime_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_store;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.junit.Ignore;
import org.junit.Test;

public class TestAuroraConnectionCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Test
    public void compileSimpleAuroraConnection()
    {
        String grammar = "###Connection\n" +
                "RelationalDatabaseConnection simple::AuroraConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Aurora;\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "  specification: Aurora\n" +
                "  {\n" +
                "    host: 'mydb.cluster-xyz.us-east-1.rds.amazonaws.com';\n" +
                "    port: 5432;\n" +
                "    name: 'mydb';\n" +
                "  };\n" +
                "}\n";

        testCompilationAndProtocolExtension(grammar, "simple::AuroraConnection",
                "{\"authenticationStrategy\":{\"_type\":\"userNamePassword\",\"passwordVaultReference\":\"pwd\",\"userNameVaultReference\":\"user\"},\"_type\":\"RelationalDatabaseConnection\",\"type\":\"Aurora\",\"datasourceSpecification\":{\"host\":\"mydb.cluster-xyz.us-east-1.rds.amazonaws.com\",\"name\":\"mydb\",\"port\":5432,\"_type\":\"aurora\"},\"element\":\"\"}");
    }

    @Test
    public void compileGlobalAuroraConnection()
    {
        String grammar = "###Connection\n" +
                "RelationalDatabaseConnection simple::GlobalAuroraConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Aurora;\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "  specification: GlobalAurora\n" +
                "  {\n" +
                "    host: 'mydb.global-xyz.rds.amazonaws.com';\n" +
                "    port: 5432;\n" +
                "    name: 'mydb';\n" +
                "    region: 'us-east-1';\n" +
                "    globalClusterInstanceHostPatterns: ['?.XYZ1.us-east-1.rds.amazonaws.com', '?.XYZ2.us-west-2.rds.amazonaws.com'];\n" +
                "  };\n" +
                "}\n";

        testCompilationAndProtocolExtension(grammar, "simple::GlobalAuroraConnection",
                "{\"authenticationStrategy\":{\"_type\":\"userNamePassword\",\"passwordVaultReference\":\"pwd\",\"userNameVaultReference\":\"user\"},\"_type\":\"RelationalDatabaseConnection\",\"type\":\"Aurora\",\"datasourceSpecification\":{\"globalClusterInstanceHostPatterns\":[\"?.XYZ1.us-east-1.rds.amazonaws.com\",\"?.XYZ2.us-west-2.rds.amazonaws.com\"],\"host\":\"mydb.global-xyz.rds.amazonaws.com\",\"name\":\"mydb\",\"port\":5432,\"region\":\"us-east-1\",\"_type\":\"globalAurora\"},\"element\":\"\"}");
    }

    private static void testCompilationAndProtocolExtension(String grammar, String connectionPath, String expectedProtocol)
    {
        Pair<PureModelContextData, PureModel> compilationResult = test(grammar);

        PureModel pureModel = compilationResult.getTwo();

        CompiledExecutionSupport executionSupport = pureModel.getExecutionSupport();
        RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(executionSupport));

        Root_meta_core_runtime_Connection connection = pureModel.getConnection(connectionPath, null);
        Root_meta_protocols_pure_vX_X_X_metamodel_runtime_Connection protocolConnection = core_pure_protocol_vX_X_X_transfers_store.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_runtime_transformConnection_Connection_1__Extension_MANY__Connection_1_(connection, routerExtensions, executionSupport);
        String json = org.finos.legend.pure.generated.core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(protocolConnection, executionSupport);
        JsonAssert.assertJsonEquals(expectedProtocol, json);
    }

    @Override
    protected String getDuplicatedElementTestCode()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        throw new UnsupportedOperationException();
    }

    @Test
    @Ignore("N/A - no new elements on extension")
    public void testDuplicatedElement()
    {
        throw new UnsupportedOperationException();
    }
}
