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

package org.finos.legend.engine.language.pure.grammar.to.test.assertion;

import org.finos.legend.engine.language.pure.grammar.from.test.assertion.EqualToGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.EqualToJsonGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.AssertAllRowsGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.AssertAllRows;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

import java.util.Objects;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperTestAssertionGrammarComposer
{
    public static String composeTestAssertion(TestAssertion testAssertion, PureGrammarComposerContext context)
    {
        String indentedString = context.getIndentationString() + PureGrammarComposerUtility.getTabString(1);
        String doubleIndentedString = indentedString + PureGrammarComposerUtility.getTabString(1);
        PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(doubleIndentedString).build();

        ContentWithType contentWithType = context.extraTestAssertionComposers
                .stream()
                .map(composer -> composer.value(testAssertion, updatedContext))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new UnsupportedOperationException(unsupported(testAssertion.getClass())));

        String assertionContent = indentedString + contentWithType.type + "\n"
                + indentedString + "#{\n"
                + contentWithType.content + "\n"
                + indentedString + "}#";

        return context.getIndentationString() + testAssertion.id + ":\n"
                + assertionContent;
    }

    public static ContentWithType composeCoreTestAssertion(TestAssertion testAssertion, PureGrammarComposerContext context)
    {
        String indentedString = context.getIndentationString() + PureGrammarComposerUtility.getTabString(1);
        PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentedString).build();

        if (testAssertion instanceof EqualTo)
        {
            EqualTo equalTo = (EqualTo) testAssertion;
            String content = context.getIndentationString() + "expected : \n"
                    + indentedString + equalTo.expected.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(updatedContext).build()) + ";";

            return new ContentWithType(EqualToGrammarParser.TYPE, content);
        }
        else if (testAssertion instanceof EqualToJson)
        {
            EqualToJson equalToJson = (EqualToJson) testAssertion;
            String content = context.getIndentationString() + "expected : \n"
                    + HelperEmbeddedDataGrammarComposer.composeEmbeddedData(equalToJson.expected, updatedContext) + ";";

            return new ContentWithType(EqualToJsonGrammarParser.TYPE, content);
        }
        else if (testAssertion instanceof AssertAllRows)
        {
            AssertAllRows assertAllRows = (AssertAllRows) testAssertion;
            String content = context.getIndentationString() + "expected : \n"
                + HelperEmbeddedDataGrammarComposer.composeEmbeddedData(assertAllRows.expected, updatedContext) + ";";

            return new ContentWithType(AssertAllRowsGrammarParser.TYPE, content);
        }
        else
        {
            return null;
        }
    }
}
