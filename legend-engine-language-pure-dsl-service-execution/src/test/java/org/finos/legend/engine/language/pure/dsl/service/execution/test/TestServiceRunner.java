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

package org.finos.legend.engine.language.pure.dsl.service.execution.test;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.service.execution.AbstractServicePlanExecutor;
import org.finos.legend.engine.language.pure.dsl.service.execution.ServiceRunner;
import org.finos.legend.engine.language.pure.dsl.service.execution.ServiceRunnerInput;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestServiceRunner
{
    @Test
    public void testSimpleM2MServiceExecution()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testSimpleM2MServiceExecutionWithOutputStream() throws IOException
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("{\"fullName\": \"Peter Smith\"}"));

        PipedOutputStream outputStream = new PipedOutputStream();
        simpleM2MServiceRunner.run(serviceRunnerInput, outputStream);
        PipedInputStream pipedInputStream = new PipedInputStream(outputStream);
        String result = IOUtils.toString(pipedInputStream, Charset.defaultCharset());
        Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\"}}", result);
    }

    @Test
    public void testSimpleM2MServiceExecutionWithSerializationFormat()
    {
        SimpleM2MServiceRunner simpleM2MServiceRunner = new SimpleM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Collections.singletonList("[{\"fullName\": \"Peter Smith\"},{\"fullName\": \"John Johnson\"}]"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = simpleM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }

    @Test
    public void testMultiParameterM2MServiceExecution()
    {
        MultiParameterM2MServiceRunner multiParameterM2MServiceRunner = new MultiParameterM2MServiceRunner();
        ServiceRunnerInput serviceRunnerInput = ServiceRunnerInput
                .newInstance()
                .withArgs(Arrays.asList("{\"fullName\": \"Peter Smith\"}", "{\"fullName\": \"John Johnson\"}"))
                .withSerializationFormat(SerializationFormat.PURE);

        String result = multiParameterM2MServiceRunner.run(serviceRunnerInput);
        Assert.assertEquals("[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"}]", result);
    }

    private static class SimpleM2MServiceRunner extends AbstractServicePlanExecutor implements ServiceRunner
    {
        private static final String modelCode = "Class test::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_Person\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::function(input: String[1]): String[1]\n" +
                "{\n" +
                "   test::Person.all()->graphFetch(#{test::Person{firstName,lastName}}#)->serialize(#{test::Person{firstName,lastName}}#)\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Map\n" +
                "(\n" +
                "  *test::Person: Pure\n" +
                "  {\n" +
                "    ~src test::S_Person\n" +
                "    firstName: $src.fullName->split(' ')->at(0),\n" +
                "    lastName: $src.fullName->split(' ')->at(1)\n" +
                "  }\n" +
                ")\n" +
                "\n" +
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
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::S_Person;\n" +
                "          url: 'data:application/json,${input}';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        SimpleM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction(modelCode), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newSingleParameterExecutionBuilder()
                    .withParameter("input", serviceRunnerInput.getArgs().get(0))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static class MultiParameterM2MServiceRunner extends AbstractServicePlanExecutor implements ServiceRunner
    {
        private static final String modelCode = "Class test::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_Person1\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::S_Person2\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "}\n" +
                "\n" +
                "function test::function(input1: String[1], input2: String[1]): String[1]\n" +
                "{\n" +
                "   test::Person.all()->graphFetch(#{test::Person{firstName,lastName}}#)->serialize(#{test::Person{firstName,lastName}}#)\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::Map\n" +
                "(\n" +
                "  test::Person[s1]: Pure\n" +
                "  {\n" +
                "    ~src test::S_Person1\n" +
                "    firstName: $src.fullName->split(' ')->at(0),\n" +
                "    lastName: $src.fullName->split(' ')->at(1)\n" +
                "  }\n" +
                "  test::Person[s2]: Pure\n" +
                "  {\n" +
                "    ~src test::S_Person2\n" +
                "    firstName: $src.fullName->split(' ')->at(0),\n" +
                "    lastName: $src.fullName->split(' ')->at(1)\n" +
                "  }\n" +
                "  *test::Person: Operation\n" +
                "  {\n" +
                "    meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_(s1,s2)\n" +
                "  }\n" +
                ")\n" +
                "\n" +
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
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::S_Person1;\n" +
                "          url: 'data:application/json,${input1}';\n" +
                "        }\n" +
                "      }#,\n" +
                "      connection_2:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::S_Person2;\n" +
                "          url: 'data:application/json,${input2}';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        MultiParameterM2MServiceRunner()
        {
            super("test::Service", buildPlanForFetchFunction(modelCode), true);
        }

        @Override
        public void run(ServiceRunnerInput serviceRunnerInput, OutputStream outputStream)
        {
            newExecutionBuilder()
                    .withParameter("input1", serviceRunnerInput.getArgs().get(0))
                    .withParameter("input2", serviceRunnerInput.getArgs().get(1))
                    .withServiceRunnerInput(serviceRunnerInput)
                    .executeToStream(outputStream);
        }
    }

    private static SingleExecutionPlan buildPlanForFetchFunction(String modelCode)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(modelCode);
        PureModel pureModel = Compiler.compile(contextData, null, null);

        List<ValueSpecification> fetchFunctionExpressions = contextData.getElementsOfType(Function.class).get(0).body;

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(fetchFunctionExpressions, Collections.emptyList(), new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                Lists.mutable.empty(),
                LegendPlanTransformers.transformers
        );
    }
}
