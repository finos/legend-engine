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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension;
import org.junit.Assert;
import org.junit.Test;

/**
 * Before the connection parser stripped the grammar's quotes from a zone id, they were kept in the protocol, so a
 * model serialized then names its connection's zone as 'US/Arizona', quotes and all. A plan generated from that model
 * must name the zone as the parser writes it now.
 */
public class TestPlanGenerationWithQuotedTimeZone
{
    private static final String MODEL = "###Pure\n" +
            "Class test::Trade\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "  time: DateTime[1];\n" +
            "}\n" +
            "\n" +
            "Class <<temporal.processingtemporal>> test::Position\n" +
            "{\n" +
            "  id: Integer[1];\n" +
            "}\n" +
            "\n" +
            "###Relational\n" +
            "Database test::DB\n" +
            "(\n" +
            "  Table tradeTable (\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    time TIMESTAMP\n" +
            "  )\n" +
            "  Table positionTable (\n" +
            "    milestoning(processing(PROCESSING_IN = in_z, PROCESSING_OUT = out_z))\n" +
            "    id INTEGER PRIMARY KEY,\n" +
            "    in_z TIMESTAMP,\n" +
            "    out_z TIMESTAMP\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Trade : Relational {\n" +
            "    id: [test::DB]tradeTable.id,\n" +
            "    time: [test::DB]tradeTable.time\n" +
            "  }\n" +
            "  test::Position : Relational {\n" +
            "    id: [test::DB]positionTable.id\n" +
            "  }\n" +
            ")\n" +
            "\n" +
            "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          timezone: 'US/Arizona';\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";

    /**
     * A date parameter is left for execution, so the zone is written into the SQL template for freemarker to apply.
     */
    @Test
    public void testPlanWithDateParameterFromModelWithQuotedTimeZone() throws Exception
    {
        SQLExecutionNode sqlNode = generateSQLNodeFromOldModel("###Pure\n" +
                "function test::fetch(dt: DateTime[1]): Any[*]\n" +
                "{\n" +
                "  test::Trade.all()->filter(t | $t.time == $dt)->project([t | $t.id], ['id'])\n" +
                "}\n");
        Assert.assertEquals("US/Arizona", sqlNode.connection.timeZone);
        Assert.assertTrue(sqlNode.sqlQuery, sqlNode.sqlQuery.contains("GMTtoTZ( \"[US/Arizona]\" dt)"));
    }

    /**
     * A date written into the query, as a milestoned class's all takes one, is rendered into the SQL during plan
     * generation, in the connection's zone.
     */
    @Test
    public void testPlanWithDateLiteralFromModelWithQuotedTimeZone() throws Exception
    {
        SQLExecutionNode sqlNode = generateSQLNodeFromOldModel("###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  test::Position.all(%9999-12-31)->project([p | $p.id], ['id'])\n" +
                "}\n");
        Assert.assertEquals("US/Arizona", sqlNode.connection.timeZone);
        Assert.assertTrue(sqlNode.sqlQuery, sqlNode.sqlQuery.contains("DATE'9999-12-31'"));
    }

    /**
     * A day renders the same in any zone, so only a date-time shows the zone was applied rather than lost to GMT.
     * Noon GMT is five in the morning in Arizona, which keeps standard time all year.
     */
    @Test
    public void testPlanWithDateTimeLiteralFromModelWithQuotedTimeZone() throws Exception
    {
        SQLExecutionNode sqlNode = generateSQLNodeFromOldModel("###Pure\n" +
                "function test::fetch(): Any[*]\n" +
                "{\n" +
                "  test::Trade.all()->filter(t | $t.time == %2020-06-01T12:00:00)->project([t | $t.id], ['id'])\n" +
                "}\n");
        Assert.assertEquals("US/Arizona", sqlNode.connection.timeZone);
        Assert.assertTrue(sqlNode.sqlQuery, sqlNode.sqlQuery.contains("TIMESTAMP'2020-06-01 05:00:00'"));
    }

    private static SQLExecutionNode generateSQLNodeFromOldModel(String fetchFunction) throws Exception
    {
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
        String json = objectMapper.writeValueAsString(PureGrammarParser.newInstance().parseModel(MODEL + fetchFunction));
        String oldJson = json.replace("\"timeZone\":\"US/Arizona\"", "\"timeZone\":\"'US/Arizona'\"");
        Assert.assertNotEquals(json, oldJson);

        SingleExecutionPlan plan = generatePlan(objectMapper.readValue(oldJson, PureModelContextData.class));

        MutableList<SQLExecutionNode> sqlNodes = collectSQLNodes(plan.rootExecutionNode, Lists.mutable.empty());
        Assert.assertEquals(1, sqlNodes.size());
        return sqlNodes.get(0);
    }

    private static SingleExecutionPlan generatePlan(PureModelContextData contextData)
    {
        PureModel pureModel = Compiler.compile(contextData, null, Identity.getAnonymousIdentity().getName());
        Function fetch = contextData.getElementsOfType(Function.class).get(0);
        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(fetch.body, fetch.parameters, new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding__Extension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers
        );
    }

    private static MutableList<SQLExecutionNode> collectSQLNodes(ExecutionNode node, MutableList<SQLExecutionNode> sqlNodes)
    {
        if (node instanceof SQLExecutionNode)
        {
            sqlNodes.add((SQLExecutionNode) node);
        }
        node.executionNodes.forEach(child -> collectSQLNodes(child, sqlNodes));
        return sqlNodes;
    }
}
