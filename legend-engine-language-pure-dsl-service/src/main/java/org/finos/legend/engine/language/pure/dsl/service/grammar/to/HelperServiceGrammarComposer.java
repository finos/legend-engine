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
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.HelperRuntimeGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedSingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;

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

    public static String renderServiceTest(ServiceTest serviceTest, PureGrammarComposerContext context)
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
        // TODO: handle parameters
        builder.append(LazyIterate.collect(testContainers, testContainer -> getTabString(indentation + 1) + "{ " + "[]" + ", " + testContainer._assert.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) + " }").makeString(",\n")).append(testContainers.isEmpty() ? "" : "\n");
        builder.append(getTabString(indentation)).append("];");
        return builder.toString();
    }
}
