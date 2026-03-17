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

package org.finos.legend.engine.language.deephaven.generation;

import net.javacrumbs.jsonunit.JsonAssert;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_deephavenApp_DeephavenApp;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TestDeephavenAppArtifactGeneration
{
    private static final String GRAMMAR =
            "###Deephaven\n" +
            "Deephaven test::DeephavenStore\n" +
            "(\n" +
            "    Table stockTrades\n" +
            "    (\n" +
            "        TradeID: INT,\n" +
            "        StockSymbol: STRING,\n" +
            "        Price: FLOAT,\n" +
            "        Quantity: INT,\n" +
            "        City: STRING,\n" +
            "        IsBuy: BOOLEAN\n" +
            "    )\n" +
            ")\n" +
            "\n" +
            "DeephavenApp test::MyDeephavenApp\n" +
            "{\n" +
            "    applicationName: 'StockTradesApp';\n" +
            "    function: test::filterStockTrades():Any[*];\n" +
            "    description: 'A sample Deephaven app that filters stock trades';\n" +
            "    ownership: Deployment { identifier: '12345' };\n" +
            "}\n" +
            "\n" +
            "###Pure\n" +
            "function test::filterStockTrades(): Any[*]\n" +
            "{\n" +
            "    #>{test::DeephavenStore.stockTrades}#->filter(c | $c.City == 'New York')->filter(c | $c.IsBuy == true)->select(~[StockSymbol])->from(test::DeephavenRuntime)\n" +
            "}\n" +
            "\n" +
            "###Connection\n" +
            "DeephavenConnection test::DeephavenConnection\n" +
            "{\n" +
            "    store: test::DeephavenStore;\n" +
            "    serverUrl: 'http://localhost:10000'\n" +
            "    authentication: # PSK {\n" +
            "        psk: 'myStaticPSK';\n" +
            "    }#;\n" +
            "}\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime test::DeephavenRuntime\n" +
            "{\n" +
            "    mappings:\n" +
            "    [\n" +
            "    ];\n" +
            "    connections:\n" +
            "    [\n" +
            "        test::DeephavenStore:\n" +
            "        [\n" +
            "            connection: test::DeephavenConnection\n" +
            "        ]\n" +
            "    ];\n" +
            "}\n";

    @Test
    public void testDeephavenAppArtifactGeneration() throws Exception
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(GRAMMAR);
        PureModel model = Compiler.compile(pmcd, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());

        PackageableElement deephavenApp = model.getPackageableElement("test::MyDeephavenApp");
        Assert.assertTrue("Should be a DeephavenApp instance",
                deephavenApp instanceof Root_meta_external_function_activator_deephavenApp_DeephavenApp);

        DeephavenAppArtifactGenerationExtension extension = new DeephavenAppArtifactGenerationExtension();
        Assert.assertTrue("canGenerate should return true for DeephavenApp", extension.canGenerate(deephavenApp));

        List<Artifact> artifacts = extension.generate(deephavenApp, model, pmcd, null);

        Assert.assertEquals("Should generate exactly 1 artifact", 1, artifacts.size());

        Artifact artifact = artifacts.get(0);
        Assert.assertEquals("deephavenAppArtifact.json", artifact.path);
        Assert.assertEquals("json", artifact.format);

        JsonAssert.assertJsonEquals(
                readModelContentFromResource("/expectedDeephavenAppArtifact.json"),
                artifact.content
        );
    }

    private String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(TestDeephavenAppArtifactGeneration.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

