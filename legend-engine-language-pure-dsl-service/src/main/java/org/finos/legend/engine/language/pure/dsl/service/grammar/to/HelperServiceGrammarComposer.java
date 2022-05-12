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

package org.finos.legend.engine.language.pure.dsl.service.grammar.to;

import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.*;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.test.assertion.HelperTestAssertionGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.*;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperServiceGrammarComposer
{
    public static String renderServiceExecution(Execution execution, PureGrammarComposerContext context)
    {
        int baseIndentation = 1;
        if (execution instanceof PureSingleExecution)
        {
            PureSingleExecution pureSingleExecution = (PureSingleExecution) execution;
            return "Single\n" +
                    getTabString(baseIndentation) + "{\n" +
                    getTabString(baseIndentation + 1) + "query: " + pureSingleExecution.func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) + ";\n" +
                    getTabString(baseIndentation + 1) + "mapping: " + pureSingleExecution.mapping + ";\n" +
                    renderServiceExecutionRuntime(pureSingleExecution.runtime, baseIndentation + 1, context) + "\n" +
                    getTabString(baseIndentation) + "}\n";
        }
        else if (execution instanceof PureMultiExecution)
        {
            PureMultiExecution pureMultiExecution = (PureMultiExecution) execution;
            StringBuilder builder = new StringBuilder().append("Multi\n");
            appendTabString(builder, baseIndentation).append("{\n");
            appendTabString(builder, baseIndentation + 1).append("query: ").append(pureMultiExecution.func.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build())).append(";\n");
            appendTabString(builder, baseIndentation + 1).append("key: ").append(convertString(pureMultiExecution.executionKey, true)).append(";\n");
            builder.append(LazyIterate.collect(pureMultiExecution.executionParameters, executionParameter -> renderKeyedExecutionParameter(executionParameter, context)).makeString("\n"));
            return builder.append("\n").append(getTabString(baseIndentation)).append("}\n").toString();
        }
        return unsupported(execution.getClass());
    }

    private static String renderServiceExecutionRuntime(Runtime runtime, int baseIndentation, PureGrammarComposerContext context)
    {
        if (runtime instanceof LegacyRuntime)
        {
            return renderServiceExecutionRuntime(((LegacyRuntime) runtime).toEngineRuntime(), baseIndentation, context);
        }
        else if (runtime instanceof EngineRuntime)
        {
            return getTabString(baseIndentation) + "runtime:\n" +
                    getTabString(baseIndentation) +
                    "#{" +
                    HelperRuntimeGrammarComposer.renderRuntimeValue((EngineRuntime) runtime, baseIndentation + 1, true, DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) +
                    "\n" + getTabString(baseIndentation) + "}#;";
        }
        else if (runtime instanceof RuntimePointer)
        {
            return getTabString(baseIndentation) + "runtime: " + ((RuntimePointer) runtime).runtime + ";";
        }
        return unsupported(runtime.getClass(), "runtime type");
    }

    private static String renderKeyedExecutionParameter(KeyedExecutionParameter keyedExecutionParameter, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder().append(getTabString(baseIndentation)).append("executions[").append(convertString(keyedExecutionParameter.key, true)).append("]:\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(getTabString(baseIndentation + 1)).append("mapping: ").append(keyedExecutionParameter.mapping).append(";\n");
        builder.append(renderServiceExecutionRuntime(keyedExecutionParameter.runtime, baseIndentation + 1, context)).append("\n");
        return builder.append(getTabString(baseIndentation)).append("}").toString();
    }

    public static String renderServiceTestSuite(ServiceTestSuite serviceTestSuite, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append(serviceTestSuite.id).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");

        // testData

        if(serviceTestSuite.testData != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("data").append(":\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");

            if(serviceTestSuite.testData.connectionsTestData != null && !serviceTestSuite.testData.connectionsTestData.isEmpty())
            {
                str.append(getTabString(baseIndentation + 2)).append("connections").append(":\n");
                str.append(getTabString(baseIndentation + 2)).append("[\n");
                str.append(String.join(",\n", ListIterate.collect(serviceTestSuite.testData.connectionsTestData, data -> renderConnectionData(data, baseIndentation + 3, context)))).append("\n");
                str.append(getTabString(baseIndentation + 2)).append("]\n");
            }

            str.append(getTabString(baseIndentation + 1)).append("]\n");
        }

        // tests
        if(serviceTestSuite.tests != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("tests").append(":\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(serviceTestSuite.tests, test -> renderServiceTest((ServiceTest) test, baseIndentation + 2, context)))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("]\n");
        }

        str.append(getTabString(baseIndentation)).append("}");

        return str.toString();
    }

    private static String renderConnectionData(ConnectionTestData connectionData, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append(connectionData.id).append(":\n");
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(connectionData.data, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(baseIndentation + 1)).build()));

        return str.toString();
    }

    private static String renderServiceTest(ServiceTest test, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append(test.id).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");

        // Parameters
        if(test.parameters != null && !test.parameters.isEmpty())
        {
            str.append(getTabString(baseIndentation + 1)).append("parameters:\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(test.parameters, param -> renderServiceTestParameter(param, baseIndentation + 2, context)))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("]\n");
        }

        // Asserts
        if(test.assertions != null)
        {
            str.append(getTabString(baseIndentation + 1)).append("asserts:\n");
            str.append(getTabString(baseIndentation + 1)).append("[\n");
            str.append(String.join(",\n", ListIterate.collect(test.assertions, assertion -> HelperTestAssertionGrammarComposer.composeTestAssertion(assertion, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(baseIndentation + 2)).build())))).append("\n");
            str.append(getTabString(baseIndentation + 1)).append("]\n");
        }

        str.append(getTabString(baseIndentation)).append("}");

        return str.toString();
    }

    private static String renderServiceTestParameter(ParameterValue parameterValue, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append(parameterValue.name);
        str.append(" = ");
        str.append(parameterValue.value.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()));

        return str.toString();
    }

    public static String renderServiceTest(ServiceTest_Legacy serviceTest, PureGrammarComposerContext context)
    {
        int baseIndentation = 1;
        if (serviceTest instanceof SingleExecutionTest)
        {
            SingleExecutionTest singleExecutionTest = (SingleExecutionTest) serviceTest;
            StringBuilder builder = new StringBuilder().append("Single\n");
            appendTabString(builder, baseIndentation).append("{\n");
            appendTabString(builder, baseIndentation + 1).append("data: ").append(convertString(singleExecutionTest.data, true)).append(";\n");
            appendTabString(builder, baseIndentation + 1).append("asserts:\n").append(renderTestContainers(singleExecutionTest.asserts, baseIndentation + 1, context)).append("\n");
            return builder.append(getTabString(baseIndentation)).append("}\n").toString();
        }
        else if (serviceTest instanceof MultiExecutionTest)
        {
            MultiExecutionTest multiExecutionTest = (MultiExecutionTest) serviceTest;
            StringBuilder builder = new StringBuilder().append("Multi\n");
            appendTabString(builder, baseIndentation).append("{\n");
            builder.append(LazyIterate.collect(multiExecutionTest.tests, test -> renderKeyedSingleExecution(test, context)).makeString("\n"));
            return builder.append("\n").append(getTabString(baseIndentation)).append("}\n").toString();
        }
        return unsupported(serviceTest.getClass());
    }

    private static String renderKeyedSingleExecution(KeyedSingleExecutionTest keyedSingleExecutionTest, PureGrammarComposerContext context)
    {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder().append(getTabString(baseIndentation)).append("tests[").append(convertString(keyedSingleExecutionTest.key, true)).append("]:\n");
        builder.append(getTabString(baseIndentation)).append("{\n");
        builder.append(getTabString(baseIndentation + 1)).append("data: ").append(convertString(keyedSingleExecutionTest.data, true)).append(";").append("\n");
        builder.append(getTabString(baseIndentation + 1)).append("asserts:\n").append(renderTestContainers(keyedSingleExecutionTest.asserts, baseIndentation + 1, context));
        return builder.append("\n").append(getTabString(baseIndentation)).append("}").toString();
    }

    private static String renderTestContainers(List<TestContainer> testContainers, int indentation, PureGrammarComposerContext context)
    {
        StringBuilder builder = new StringBuilder().append(getTabString(indentation)).append("[\n");
        builder.append(LazyIterate.collect(testContainers, testContainer -> getTabString(indentation + 1)
                + "{ " + "[" + LazyIterate.collect(testContainer.parametersValues, parameter -> parameter.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build())).makeString(", ") + "]" + ", "
                + testContainer._assert.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) + " }").makeString(",\n")).append(testContainers.isEmpty() ? "" : "\n");
        builder.append(getTabString(indentation)).append("];");
        return builder.toString();
    }

    public static String renderServiceTelemetry(List<ServiceTag> tags, PureGrammarComposerContext context) {
        int baseIndentation = 2;
        StringBuilder builder = new StringBuilder().append(getTabString(baseIndentation)).append("metricGroups:\n").append(getTabString(baseIndentation)).append("[\n");
        builder.append(LazyIterate.collect(tags, o -> getTabString(baseIndentation+1) + convertString(o.value, true)).makeString(",\n")).append("\n").append(getTabString(baseIndentation)).append("];\n");
        return builder.toString();
    }
}
