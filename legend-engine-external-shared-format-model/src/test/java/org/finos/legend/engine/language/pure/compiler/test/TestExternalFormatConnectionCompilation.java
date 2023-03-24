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

package org.finos.legend.engine.language.pure.compiler.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_ExternalFormatConnection;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestExternalFormatConnectionCompilation
{
    @Test
    public void testPackageableExternalFormatConnection()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Pure\n" +
                        "Class meta::firm::Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###ExternalFormat\n" +
                        "SchemaSet meta::firm::SchemaSet\n" +
                        "{\n" +
                        "  format: Example;\n" +
                        "  schemas: [ { content: 'example'; } ];\n" +
                        "}\n" +
                        "\n" +
                        "Binding meta::firm::Binding\n" +
                        "{\n" +
                        "  schemaSet: meta::firm::SchemaSet;\n" +
                        "  contentType: 'text/example';\n" +
                        "  modelIncludes: [\n" +
                        "    meta::firm::Person\n" +
                        "  ];\n" +
                        "}\n" +
                        "\n" +
                        "###Connection\n" +
                        "ExternalFormatConnection simple::Connection\n" +
                        "{\n" +
                        "  store: meta::firm::Binding;\n" +
                        "  source: UrlStream\n" +
                        "  {\n" +
                        "    url: 'executor:default';\n" +
                        "  };\n" +
                        "}\n");
        Root_meta_external_shared_format_executionPlan_ExternalFormatConnection connection = (Root_meta_external_shared_format_executionPlan_ExternalFormatConnection) compiledGraph.getTwo().getConnection("simple::Connection", SourceInformation.getUnknownSourceInformation());
        Assert.assertTrue(connection._externalSource() instanceof Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource);
        Assert.assertEquals("executor:default", ((Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource) connection._externalSource())._url());
    }

    @Test
    public void testExternalFormatConnectionInPackageableRuntime()
    {
        Pair<PureModelContextData, PureModel> compiledGraph = test(
                "###Pure\n" +
                        "Class meta::firm::Person\n" +
                        "{\n" +
                        "  fullName: String[1];\n" +
                        "}\n" +
                        "\n" +
                        "###ExternalFormat\n" +
                        "SchemaSet meta::firm::SchemaSet\n" +
                        "{\n" +
                        "  format: Example;\n" +
                        "  schemas: [ { content: 'example'; } ];\n" +
                        "}\n" +
                        "\n" +
                        "Binding meta::firm::Binding\n" +
                        "{\n" +
                        "  schemaSet: meta::firm::SchemaSet;\n" +
                        "  contentType: 'text/example';\n" +
                        "  modelIncludes: [\n" +
                        "    meta::firm::Person\n" +
                        "  ];\n" +
                        "}\n" +
                        "\n" +
                        "###Mapping\n" +
                        "Mapping test::Mapping\n" +
                        "(\n" +
                        "  meta::firm::Person: Pure\n" +
                        "  {\n" +
                        "    ~src meta::firm::Person\n" +
                        "    fullName: $src.fullName\n" +
                        "  }\n" +
                        ")\n" +
                        "###Runtime\n" +
                        "Runtime test::runtime\n" +
                        "{\n" +
                        "  mappings:\n" +
                        "  [\n" +
                        "    test::Mapping\n" +
                        "  ];\n" +
                        "  connections:\n" +
                        "  [\n" +
                        "    meta::firm::Binding:\n" +
                        "    [\n" +
                        "      c1:\n" +
                        "      #{\n" +
                        "        ExternalFormatConnection\n" +
                        "        {\n" +
                        "          source: UrlStream\n" +
                        "          {\n" +
                        "            url: 'executor:default';\n" +
                        "          };\n" +
                        "        }\n" +
                        "      }#\n" +
                        "    ]\n" +
                        "  ];\n" +
                        "}\n");
        Root_meta_pure_runtime_Runtime runtime = compiledGraph.getTwo().getRuntime("test::runtime");
        Root_meta_external_shared_format_executionPlan_ExternalFormatConnection connection = (Root_meta_external_shared_format_executionPlan_ExternalFormatConnection) runtime._connections().toList().get(0);
        Assert.assertTrue(connection._externalSource() instanceof Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource);
        Assert.assertEquals("executor:default", ((Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource) connection._externalSource())._url());
    }
}
