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

package org.finos.legend.engine.language.pure.grammar.from.mapping;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.modelJoinAssociationMapping.ModelJoinAssociationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.modelJoin.ModelJoinAssociationMapping;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

public class ModelJoinAssociationMappingParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;

    public ModelJoinAssociationMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, CharStream input, PureGrammarParserContext parserContext)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
    }

    public void visitModelJoinAssociationMapping(ModelJoinAssociationMappingParserGrammar.ModelJoinAssociationMappingContext ctx, ModelJoinAssociationMapping modelJoinAssociationMapping)
    {
        modelJoinAssociationMapping.joinCondition = visitLambda(ctx.combinedExpression());
    }

    private LambdaFunction visitLambda(ModelJoinAssociationMappingParserGrammar.CombinedExpressionContext ctx)
    {
        int startLine = ctx.start.getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combinedExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();

        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = new DomainParser().parseCombinedExpression(lambdaString, combinedExpressionSourceInformation, this.parserContext);

        if (!(valueSpecification instanceof LambdaFunction))
        {
            throw new EngineException("ModelJoin association mapping requires a lambda join condition of the form '{src: SrcClass[1], tgt: TgtClass[1] | <boolean expression>}'", this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        return (LambdaFunction) valueSpecification;
    }
}
