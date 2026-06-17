// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.dataquality.grammar.from;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureIslandGrammarSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityPersistenceStrategy;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.collections.api.block.function.Function;

public interface IDataQualityGrammarParserExtension extends PureGrammarParserExtension
{
    static Stream<IDataQualityGrammarParserExtension> getExtensions(PureGrammarParserExtensions context)
    {
        return context.getExtensions()
                .stream()
                .filter(IDataQualityGrammarParserExtension.class::isInstance)
                .map(IDataQualityGrammarParserExtension.class::cast);
    }

    default List<Function<PureIslandGrammarSourceCode, DataQualityPersistenceStrategy>> getExtraDataQualityPersistenceStrategyParsers()
    {
        return Collections.emptyList();
    }

    static DataQualityPersistenceStrategy parsePersistenceStrategy(ParserRuleContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserExtensions context)
    {
        return parseIsland(ctx, walkerSourceInformation, IDataQualityGrammarParserExtension.getExtensions(context).map(IDataQualityGrammarParserExtension::getExtraDataQualityPersistenceStrategyParsers).flatMap(List::stream));
    }

    static DataQualityPersistenceStrategy parseIsland(ParserRuleContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation, Stream<Function<PureIslandGrammarSourceCode, DataQualityPersistenceStrategy>> processors)
    {
        Assert.assertTrue(ctx.getChildCount() == 2, () -> "Wrong number of token on persistence strategy island", walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        TerminalNode islandOpenToken = (TerminalNode) ctx.getChild(0);
        String islandOpen = islandOpenToken.getText();
        String type = islandOpen.substring(1, islandOpen.length() - 1).trim();

        Assert.assertTrue(!type.isEmpty(), () -> "Missing persistence strategy type.  Expect '# TYPE_HERE { ... }#'", walkerSourceInformation.getSourceInformation(islandOpenToken.getSymbol()), EngineErrorType.PARSER);

        ParserRuleContext islandContentContext = (ParserRuleContext) ctx.getChild(1);
        islandContentContext.removeLastChild();
        String islandCode = islandContentContext.getText();

        // prepare island grammar walker source information
        int startLine = islandOpenToken.getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + islandOpenToken.getSymbol().getCharPositionInLine() + islandOpenToken.getSymbol().getText().length();
        ParseTreeWalkerSourceInformation subWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(walkerSourceInformation.getReturnSourceInfo()).build();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        PureIslandGrammarSourceCode code = new PureIslandGrammarSourceCode(islandCode, type, sourceInformation, subWalkerSourceInformation);

        return processors.map(processor -> processor.apply(code))
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow(() -> new EngineException("Unsupported Persistence Strategy type '" + code.type + "'", code.sourceInformation, EngineErrorType.PARSER));
    }
}
