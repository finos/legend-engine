// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.query.pure.api.test.inMemory;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Mapping_Impl;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime_Impl;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_generation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;

public class TestExecuteFunctionExecutionPlan
{
    private Result getResultFromFunctionGrammar(String functionGrammar, Map<String, ?> params, String funcName)
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(functionGrammar);
        PureModel pureModelForFunction = Compiler.compile(pmcd, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        ConcreteFunctionDefinition<?> concreteFxn = pureModelForFunction.getConcreteFunctionDefinition_safe(funcName);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModelForFunction.getExecutionSupport()));
        Root_meta_pure_executionPlan_ExecutionPlan executionPlanInPure = core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__Extension_MANY__ExecutionPlan_1_(concreteFxn, new Root_meta_pure_mapping_Mapping_Impl(""), new Root_meta_core_runtime_Runtime_Impl(""), extensions, pureModelForFunction.getExecutionSupport());
        PlanPlatform platform = PlanPlatform.JAVA;
        executionPlanInPure = platform.bindPlan(executionPlanInPure, null, pureModelForFunction, extensions);
        SingleExecutionPlan singleExecPlan = PlanGenerator.transformExecutionPlan(executionPlanInPure, pureModelForFunction, "vX_X_X", Identity.getAnonymousIdentity(), extensions, LegendPlanTransformers.transformers);
        Result result = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build().execute(singleExecPlan, params);
        return result;
    }

    @Test
    public void testExecutingFunctionWithMutlipleVariablesExecutionPlan()
    {
        String functionGrammar = "###Pure\n" +
                                "function meta::pure::executionPlan::tests::function::welcomeFunction(msg: String[1]):String[1]\n" +
                                "{\n" +
                                    "let x = 'Hello, ' + $msg;\n" +
                                    "$x->replace(',', '!');\n" +
                                "}\n";
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("msg", "Hakuna Matata");
        Result result = getResultFromFunctionGrammar(functionGrammar, params,"meta::pure::executionPlan::tests::function::welcomeFunction_String_1__String_1_");
        Assert.assertEquals("Hello! Hakuna Matata", ((ConstantResult) result).getValue());
    }

    @Test
    public void testExecutingFunctionWithWrapperPrimitives()
    {
        String functionGrammar = "###Pure\n" +
                "function meta::pure::executionPlan::tests::function::integerFunction(var1: String[1], var2: String[1]):Integer[1]\n" +
                "{\n" +
                "$var1->parseInteger() + $var2->parseInteger();\n" +
                "}\n";
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("var1", "1", "var2", "2");
        Result result = getResultFromFunctionGrammar(functionGrammar, params,"meta::pure::executionPlan::tests::function::integerFunction_String_1__String_1__Integer_1_");
        Assert.assertEquals(3L, ((ConstantResult) result).getValue());
    }

    @Test
    public void testExecutingFunctionWithListParameter()
    {
        String functionGrammar = "###Pure\n" +
                "function meta::pure::executionPlan::tests::function::listTransformation(var1: String[*]):String[*]\n" +
                "{\n" +
                "$var1->map(x|$x+'_');\n" +
                "}\n";
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("var1", Lists.mutable.of("hakuna","matata"));
        Result result = getResultFromFunctionGrammar(functionGrammar, params,"meta::pure::executionPlan::tests::function::listTransformation_String_MANY__String_MANY_");
        Assert.assertEquals(2, ((ArrayList)((ConstantResult) result).getValue()).size());
        Assert.assertEquals("hakuna_", ((ArrayList)((ConstantResult) result).getValue()).get(0));
        Assert.assertEquals("matata_", ((ArrayList)((ConstantResult) result).getValue()).get(1));
    }

    @Test
    public void testExecutingFunctionWithListParameterAndMultipleVariableExpressions()
    {
        String functionGrammar = "###Pure\n" +
                "function meta::pure::executionPlan::tests::function::listTransformation(var1: String[*]):Integer[*]\n" +
                "{\n" +
                "let y = $var1->map(x|$x+'2');\n" +
                "$y->map(x|$x->parseInteger());\n" +
                "}\n";
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("var1", Lists.mutable.of("1","2"));
        Result result = getResultFromFunctionGrammar(functionGrammar, params,"meta::pure::executionPlan::tests::function::listTransformation_String_MANY__Integer_MANY_");
        Assert.assertEquals(2, ((ArrayList)((ConstantResult) result).getValue()).size());
        Assert.assertEquals(12L, ((ArrayList)((ConstantResult) result).getValue()).get(0));
        Assert.assertEquals(22L, ((ArrayList)((ConstantResult) result).getValue()).get(1));
    }

    @Test
    public void testExecutingFunctionWithAdvancedMultiLevelOperations()
    {
        String grammar = "###Pure\n" +
                "Class test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::testAdvancedMultiLevelOperations(p1: String[1], p2: String[0..1], p3: String[*]): String[1]\n" +
                "{\n" +
                "   let p1Upper  = $p1->toUpper();\n" +
                "   let p2Lower  = if($p2->isEmpty(), | '', | $p2->toOne()->toLower());\n" +
                "   let p3Joined = $p3->joinStrings('|');\n" +
                "   let p3Parsed = $p3->map(x | $x->parseInteger());\n" +
                "   let p3Sum    = $p3Parsed->sum();\n" +
                "   let queryRes = test::Person.all()->filter(x | $p1 != '').name->count()->from(test::Map, test::Runtime);\n" +
                "   let ifClause = if($p3Sum > 10, | $p1Upper + '~' + $queryRes->toString(), | $p2Lower + '~' + $queryRes->toString());\n" +
                "\n" +
                "   let res = [\n" +
                "      ('P1Upper:' + $p1Upper),\n" +
                "      ('P2Lower:' + $p2Lower),\n" +
                "      ('P3Joined:' + $p3Joined),\n" +
                "      ('P3Sum:' + $p3Sum->toString()),\n" +
                "      ('QueryRes:' + $queryRes->toString()),\n" +
                "      ('IfClause:' + $ifClause)\n" +
                "   ]->joinStrings('[', ', ', ']');\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Map\n" +
                "(\n" +
                "  test::Person: Relational {\n" +
                "    name: [test::DB]person.NAME\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Relational\n" +
                "Database test::DB\n" +
                "(\n" +
                "  Table person(NAME VARCHAR(100) PRIMARY KEY)\n" +
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
                "          specification: LocalH2 {testDataSetupCSV: 'default\\nperson\\nNAME\\nPeter\\n----'; };\n" +
                "          auth: DefaultH2;\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        BiConsumer<Map<String, Object>, String> assertionFunc = (params, expected) ->
        {
            try (Result result = getResultFromFunctionGrammar(grammar, params, "test::testAdvancedMultiLevelOperations_String_1__String_$0_1$__String_MANY__String_1_"))
            {
                Assert.assertTrue(((ConstantResult) result).getValue() instanceof String);
                Assert.assertEquals(expected, ((ConstantResult) result).getValue());
            }
        };

        assertionFunc.accept(
                Maps.mutable.of("p1", "Hello", "p2", "World", "p3", Lists.mutable.of("1", "4")),
                "[P1Upper:HELLO, P2Lower:world, P3Joined:1|4, P3Sum:5, QueryRes:1, IfClause:world~1]"
        );
        assertionFunc.accept(
                Maps.mutable.of("p1", "Hello", "p2", "World", "p3", Lists.mutable.of("6", "7")),
                "[P1Upper:HELLO, P2Lower:world, P3Joined:6|7, P3Sum:13, QueryRes:1, IfClause:HELLO~1]"
        );
        assertionFunc.accept(
                Maps.mutable.of("p1", "", "p3", Lists.mutable.empty()),
                "[P1Upper:, P2Lower:, P3Joined:, P3Sum:0, QueryRes:0, IfClause:~0]"
        );
    }
}
