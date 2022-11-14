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
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.core_pure_extensions_functions;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_Mapping_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_Runtime_Impl;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_generation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

public class TestExecuteFunctionExecutionPlan
{
    private Result getResultFromFunctionGrammar(String functionGrammar, Map<String, ?> params, String funcName)
    {
        PureModelContextData pmcd = PureGrammarParser.newInstance().parseModel(functionGrammar);
        PureModel pureModelForFunction = Compiler.compile(pmcd, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, null);
        ConcreteFunctionDefinition<?> concreteFxn = pureModelForFunction.getConcreteFunctionDefinition_safe(funcName);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = core_pure_extensions_functions.Root_meta_pure_extension_defaultExtensions__Extension_MANY_(pureModelForFunction.getExecutionSupport());
        Root_meta_pure_executionPlan_ExecutionPlan executionPlanInPure = core_pure_executionPlan_executionPlan_generation.Root_meta_pure_executionPlan_executionPlan_FunctionDefinition_1__Mapping_1__Runtime_1__Extension_MANY__ExecutionPlan_1_(concreteFxn, new Root_meta_pure_mapping_Mapping_Impl(""), new Root_meta_pure_runtime_Runtime_Impl(""), extensions, pureModelForFunction.getExecutionSupport());
        PlanPlatform platform = PlanPlatform.JAVA;
        executionPlanInPure = platform.bindPlan(executionPlanInPure, null, pureModelForFunction, extensions);
        SingleExecutionPlan singleExecPlan = PlanGenerator.transformExecutionPlan(executionPlanInPure, pureModelForFunction, "vX_X_X", null, extensions, LegendPlanTransformers.transformers);
        Result result = PlanExecutor.newPlanExecutor().execute(singleExecPlan, params);
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
}
