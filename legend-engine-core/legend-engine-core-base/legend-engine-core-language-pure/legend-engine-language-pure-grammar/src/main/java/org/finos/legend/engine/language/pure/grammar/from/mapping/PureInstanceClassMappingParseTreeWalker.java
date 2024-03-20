// Copyright 2020 Goldman Sachs
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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.LocalMappingPropertyInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PurePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.ArrayList;
import java.util.Collections;

public class PureInstanceClassMappingParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;

    public PureInstanceClassMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, CharStream input, PureGrammarParserContext parserContext)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
    }

    public void visitPureInstanceClassMapping(PureInstanceClassMappingParserGrammar.PureInstanceClassMappingContext ctx, MappingParserGrammar.MappingElementContext classMappingContext, PureInstanceClassMapping pureInstanceClassMapping)
    {
        // source (optional)
        PureInstanceClassMappingParserGrammar.MappingSrcContext mappingSrcContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.mappingSrc(), "~src", pureInstanceClassMapping.sourceInformation);
        if (mappingSrcContext != null)
        {
            pureInstanceClassMapping.srcClass = PureGrammarParserUtility.fromQualifiedName(mappingSrcContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingSrcContext.qualifiedName().packagePath().identifier(), mappingSrcContext.qualifiedName().identifier());
            pureInstanceClassMapping.sourceClassSourceInformation = walkerSourceInformation.getSourceInformation(mappingSrcContext.qualifiedName());
        }
        // filter (optional)
        PureInstanceClassMappingParserGrammar.MappingFilterContext mappingFilterContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.mappingFilter(), "~filter", pureInstanceClassMapping.sourceInformation);
        if (mappingFilterContext != null)
        {
            pureInstanceClassMapping.filter = visitLambda(mappingFilterContext.combinedExpression(), pureInstanceClassMapping);
        }
        // property mappings (optional)
        pureInstanceClassMapping.propertyMappings = ListIterate.collect(ctx.propertyMapping(), propertyMappingContext -> visitPurePropertyMapping(propertyMappingContext, classMappingContext, pureInstanceClassMapping));
    }

    private PurePropertyMapping visitPurePropertyMapping(PureInstanceClassMappingParserGrammar.PropertyMappingContext ctx, MappingParserGrammar.MappingElementContext classMappingContext, PureInstanceClassMapping pureInstanceClassMapping)
    {
        PurePropertyMapping purePropertyMapping = new PurePropertyMapping();

        if (ctx.PLUS() != null)
        {
            // Local property mapping
            purePropertyMapping.localMappingProperty = new LocalMappingPropertyInfo();
            purePropertyMapping.localMappingProperty.type = ctx.type().getText();
            purePropertyMapping.localMappingProperty.multiplicity = buildMultiplicity(ctx.multiplicity().multiplicityArgument());
            purePropertyMapping.localMappingProperty.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        }

        purePropertyMapping.property = new PropertyPointer();
        purePropertyMapping.property._class = ctx.PLUS() != null ? null : PureGrammarParserUtility.fromQualifiedName(classMappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : classMappingContext.qualifiedName().packagePath().identifier(), classMappingContext.qualifiedName().identifier());
        purePropertyMapping.property.property = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        purePropertyMapping.source = classMappingContext.mappingElementId() == null ? "" : classMappingContext.mappingElementId().getText();
        // This might looks strange but the parser rule looks like: sourceAndTargetMappingId: BRACKET_OPEN sourceId (COMMA targetId)? BRACKET_CLOSE
        // so in this case the class mapping ID is `sourceId`
        purePropertyMapping.target = ctx.sourceAndTargetMappingId() != null ? PureGrammarParserUtility.fromQualifiedName(ctx.sourceAndTargetMappingId().sourceId().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.sourceAndTargetMappingId().sourceId().qualifiedName().packagePath().identifier(), ctx.sourceAndTargetMappingId().sourceId().qualifiedName().identifier()) : null;
        if (ctx.ENUMERATION_MAPPING() != null)
        {
            purePropertyMapping.enumMappingId = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        }
        purePropertyMapping.transform = visitLambda(ctx.combinedExpression(), pureInstanceClassMapping);
        purePropertyMapping.property.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        purePropertyMapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        purePropertyMapping.explodeProperty = ctx.STAR() != null;
        return purePropertyMapping;
    }

    private Lambda visitLambda(PureInstanceClassMappingParserGrammar.CombinedExpressionContext ctx, PureInstanceClassMapping pureInstanceClassMapping)
    {
        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionSourceInformation, this.parserContext);
        // add source parameter
        Lambda lambda = new Lambda();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = Lists.mutable.empty();
        return lambda;
    }

    private static Multiplicity buildMultiplicity(PureInstanceClassMappingParserGrammar.MultiplicityArgumentContext ctx)
    {
        String star = "*";
        Multiplicity m = new Multiplicity();
        m.lowerBound = Integer.parseInt(ctx.fromMultiplicity() != null ? ctx.fromMultiplicity().getText() : star.equals(ctx.toMultiplicity().getText()) ? "0" : ctx.toMultiplicity().getText());
        m.setUpperBound(star.equals(ctx.toMultiplicity().getText()) ? null : Integer.parseInt(ctx.toMultiplicity().getText()));
        return m;
    }
}

