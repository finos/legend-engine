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

package org.finos.legend.engine.language.pure.dsl.persistence.cloud.grammar.to;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.cloud.context.AwsGluePersistencePlatform;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperPersistenceCloudComposer
{
    private HelperPersistenceCloudComposer()
    {
    }

    public static String renderAwsGluePersistencePlatform(AwsGluePersistencePlatform persistencePlatform, int indentLevel, PureGrammarComposerContext context)
    {
        return "AwsGlue\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "dataProcessingUnits: " + persistencePlatform.dataProcessingUnits + ";\n" +
                getTabString(indentLevel) + "}";
    }
}
