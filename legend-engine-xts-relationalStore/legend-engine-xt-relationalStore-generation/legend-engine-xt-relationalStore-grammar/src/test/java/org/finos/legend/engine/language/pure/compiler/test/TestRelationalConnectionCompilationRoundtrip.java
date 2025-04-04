//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_UserNamePasswordAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_external_store_relational_runtime_GenerationFeaturesConfig;
import org.junit.Assert;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;
import static org.junit.Assert.assertEquals;

public class TestRelationalConnectionCompilationRoundtrip
{
    @Test
    public void testMemSqlConnectionPropertiesPropagatedToCompiledGraph()
    {
        Pair<PureModelContextData, PureModel> result = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::StaticConnection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
                "  type: MemSQL;\n" +
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

        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection connection = (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) result.getTwo().getConnection("simple::StaticConnection", SourceInformation.getUnknownSourceInformation());
        String baseVaultReference = ((Root_meta_pure_alloy_connections_alloy_authentication_UserNamePasswordAuthenticationStrategy_Impl) connection._authenticationStrategy())._baseVaultReference();
        String userNameVaultReference = ((Root_meta_pure_alloy_connections_alloy_authentication_UserNamePasswordAuthenticationStrategy_Impl) connection._authenticationStrategy())._userNameVaultReference();
        String passwordVaultReference = ((Root_meta_pure_alloy_connections_alloy_authentication_UserNamePasswordAuthenticationStrategy_Impl) connection._authenticationStrategy())._passwordVaultReference();
        assertEquals("value", baseVaultReference);
        assertEquals("value", userNameVaultReference);
        assertEquals("value", passwordVaultReference);
    }

    @Test
    public void testSqlServerConnectionPropertiesPropagatedToCompiledGraph()
    {
        test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::StaticConnection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
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
                "  queryTimeOutInSeconds: 7777;\n" +
                "}\n");
    }

    @Test
    public void testConnectionWithNoStore()
    {
        Pair<PureModelContextData, PureModel> test = test("");
        PureModelContextData result = PureGrammarParser.newInstance().parseModel(
                "###Connection\n" +
                        "RelationalDatabaseConnection simple::StaticConnection\n" +
                        "{\n" +
                        "  store: asasmodel::relational::tests::dbInc;\n" +
                        "  type: MemSQL;\n" +
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

        LegacyRuntime runtime = new LegacyRuntime();
        runtime.connections = result.getElementsOfType(PackageableConnection.class).stream().map(x -> x.connectionValue).collect(Collectors.toList());
        HelperRuntimeBuilder.buildPureRuntime(runtime, test.getTwo().getContext());
    }

    @Test
    public void testH2ConnectionPropertiesPropagatedToCompiledGraph()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: true;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "  queryTimeOutInSeconds: 7777;\n" +
                "}\n");
        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection connection = (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) compiledGraph.getTwo().getConnection("simple::H2Connection", SourceInformation.getUnknownSourceInformation());
        Boolean quoteIdentifiers = connection._quoteIdentifiers();
        Long expectedQueryTimeOutInSeconds = 7777L;

        Assert.assertTrue(quoteIdentifiers);
        Assert.assertEquals(expectedQueryTimeOutInSeconds, connection._queryTimeOutInSeconds());
    }

    @Test
    public void testConnectionWithQueryGenerationConfigs()
    {
        test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: true;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "  queryGenerationConfigs: [\n" +
                "    GenerationFeaturesConfig\n" +
                "    {\n" +
                "      enabled: ['FEAT_1'];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [80:5-83:5]: Unknown relational generation feature: FEAT_1. Known features are: [REMOVE_UNION_OR_JOINS]");

        Pair<PureModelContextData, PureModel> compiledGraph = test(TestRelationalCompilationFromGrammar.DB_INC +
                "###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: model::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: true;\n" +
                "  specification: Static\n" +
                "  {\n" +
                "    name: 'name';\n" +
                "    host: 'host';\n" +
                "    port: 1234;\n" +
                "  };\n" +
                "  auth: Test\n" +
                "  {\n" +
                "  };\n" +
                "  queryGenerationConfigs: [\n" +
                "    GenerationFeaturesConfig\n" +
                "    {\n" +
                "      disabled: ['REMOVE_UNION_OR_JOINS'];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
        Root_meta_external_store_relational_runtime_RelationalDatabaseConnection connection = (Root_meta_external_store_relational_runtime_RelationalDatabaseConnection) compiledGraph.getTwo().getConnection("simple::H2Connection", SourceInformation.getUnknownSourceInformation());
        Assert.assertEquals(1, connection._queryGenerationConfigs().size());
        Assert.assertTrue(connection._queryGenerationConfigs().toList().get(0) instanceof Root_meta_external_store_relational_runtime_GenerationFeaturesConfig);
        Assert.assertTrue(((Root_meta_external_store_relational_runtime_GenerationFeaturesConfig) connection._queryGenerationConfigs().toList().get(0))._enabled().isEmpty());
        Assert.assertEquals(1, ((Root_meta_external_store_relational_runtime_GenerationFeaturesConfig) connection._queryGenerationConfigs().toList().get(0))._disabled().size());
        Assert.assertEquals("REMOVE_UNION_OR_JOINS", ((Root_meta_external_store_relational_runtime_GenerationFeaturesConfig) connection._queryGenerationConfigs().toList().get(0))._disabled().toList().get(0));
    }
}
