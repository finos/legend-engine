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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.xStoreAssociationMapping.XStoreAssociationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStoreAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStorePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.Collections;

public class XStoreAssociationMappingParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;

    public XStoreAssociationMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, CharStream input, PureGrammarParserContext parserContext)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
    }

    public void visitXStoreAssociationMapping(XStoreAssociationMappingParserGrammar.XStoreAssociationMappingContext ctx, XStoreAssociationMapping xStoreAssociationMapping)
    {
        xStoreAssociationMapping.propertyMappings = ListIterate.collect(ctx.xStorePropertyMapping(), x -> this.visitXStorePropertyMapping(x, xStoreAssociationMapping.association));
    }

    private XStorePropertyMapping visitXStorePropertyMapping(XStoreAssociationMappingParserGrammar.XStorePropertyMappingContext ctx, String association)
    {
        XStorePropertyMapping xStorePropertyMapping = new XStorePropertyMapping();

        xStorePropertyMapping.property = new PropertyPointer();
        xStorePropertyMapping.property._class = association;
        xStorePropertyMapping.property.property = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        xStorePropertyMapping.property.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.qualifiedName());

        xStorePropertyMapping.source = ctx.sourceAndTargetMappingId() == null ? "" : ctx.sourceAndTargetMappingId().sourceId().getText();
        xStorePropertyMapping.target = ctx.sourceAndTargetMappingId() == null ? "" : ctx.sourceAndTargetMappingId().targetId().getText();

        xStorePropertyMapping.crossExpression = visitLambda(ctx.combinedExpression());

        xStorePropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        return xStorePropertyMapping;
    }

    private Lambda visitLambda(XStoreAssociationMappingParserGrammar.CombinedExpressionContext ctx)
    {
        // Build source info for Lambda
        int startLine = ctx.start.getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combinedExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).build();

        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = new DomainParser().parseCombinedExpression(lambdaString, combinedExpressionSourceInformation, this.parserContext);

        Lambda lambda = new Lambda();
        lambda.body = Collections.singletonList(valueSpecification);
        return lambda;
    }
}

