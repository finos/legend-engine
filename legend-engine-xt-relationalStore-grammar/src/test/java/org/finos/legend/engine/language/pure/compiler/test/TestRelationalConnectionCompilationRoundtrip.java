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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_RelationalDatabaseConnection;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_DelegatedKerberosAuthenticationStrategy_Impl;
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
