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

package org.finos.legend.engine.language.pure.grammar.from.test.assertion;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class HelperTestAssertionGrammarParser
{
    /**
     * Helper for parsing test assertion grammar.  To use your grammar should include the definitions from
     * TestAssertionParserGrammar.g4:
     *
     * <pre>
     *     testAssertion:              identifier ISLAND_OPEN (testAssertionContent)*
     *     ;
     *     testAssertionContent:        ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
     *     ;
     * </pre>
     * <p>
     * You can then call by passing the testAssertionContext, ParseTreeWalkerSourceInformation and the PureGrammarParserExtensions from your context:
     *
     * <pre>
     *     HelperTestAssertionGrammarParser.parseTestAssertion(ctx.testAssertion(), walkerSourceInformation, extensions)
     * </pre>
     */
    public static TestAssertion parseTestAssertion(ParserRuleContext testAssertionContext, ParseTreeWalkerSourceInformation parentSourceInformation, PureGrammarParserExtensions extensions)
    {
        List<ParseTree> children = testAssertionContext.children;
        if (children.size() < 3 || !children.get(0).getClass().getSimpleName().equals("IdentifierContext") || !(children.get(1) instanceof TerminalNode))
        {
            throw new IllegalStateException("Unrecognized test assertion pattern");
        }

        ParserRuleContext identifierContext = (ParserRuleContext) children.get(0);
        TerminalNode islandOpen = (TerminalNode) children.get(1);
        List<ParseTree> content = children.subList(2, children.size());
        if (!content.stream().allMatch(ch -> ch.getClass().getSimpleName().equals("TestAssertionContentContext")))
        {
            throw new IllegalStateException("Unrecognized test assertion pattern");
        }

        String assertionType = PureGrammarParserUtility.fromIdentifier(identifierContext);
        TestAssertionParser parser = extensions.getExtraTestAssertionParser(assertionType);
        if (parser == null)
        {
            throw new EngineException("Unknown test assertion type: " + assertionType, parentSourceInformation.getSourceInformation(identifierContext), EngineErrorType.PARSER);
        }

        StringBuilder builder = new StringBuilder();
        content.forEach(cc -> builder.append(cc.getText()));
        builder.setLength(Math.max(0, builder.length() - 2));
        String text = builder.toString();

        // prepare island grammar walker source information
        int startLine = islandOpen.getSymbol().getLine();
        int lineOffset = parentSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? parentSourceInformation.getColumnOffset() : 0) + islandOpen.getSymbol().getCharPositionInLine() + islandOpen.getSymbol().getText().length();
        ParseTreeWalkerSourceInformation walkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(parentSourceInformation).withLineOffset(lineOffset).withColumnOffset(columnOffset).build();
        SourceInformation sourceInformation = parentSourceInformation.getSourceInformation(testAssertionContext);

        if (text.isEmpty())
        {
            throw new EngineException("Test Assertion must not be empty", sourceInformation, EngineErrorType.PARSER);
        }

        return parser.parse(text, walkerSourceInformation, sourceInformation, extensions);
    }
}
