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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionParameters;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperExecutionEnvironmentGrammarComposer
{
    protected static String renderSingleExecutionParameter(SingleExecutionParameters parameters, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation)).append(parameters.key).append(":\n");
        str.append(getTabString(baseIndentation)).append("{\n");
        str.append(getTabString(baseIndentation + 1)).append("mapping: ").append(parameters.mapping).append(";\n");
        str.append(HelperServiceGrammarComposer.renderServiceExecutionRuntime(parameters.runtime, baseIndentation + 1, context));
        str.append("\n").append(getTabString(baseIndentation)).append("}");
        return str.toString();
    }

    protected static String renderMultiExecutionParameter(MultiExecutionParameters parameters, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();
        str.append(getTabString(baseIndentation)).append(parameters.masterKey).append(":\n");
        str.append(getTabString(baseIndentation)).append("[\n");
        str.append(String.join(",\n", ListIterate.collect(parameters.singleExecutionParameters, execEnv -> renderSingleExecutionParameter(execEnv, baseIndentation + 1, context)))).append("\n");
        str.append(getTabString(baseIndentation)).append("]");
        return str.toString();
    }

    protected static String renderExecutionEnvironmentDetails(List<ExecutionParameters> execParameters, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder execEnvBuilder = new StringBuilder();
        execEnvBuilder.append(getTabString(baseIndentation)).append("executions:\n").append(getTabString(baseIndentation)).append("[\n");
        execEnvBuilder.append(String.join(",\n", ListIterate.collect(execParameters, execParam ->
        {
            if (execParam instanceof SingleExecutionParameters)
            {
                return HelperExecutionEnvironmentGrammarComposer.renderSingleExecutionParameter((SingleExecutionParameters) execParam, baseIndentation + 1, context);
            }
            return HelperExecutionEnvironmentGrammarComposer.renderMultiExecutionParameter((MultiExecutionParameters) execParam, baseIndentation + 1, context);
        }))).append("\n").append(getTabString(baseIndentation)).append("];\n");
        return execEnvBuilder.toString();
    }
}
