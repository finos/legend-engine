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

package org.finos.legend.engine.language.pure.grammar.to.data.contentPattern;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Optional;

public class HelperContentPatternGrammarComposer
{
    public static <T extends ContentPattern> String composeContentPattern(ContentPattern contentPattern, PureGrammarComposerContext context)
    {
        Optional<ContentPatternGrammarComposer> composerExtension = ListIterate.detectOptional(ContentPatternComposerExtensionLoader.extensions(), extension -> extension.supports(contentPattern));

        if (composerExtension.isPresent())
        {
            PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(context.getIndentationString() + PureGrammarComposerUtility.getTabString()).build();
            ContentWithType contentWithType = composerExtension.get().compose(contentPattern, updatedContext);

            return context.getIndentationString() + contentWithType.type + "\n"
                    + context.getIndentationString() + "#{\n"
                    + contentWithType.content + "\n"
                    + context.getIndentationString() + "}#";
        }
        else
        {
            throw new EngineException("No composer extension for contentPattern pattern type : " + contentPattern.getClass().getSimpleName(), contentPattern.sourceInformation, EngineErrorType.PARSER);
        }
    }
}
