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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.qst.table.TicketTable;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.stores.deephaven.connection.DeephavenSession;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.DeephavenTestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_deephavenApp_DeephavenApp;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class TestDeephavenAppDockerIntegration
{
    private static final ObjectMapper mapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private static final String DEEPHAVEN_VERSION = "0.40.7";

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
            "    description: 'Filters stock trades by city and buy flag';\n" +
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

    @BeforeClass
    public static void generateArtifactsAndStartContainer() throws Exception
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(GRAMMAR);
        PureModel model = Compiler.compile(pmcd, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());

        PackageableElement deephavenApp = model.getPackageableElement("test::MyDeephavenApp");
        Assert.assertTrue("Should compile to a DeephavenApp",
                deephavenApp instanceof Root_meta_external_function_activator_deephavenApp_DeephavenApp);

        DeephavenAppArtifactGenerationExtension extension = new DeephavenAppArtifactGenerationExtension();
        List<Artifact> artifacts = extension.generate(deephavenApp, model, pmcd, null);
        Assert.assertEquals("Should generate exactly 1 artifact", 1, artifacts.size());

        DeephavenAppArtifactContent content = mapper.readValue(artifacts.get(0).content, DeephavenAppArtifactContent.class);
        Assert.assertNotNull("appConfigContent should not be null", content.appConfigContent);
        Assert.assertNotNull("javaSourceContent should not be null", content.javaSourceContent);

        String appFileName = "stocktradesapp";
        boolean started = DeephavenTestContainer.startDeephavenWithGeneratedApp(
                DEEPHAVEN_VERSION, content.appConfigContent, content.javaSourceContent, appFileName);
        Assert.assertTrue(
                "Deephaven Docker container failed to start. Check Docker availability and container logs.",
                started);
    }

    @AfterClass
    public static void stopContainer()
    {
        DeephavenTestContainer.stopDeephaven();
    }

    @Test
    public void testGeneratedAppIsQueryable() throws Exception
    {
        try (BufferAllocator allocator = new RootAllocator();
             DeephavenSession session = DeephavenTestContainer.buildSession(DeephavenTestContainer.deephavenContainer, allocator))
        {
            TableHandle resultHandle = session.getClientSession().execute(TicketTable.fromApplicationField("stocktradesapp", "result"));
            Assert.assertNotNull("The 'result' table should be queryable via application field", resultHandle);
            resultHandle.close();

            TableHandle sourceHandle = session.getClientSession().execute(TicketTable.fromApplicationField("stocktradesapp", "stockTrades"));
            Assert.assertNotNull("The 'stockTrades' source table should be queryable via application field", sourceHandle);
            sourceHandle.close();
        }
    }
}

