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

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.RelationElementsEmbeddedDataTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class HelperTestAssertionGrammarParser
{
    // -------------------------------------- Island Content Extraction --------------------------------------

    /**
     * Extracts raw text content from island grammar content contexts (the tokens between #{ and }#).
     * Strips the trailing }# (last 2 characters).
     */
    public static String extractIslandContent(List<? extends ParseTree> contentContexts)
    {
        StringBuilder builder = new StringBuilder();
        contentContexts.forEach(cc -> builder.append(cc.getText()));
        if (builder.length() >= 2)
        {
            builder.setLength(builder.length() - 2);
        }
        return builder.toString();
    }

    /**
     * Builds a ParseTreeWalkerSourceInformation for island grammar content,
     * adjusting line/column offsets based on the island open token position.
     */
    public static ParseTreeWalkerSourceInformation buildIslandSourceInformation(
            TerminalNode islandOpen,
            ParseTreeWalkerSourceInformation parentWalkerSourceInformation)
    {
        int startLine = islandOpen.getSymbol().getLine();
        int lineOffset = parentWalkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? parentWalkerSourceInformation.getColumnOffset() : 0)
                + islandOpen.getSymbol().getCharPositionInLine()
                + islandOpen.getSymbol().getText().length();
        return new ParseTreeWalkerSourceInformation.Builder(parentWalkerSourceInformation)
                .withLineOffset(lineOffset)
                .withColumnOffset(columnOffset)
                .build();
    }

    // -------------------------------------- Relation Parsing --------------------------------------

    /**
     * Parses flat CSV content into a RelationElement using the RelationElementsData grammar.
     * Prepends ':' to trigger TABLE_START in the lexer (entering TABLE_MODE).
     *
     * @param flatCsvContent the raw CSV text (column headers + rows)
     * @param innerWalkerSourceInformation source information for error reporting
     * @param sourceInformation source information for the resulting element
     * @return the parsed RelationElement with columns and rows
     */
    public static RelationElement parseRelationElement(
            String flatCsvContent,
            ParseTreeWalkerSourceInformation innerWalkerSourceInformation,
            SourceInformation sourceInformation)
    {
        // Prepend ':' to trigger TABLE_START in the lexer, entering TABLE_MODE for flat CSV parsing
        String content = ": " + flatCsvContent.trim();

        org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(content);
        ParserErrorListener errorListener = new ParserErrorListener(innerWalkerSourceInformation);
        org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.relation.RelationElementsDataLexerGrammar lexer =
                new org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.relation.RelationElementsDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.relation.RelationElementsDataParserGrammar parser =
                new org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.relation.RelationElementsDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        RelationElementsEmbeddedDataTreeWalker treeWalker = new RelationElementsEmbeddedDataTreeWalker(innerWalkerSourceInformation, sourceInformation, null);
        org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElementsData elementsData = treeWalker.visit(parser.definition());

        if (elementsData.relationElements == null || elementsData.relationElements.isEmpty())
        {
            throw new EngineException("Expected at least one relation element in Relation assertion", sourceInformation, EngineErrorType.PARSER);
        }

        return elementsData.relationElements.get(0);
    }

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
    public static TestAssertion parseTestAssertion(ParserRuleContext testAssertionContext, ParseTreeWalkerSourceInformation parentWalkerSourceInformation, PureGrammarParserExtensions extensions)
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
            throw new EngineException("Unknown test assertion type: " + assertionType, parentWalkerSourceInformation.getSourceInformation(identifierContext), EngineErrorType.PARSER);
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
        SourceInformation sourceInformation = parentWalkerSourceInformation.getSourceInformation(testAssertionContext);

        if (text.isEmpty())
        {
            throw new EngineException("Test Assertion must not be empty", sourceInformation, EngineErrorType.PARSER);
        }

        return parser.parse(text, walkerSourceInformation, sourceInformation, extensions);
    }
}
