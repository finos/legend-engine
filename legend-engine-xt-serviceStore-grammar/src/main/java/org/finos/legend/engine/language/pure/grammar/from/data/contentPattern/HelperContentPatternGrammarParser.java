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

package org.finos.legend.engine.language.pure.grammar.from.data.contentPattern;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class HelperContentPatternGrammarParser
{
    /**
     * Helper for parsing contentPattern pattern grammar.  To use your grammar should include the definitions from
     * ServiceStoreEmbeddedDataParserGrammar.g4:
     *
     * <pre>
     *     serviceRequestContentPattern:               identifier ISLAND_OPEN ( serviceRequestContentPatternContent )*
     *     ;
     *     serviceRequestContentPatternContent:        ISLAND_START | ISLAND_BRACE_OPEN | ISLAND_CONTENT | ISLAND_HASH | ISLAND_BRACE_CLOSE | ISLAND_END
     *     ;
     * </pre>
     * <p>
     * You can then call by passing the serviceRequestContentPatternContext, ParseTreeWalkerSourceInformation and the PureGrammarParserExtensions from your context:
     *
     * <pre>
     *     HelperContentPatternGrammarParser.parseContentPattern(ctx.serviceRequestContentPattern(), walkerSourceInformation, extensions)
     * </pre>
     */
    public static ContentPattern parseContentPattern(ParserRuleContext contentPatternContext, ParseTreeWalkerSourceInformation parentWalkerSourceInformation, PureGrammarParserExtensions extensions)
    {
        List<ParseTree> children = contentPatternContext.children;
        if (children.size() < 3 || !children.get(0).getClass().getSimpleName().equals("IdentifierContext") || !(children.get(1) instanceof TerminalNode))
        {
            throw new IllegalStateException("Unrecognized contentPattern pattern");
        }

        ParserRuleContext identifierContext = (ParserRuleContext) children.get(0);
        TerminalNode islandOpen = (TerminalNode) children.get(1);
        List<ParseTree> content = children.subList(2, children.size());
        if (!content.stream().allMatch(ch -> ch.getClass().getSimpleName().equals("ServiceRequestContentPatternContentContext")))
        {
            throw new IllegalStateException("Unrecognized contentPattern pattern");
        }

        String dataType = PureGrammarParserUtility.fromIdentifier(identifierContext);
        ContentPatternGrammarParser parser = ListIterate.detect(ContentPatternParserExtensionLoader.extensions(), e -> e.getType().equals(dataType));
        if (parser == null)
        {
            throw new EngineException("Unknown contentPattern pattern type: " + dataType, parentWalkerSourceInformation.getSourceInformation(identifierContext), EngineErrorType.PARSER);
        }

        StringBuilder builder = new StringBuilder();
        content.forEach(cc -> builder.append(cc.getText()));
        builder.setLength(Math.max(0, builder.length() - 2));
        String text = builder.toString();

        // prepare island grammar walker source information
        int startLine = islandOpen.getSymbol().getLine();
        int lineOffset = parentWalkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? parentWalkerSourceInformation.getColumnOffset() : 0) + islandOpen.getSymbol().getCharPositionInLine() + islandOpen.getSymbol().getText().length();
        ParseTreeWalkerSourceInformation walkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(parentWalkerSourceInformation).withLineOffset(lineOffset).withColumnOffset(columnOffset).build();
        SourceInformation sourceInformation = parentWalkerSourceInformation.getSourceInformation(contentPatternContext);

        if (text.isEmpty())
        {
            throw new EngineException("Content Pattern must not be empty", sourceInformation, EngineErrorType.PARSER);
        }

        return parser.parse(text, walkerSourceInformation, sourceInformation, extensions);
    }
}
