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
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.ExpectedOutputMappingTestAssert;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MappingParseTreeWalker
{
    private final CharStream input;
    private final PureModelContextData pureModelContextData;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;
    private final ImportAwareCodeSection section;
    private final List<Procedure3<MappingElementSourceCode, Mapping, PureGrammarParserContext>> extraMappingElementParsers;
    private final List<Function3<String, MappingParserGrammar.TestInputElementContext, ParseTreeWalkerSourceInformation, InputData>> extraMappingTestInputDataParsers;

    public MappingParseTreeWalker(CharStream input, List<Procedure3<MappingElementSourceCode, Mapping, PureGrammarParserContext>> extraMappingElementParsers, List<Function3<String, MappingParserGrammar.TestInputElementContext, ParseTreeWalkerSourceInformation, InputData>> extraMappingTestInputDataParsers, ParseTreeWalkerSourceInformation walkerSourceInformation, PureModelContextData pureModelContextData, PureGrammarParserContext parserContext, ImportAwareCodeSection section)
    {
        this.input = input;
        this.extraMappingElementParsers = extraMappingElementParsers;
        this.extraMappingTestInputDataParsers = extraMappingTestInputDataParsers;
        this.walkerSourceInformation = walkerSourceInformation;
        this.pureModelContextData = pureModelContextData;
        this.parserContext = parserContext;
        this.section = section;
    }

    public void visitDefinition(MappingParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        List<Mapping> elements = ListIterate.collect(ctx.mapping(), this::visitMapping);
        this.section.elements = ListIterate.collect(elements, PackageableElement::getPath);
        this.pureModelContextData.mappings.addAll(elements);
    }

    private Mapping visitMapping(MappingParserGrammar.MappingContext ctx)
    {
        Mapping mapping = new Mapping();
        mapping.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        mapping._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        mapping.classMappings = new ArrayList<>();
        mapping.enumerationMappings = new ArrayList<>();
        mapping.associationMappings = new ArrayList<>();
        mapping.includedMappings = ctx.includeMapping() == null ? Collections.emptyList() : ListIterate.collect(ctx.includeMapping(), this::visitMappingInclude);
        mapping.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        ListIterate.collect(ctx.mappingElement(), mappingElementContext -> visitMappingElement(mappingElementContext, mapping));
        mapping.tests = ctx.tests() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.tests().test(), testContext -> this.visitMappingTest(testContext, mapping));
        return mapping;
    }

    private MappingInclude visitMappingInclude(MappingParserGrammar.IncludeMappingContext ctx)
    {
        MappingInclude mappingInclude = new MappingInclude();
        mappingInclude.setIncludedMapping(PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier()));
        mappingInclude.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        // TODO support storeSubPath
        mappingInclude.sourceDatabasePath = null;
        mappingInclude.targetDatabasePath = null;
        return mappingInclude;
    }

    private Mapping visitMappingElement(MappingParserGrammar.MappingElementContext ctx, Mapping mapping)
    {
        String parserName = ctx.parserName().getText();
        // Construct the mapping element string (with spacing) to be dispatched to another parser
        // NOTE: we want to preserve the spacing so we can correctly produce source information in the dispatched parser
        StringBuilder mappingElementStringBuilder = new StringBuilder();
        for (MappingParserGrammar.MappingElementBodyContentContext fragment : ctx.mappingElementBody().mappingElementBodyContent())
        {
            mappingElementStringBuilder.append(fragment.getText());
        }
        String mappingElementCode = mappingElementStringBuilder.length() > 0 ? mappingElementStringBuilder.substring(0, mappingElementStringBuilder.length() - 1) : mappingElementStringBuilder.toString();
        // prepare island grammar walker source information
        int startLine = ctx.mappingElementBody().BRACE_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.mappingElementBody().BRACE_OPEN().getSymbol().getCharPositionInLine() + ctx.mappingElementBody().BRACE_OPEN().getText().length();
        ParseTreeWalkerSourceInformation mappingElementWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(mapping.getPath(), lineOffset, columnOffset).build();
        MappingElementSourceCode mappingElementSourceCode = new MappingElementSourceCode(mappingElementCode, parserName, mappingElementWalkerSourceInformation, ctx, this.walkerSourceInformation);
        this.extraMappingElementParsers.forEach(parser -> parser.value(mappingElementSourceCode, mapping, this.parserContext));
        return mapping;
    }

    private MappingTest visitMappingTest(MappingParserGrammar.TestContext ctx, Mapping mapping)
    {
        MappingTest mappingTest = new MappingTest();
        mappingTest.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        mappingTest.name = ctx.testName().getText();
        // query
        MappingParserGrammar.TestQueryContext testQueryContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testQuery(), "query", mappingTest.sourceInformation);
        if (testQueryContext.combinedExpression() != null)
        {
            mappingTest.query = this.visitMappingTestQuery(testQueryContext.combinedExpression(), mapping);
        }
        // input data
        MappingParserGrammar.TestInputDataContext inputDataContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testInputData(), "data", mappingTest.sourceInformation);
        mappingTest.inputData = inputDataContext.testInput() != null && inputDataContext.testInput().testInputElement() != null ? ListIterate.collect(inputDataContext.testInput().testInputElement(), this::visitMappingTestInputData) : Lists.mutable.empty();
        // assert
        MappingParserGrammar.TestAssertContext testAssertContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testAssert(), "assert", mappingTest.sourceInformation);
        // NOTE: it's important to have `STRING` before `combinedExpression` since the latter matches the former
        if (testAssertContext.STRING() != null)
        {
            ExpectedOutputMappingTestAssert expectedOutputMappingTestAssert = new ExpectedOutputMappingTestAssert();
            expectedOutputMappingTestAssert.expectedOutput = PureGrammarParserUtility.fromGrammarString(testAssertContext.STRING().getText(), false);
            expectedOutputMappingTestAssert.sourceInformation = this.walkerSourceInformation.getSourceInformation(testAssertContext);
            mappingTest._assert = expectedOutputMappingTestAssert;
        }
        else if (testAssertContext.combinedExpression() != null)
        {
            throw new EngineException("Mapping test currently only support string assertion", this.walkerSourceInformation.getSourceInformation(testAssertContext), EngineErrorType.PARSER);
        }
        return mappingTest;
    }

    private InputData visitMappingTestInputData(MappingParserGrammar.TestInputElementContext ctx)
    {
        SourceInformation testInputDataSourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        String inputDataType = ctx.testInputType().getText();
        if ("Object".equals(inputDataType))
        {
            ObjectInputData objectInputData = new ObjectInputData();
            objectInputData.sourceInformation = testInputDataSourceInformation;
            objectInputData.sourceClass = PureGrammarParserUtility.fromQualifiedName(ctx.testInputSrc().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.testInputSrc().qualifiedName().packagePath().identifier(), ctx.testInputSrc().qualifiedName().identifier());
            objectInputData.data = PureGrammarParserUtility.fromGrammarString(ctx.testInputDataContent().STRING().getText(), false);
            if (ctx.testInputFormat() != null)
            {
                try
                {
                    objectInputData.inputType = ObjectInputType.valueOf(ctx.testInputFormat().getText());
                }
                catch (IllegalArgumentException e)
                {
                    throw new EngineException("Mapping test object input data does not support format '" + ctx.testInputFormat().getText() + "'", this.walkerSourceInformation.getSourceInformation(ctx.testInputFormat()), EngineErrorType.PARSER);
                }
            }
            else
            {
                throw new EngineException("Mapping test object input data format type is missing", testInputDataSourceInformation, EngineErrorType.PARSER);
            }
            return objectInputData;
        }
        return this.extraMappingTestInputDataParsers.stream().map(parser -> parser.value(inputDataType, ctx, this.walkerSourceInformation)).filter(Objects::nonNull).findFirst()
                .orElseThrow(() -> new EngineException("Unsupported mapping test input data type '" + inputDataType + "'", testInputDataSourceInformation, EngineErrorType.PARSER));
    }

    private Lambda visitMappingTestQuery(MappingParserGrammar.CombinedExpressionContext ctx, Mapping mapping)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(mapping.getPath(), lineOffset, columnOffset).build();
        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionWalkerSourceInformation, null, null);
        if (valueSpecification instanceof Lambda)
        {
            return (Lambda) valueSpecification;
        }
        // NOTE: If the user just provides the body of the lambda, we will wrap a lambda around it
        // we might want to reconsider this behavior and throw error if this convenience causes any trouble
        Lambda lambda = new Lambda();
        lambda.body = new ArrayList<>();
        lambda.body.add(valueSpecification);
        lambda.parameters = new ArrayList<>();
        return lambda;
    }
}
