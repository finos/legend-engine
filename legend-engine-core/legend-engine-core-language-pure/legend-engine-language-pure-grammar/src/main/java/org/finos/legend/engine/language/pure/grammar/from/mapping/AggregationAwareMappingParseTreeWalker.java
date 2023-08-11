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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.aggregationAware.AggregationAwareParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateFunction;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSetImplementationContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwarePropertyMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.GroupByFunction;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

public class AggregationAwareMappingParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;
    private final MappingElementSourceCode mappingElementSourceCode;

    public AggregationAwareMappingParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, CharStream input, PureGrammarParserContext parserContext, MappingElementSourceCode mappingElementSourceCode)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.parserContext = parserContext;
        this.mappingElementSourceCode = mappingElementSourceCode;
    }

    public void visitAggregationAwareMapping(AggregationAwareParserGrammar.AggregationAwareClassMappingContext ctx, AggregationAwareClassMapping aggregationAwareClassMapping)
    {
        visitMainMapping(ctx.mainMapping(), aggregationAwareClassMapping);
        aggregationAwareClassMapping.aggregateSetImplementations = ListIterate.collect(ctx.aggregationSpecification(), aggregationSpecificationContext ->
                visitAggregationSpecificationContainer(aggregationSpecificationContext, aggregationAwareClassMapping, ctx));
    }

    public void visitAggregateSpecification(AggregationAwareParserGrammar.AggregateSpecificationContext ctx, AggregateSpecification specification)
    {
        specification.canAggregate = ctx.BOOLEAN().getText().equals("true");
        specification.groupByFunctions = ListIterate.collect(ctx.groupByFunctionSpecifications().groupByFunctionSpecification(), this::visitGroupByFunction);
        specification.aggregateValues = ListIterate.collect(ctx.aggregationFunctionSpecifications().aggregationFunctionSpecification(), this::visitAggregateValue);
    }

    private void visitMainMapping(AggregationAwareParserGrammar.MainMappingContext ctx, AggregationAwareClassMapping mapping)
    {
        String parserName = ctx.parserName().getText();
        int startLine = ctx.BRACE_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.BRACE_OPEN().getText().length();

        MappingElementParser extraParser = this.parserContext.getPureGrammarParserExtensions().getExtraMappingElementParser(parserName);
        String mappingInput = this.input.getText(new Interval(ctx.BRACE_OPEN().getSymbol().getStartIndex() + 1, ctx.MAPPING_ISLAND_BRACE_CLOSE().getSymbol().getStartIndex() - 1));

        ParseTreeWalkerSourceInformation mappingElementWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(this.walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        MappingElementSourceCode mappingElementSourceCode = new MappingElementSourceCode(mappingInput, parserName, mappingElementWalkerSourceInformation,
                this.mappingElementSourceCode.mappingElementParserRuleContext, this.walkerSourceInformation);


        Object mappingElement = extraParser.parse(mappingElementSourceCode, this.parserContext);
        if (mappingElement instanceof ClassMapping)
        {
            ClassMapping classMapping = (ClassMapping) mappingElement;
            classMapping.id = classMapping.id == null ? mapping.id + "_Main" : classMapping.id + "_Main";
            mapping.mainSetImplementation = classMapping;

            if (classMapping instanceof PureInstanceClassMapping)
            {
                mapping.propertyMappings = ListIterate.collect(((PureInstanceClassMapping) classMapping).propertyMappings, this::visitAggregationAwarePropertyMapping);
            }
        }
        else
        {
            throw new EngineException("Invalid parser result for " + mappingElementSourceCode.name + ": " + mappingElement, this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

    }

    private AggregateSetImplementationContainer visitAggregationSpecificationContainer(AggregationAwareParserGrammar.AggregationSpecificationContext ctx, AggregationAwareClassMapping mapping, AggregationAwareParserGrammar.AggregationAwareClassMappingContext parent)
    {
        AggregateSetImplementationContainer aggregateSetImplementationContainer = new AggregateSetImplementationContainer();
        aggregateSetImplementationContainer.index = (long) parent.aggregationSpecification().indexOf(ctx);
        visitModelOperationContext(ctx.modelOperation(), aggregateSetImplementationContainer);
        visitAggregateMappingContext(ctx.aggregateMapping(), aggregateSetImplementationContainer, mapping);
        return aggregateSetImplementationContainer;
    }

    private void visitModelOperationContext(AggregationAwareParserGrammar.ModelOperationContext ctx, AggregateSetImplementationContainer aggregateSetImplementationContainer)
    {
        int startLine = ctx.start.getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.BRACE_OPEN().getText().length();

        MappingElementParser extraParser = this.parserContext.getPureGrammarParserExtensions().getExtraMappingElementParser("AggregateSpecification");
        String inputText = this.input.getText(new Interval(ctx.BRACE_OPEN().getSymbol().getStartIndex() + 1, ctx.MAPPING_ISLAND_BRACE_CLOSE().getSymbol().getStartIndex() - 1));

        ParseTreeWalkerSourceInformation sourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        MappingElementSourceCode sourceCode = new MappingElementSourceCode(inputText, "AggregateSpecification", sourceInformation, this.mappingElementSourceCode.mappingElementParserRuleContext, this.walkerSourceInformation);

        Object element = extraParser.parse(sourceCode, this.parserContext);
        if (element instanceof AggregateSpecification)
        {
            aggregateSetImplementationContainer.aggregateSpecification = (AggregateSpecification) element;
        }
        else
        {
            throw new EngineException("Invalid parser result for " + mappingElementSourceCode.name + ": " + element, this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
    }

    private void visitAggregateMappingContext(AggregationAwareParserGrammar.AggregateMappingContext ctx, AggregateSetImplementationContainer aggregateSetImplementationContainer, AggregationAwareClassMapping parent)
    {
        String parserName = ctx.parserName().getText();
        int startLine = ctx.BRACE_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.BRACE_OPEN().getText().length();

        MappingElementParser extraParser = this.parserContext.getPureGrammarParserExtensions().getExtraMappingElementParser(parserName);
        String mappingInput = this.input.getText(new Interval(ctx.BRACE_OPEN().getSymbol().getStartIndex() + 1, ctx.MAPPING_ISLAND_BRACE_CLOSE().getSymbol().getStartIndex() - 1));

        ParseTreeWalkerSourceInformation mappingElementWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(this.walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        MappingElementSourceCode mappingElementSourceCode = new MappingElementSourceCode(mappingInput, parserName, mappingElementWalkerSourceInformation, this.mappingElementSourceCode.mappingElementParserRuleContext, this.walkerSourceInformation);


        Object mappingElement = extraParser.parse(mappingElementSourceCode, this.parserContext);
        if (mappingElement instanceof ClassMapping)
        {
            ClassMapping classMapping = (ClassMapping) mappingElement;
            classMapping.id = classMapping.id == null ? parent.id + "_Aggregate_" + aggregateSetImplementationContainer.index :
                    classMapping.id + "_Aggregate_" + aggregateSetImplementationContainer.index;
            aggregateSetImplementationContainer.setImplementation = classMapping;
        }
        else
        {
            throw new EngineException("Invalid parser result for " + mappingElementSourceCode.name + ": " + mappingElement, this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }

    }

    private GroupByFunction visitGroupByFunction(AggregationAwareParserGrammar.GroupByFunctionSpecificationContext ctx)
    {
        Lambda lambda = visitLambda(ctx.combinedExpression());
        GroupByFunction groupByFunction = new GroupByFunction();
        groupByFunction.groupByFn = lambda;

        return groupByFunction;
    }

    private AggregateFunction visitAggregateValue(AggregationAwareParserGrammar.AggregationFunctionSpecificationContext ctx)
    {
        AggregateFunction aggregateFunction = new AggregateFunction();
        aggregateFunction.aggregateFn = visitLambda(ctx.aggregateFunction().combinedExpression());
        aggregateFunction.mapFn = visitLambda(ctx.mapFunction().combinedExpression());

        return aggregateFunction;
    }

    private Lambda visitLambda(AggregationAwareParserGrammar.CombinedExpressionContext ctx)
    {
        // Build source info for Lambda
        int startLine = ctx.start.getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();

        ParseTreeWalkerSourceInformation combinedExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();

        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = new DomainParser().parseCombinedExpression(lambdaString, combinedExpressionSourceInformation, this.parserContext);

        Lambda lambda = new Lambda();
        lambda.body = Collections.singletonList(valueSpecification);
        return lambda;
    }

    public AggregationAwarePropertyMapping visitAggregationAwarePropertyMapping(PropertyMapping propertyMapping)
    {
        AggregationAwarePropertyMapping aggregationAwarePropertyMapping = new AggregationAwarePropertyMapping();

        aggregationAwarePropertyMapping.property = propertyMapping.property;
        aggregationAwarePropertyMapping.source = propertyMapping.source;
        aggregationAwarePropertyMapping.target = propertyMapping.target;
        aggregationAwarePropertyMapping.sourceInformation = propertyMapping.sourceInformation;

        return aggregationAwarePropertyMapping;
    }
}

