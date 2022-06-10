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

import org.finos.legend.engine.language.pure.grammar.from.data.contentPattern.EqualToContentPatternGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToPattern;

public class EqualToContentPatternGrammarComposer implements ContentPatternGrammarComposer
{
    @Override
    public boolean supports(ContentPattern contentPattern)
    {
        if (contentPattern instanceof EqualToPattern)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public ContentWithType compose(ContentPattern contentPattern, PureGrammarComposerContext context)
    {
        String content = context.getIndentationString() + "expected: " + PureGrammarComposerUtility.convertString(((EqualToPattern) contentPattern).expectedValue, true) + ";";
        return new ContentWithType(EqualToContentPatternGrammarParser.TYPE, content);
    }
}
