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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingTestInputDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.HelperTestAssertionGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.AssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.StoreTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.MappingTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.ExpectedOutputMappingTestAssert;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Pair;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MappingParseTreeWalker
{
    private final CharStream input;
    private final Consumer<PackageableElement> elementConsumer;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext parserContext;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserExtensions extensions;

    public MappingParseTreeWalker(CharStream input, PureGrammarParserExtensions extensions, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext, ImportAwareCodeSection section)
    {
        this.input = input;
        this.extensions = extensions;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.parserContext = parserContext;
        this.section = section;
    }

    public void visitDefinition(MappingParserGrammar.DefinitionContext ctx)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.mapping().stream().map(this::visitMapping).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
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
        mapping.testSuites = ctx.mappingTestSuites() == null ? Lists.mutable.empty() : ListIterate.collect(ctx.mappingTestSuites().mappingTestSuite(), testSuiteContext -> this.visitMappingTestSuite(testSuiteContext, mapping));
        return mapping;
    }

    private MappingInclude visitMappingInclude(MappingParserGrammar.IncludeMappingContext ctx)
    {
        MappingInclude mappingInclude = new MappingInclude();
        mappingInclude.setIncludedMapping(PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier()));
        mappingInclude.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        List<MappingParserGrammar.StoreSubPathContext> storeSubPathContextList = ctx.storeSubPath();

        if (storeSubPathContextList.size() == 1)
        {
            visitStoreSubPath(storeSubPathContextList.get(0), mappingInclude);
        }
        else
        {
            mappingInclude.sourceDatabasePath = null;
            mappingInclude.targetDatabasePath = null;
        }
        return mappingInclude;
    }

    private MappingInclude visitStoreSubPath(MappingParserGrammar.StoreSubPathContext ctx, MappingInclude mappingInclude)
    {
        mappingInclude.sourceDatabasePath = PureGrammarParserUtility.fromQualifiedName(ctx.sourceStore().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.sourceStore().qualifiedName().packagePath().identifier(), ctx.sourceStore().qualifiedName().identifier());
        mappingInclude.targetDatabasePath = PureGrammarParserUtility.fromQualifiedName(ctx.targetStore().qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.targetStore().qualifiedName().packagePath().identifier(), ctx.targetStore().qualifiedName().identifier());
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
        ParseTreeWalkerSourceInformation mappingElementWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(mapping.getPath(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        MappingElementSourceCode mappingElementSourceCode = new MappingElementSourceCode(mappingElementCode, parserName, mappingElementWalkerSourceInformation, ctx, this.walkerSourceInformation);
        MappingElementParser extraParser = this.extensions.getExtraMappingElementParser(mappingElementSourceCode.name);
        if (extraParser == null)
        {
            throw new EngineException("No parser for " + mappingElementSourceCode.name, this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        Object mappingElement = extraParser.parse(mappingElementSourceCode, this.parserContext);
        if (mappingElement instanceof ClassMapping)
        {
            mapping.classMappings.add((ClassMapping) mappingElement);
        }
        else if (mappingElement instanceof EnumerationMapping)
        {
            mapping.enumerationMappings.add((EnumerationMapping) mappingElement);
        }
        else if (mappingElement instanceof AssociationMapping)
        {
            mapping.associationMappings.add((AssociationMapping) mappingElement);
        }
        else
        {
            throw new EngineException("Invalid parser result for " + mappingElementSourceCode.name + ": " + mappingElement, this.walkerSourceInformation.getSourceInformation(ctx), EngineErrorType.PARSER);
        }
        return mapping;
    }

    private MappingTestSuite visitMappingTestSuite(MappingParserGrammar.MappingTestSuiteContext ctx, Mapping mapping)
    {
        MappingTestSuite mappingTestSuite = new MappingTestSuite();
        mappingTestSuite.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        mappingTestSuite.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        List<MappingParserGrammar.MappingTestDataContext> validatedDataCtx = PureGrammarParserUtility.validateRequiredListField(ctx.mappingTestData(), "data", mappingTestSuite.sourceInformation);
        mappingTestSuite.storeTestDatas = ListIterate.collect(validatedDataCtx, this::visitMappingStoreTestData);
        List<MappingParserGrammar.MappingTestContext> validatedTestCtx = PureGrammarParserUtility.validateRequiredListField(ctx.mappingTest(), "tests", mappingTestSuite.sourceInformation);
        mappingTestSuite.tests = ListIterate.collect(validatedTestCtx, atomicTest -> this.visitAtomicTest(atomicTest, mapping));

        return mappingTestSuite;
    }

    private StoreTestData visitMappingStoreTestData(MappingParserGrammar.MappingTestDataContext ctx)
    {
        if (ctx.embeddedData() != null)
        {
            StoreTestData testData = new StoreTestData();
            testData.data = HelperEmbeddedDataGrammarParser.parseEmbeddedData(ctx.embeddedData(), this.walkerSourceInformation, this.parserContext.getPureGrammarParserExtensions());
            testData.store = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath().identifier(),ctx.qualifiedName().identifier());     //build store
            return testData;
        }
        else
        {
            return visitModelStoreTestData(ctx);
        }
    }

    private StoreTestData visitModelStoreTestData(MappingParserGrammar.MappingTestDataContext ctx)
    {
        StoreTestData modelStoreTestData = new StoreTestData();
        modelStoreTestData.data = HelperEmbeddedDataGrammarParser.parseEmbeddedData(ctx.modelEmbeddedData().embeddedData(), this.walkerSourceInformation, this.parserContext.getPureGrammarParserExtensions());
        modelStoreTestData.store = ctx.qualifiedName().getText();
        if (modelStoreTestData.data instanceof DataElementReference)
        {
            ModelStoreData modelData = new ModelStoreData();
            String clazz = PureGrammarParserUtility.fromQualifiedName(ctx.modelEmbeddedData().qualifiedName().packagePath().identifier(), ctx.modelEmbeddedData().qualifiedName().identifier());
            PackageableElementPtr ptr = new PackageableElementPtr();
            ptr.fullPath = ((DataElementReference) modelStoreTestData.data).dataElement;
            //creating bindingPtr
            Binding binding = new Binding();
            ModelUnit modelUnit = new ModelUnit();
            modelUnit.packageableElementIncludes = Collections.singletonList(clazz);
            binding.name = "binding";
            binding._package = "default::" + clazz;
            binding.modelUnit = modelUnit;
            binding.contentType = "application/json";    //default content type
            Stream.of(binding).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);

            PackageableElementPtr bindingPtr = new PackageableElementPtr();
            bindingPtr.fullPath = binding.getPath();
            Pair pair = new Pair();
            pair.first = bindingPtr;
            pair.second = ptr;
            modelData.instances = Maps.mutable.of(clazz, pair);
            modelStoreTestData.data = modelData;
        }
        else
        {
            throw new EngineException("unsupported data for m2m mappings, currently only external format data as references are supported");
        }
        return modelStoreTestData;
    }

    private MappingTest visitAtomicTest(MappingParserGrammar.MappingTestContext ctx, Mapping mapping)
    {
        MappingTest mappingTest = new MappingTest();
        mappingTest.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        mappingTest.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        mappingTest.query = this.visitMappingTestQuery(ctx.combinedExpression(), mapping);
        List<MappingParserGrammar.MappingTestAssertContext> assertCtx = PureGrammarParserUtility.validateRequiredListField(ctx.mappingTestAssert(), "asserts", mappingTest.sourceInformation);
        mappingTest.assertions = ListIterate.collect(assertCtx, asserts -> this.visitMappingTestAsserts(asserts));
        return mappingTest;
    }

    private TestAssertion visitMappingTestAsserts(MappingParserGrammar.MappingTestAssertContext ctx)
    {
        TestAssertion testAssertion = HelperTestAssertionGrammarParser.parseTestAssertion(ctx.testAssertion(), this.walkerSourceInformation, this.parserContext.getPureGrammarParserExtensions());
        testAssertion.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        return testAssertion;
    }

    private MappingTest_Legacy visitMappingTest(MappingParserGrammar.TestContext ctx, Mapping mapping)
    {
        MappingTest_Legacy mappingTestLegacy = new MappingTest_Legacy();
        mappingTestLegacy.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        mappingTestLegacy.name = ctx.testName().getText();
        // query
        MappingParserGrammar.TestQueryContext testQueryContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testQuery(), "query", mappingTestLegacy.sourceInformation);
        if (testQueryContext.combinedExpression() != null)
        {
            mappingTestLegacy.query = this.visitMappingTestQuery(testQueryContext.combinedExpression(), mapping);
        }
        // input data
        MappingParserGrammar.TestInputDataContext inputDataContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testInputData(), "data", mappingTestLegacy.sourceInformation);
        mappingTestLegacy.inputData = inputDataContext.testInput() != null && inputDataContext.testInput().testInputElement() != null ? ListIterate.collect(inputDataContext.testInput().testInputElement(), this::visitMappingTestInputData) : Lists.mutable.empty();
        // assert
        MappingParserGrammar.TestAssertContext testAssertContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testAssert(), "assert", mappingTestLegacy.sourceInformation);
        // NOTE: it's important to have `STRING` before `combinedExpression` since the latter matches the former
        if (testAssertContext.STRING() != null)
        {
            ExpectedOutputMappingTestAssert expectedOutputMappingTestAssert = new ExpectedOutputMappingTestAssert();
            expectedOutputMappingTestAssert.expectedOutput = PureGrammarParserUtility.fromGrammarString(testAssertContext.STRING().getText(), false);
            expectedOutputMappingTestAssert.sourceInformation = this.walkerSourceInformation.getSourceInformation(testAssertContext);
            mappingTestLegacy._assert = expectedOutputMappingTestAssert;
        }
        else if (testAssertContext.combinedExpression() != null)
        {
            throw new EngineException("Mapping test currently only support string assertion", this.walkerSourceInformation.getSourceInformation(testAssertContext), EngineErrorType.PARSER);
        }
        return mappingTestLegacy;
    }

    private InputData visitMappingTestInputData(MappingParserGrammar.TestInputElementContext ctx)
    {
        SourceInformation testInputDataSourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        String inputDataType = ctx.testInputType().getText();
        MappingTestInputDataParser mappingTestInputDataParser = this.extensions.getExtraMappingTestInputDataParser(inputDataType);
        if (mappingTestInputDataParser == null)
        {
            throw new EngineException("Unsupported mapping test input data type '" + inputDataType + "'", testInputDataSourceInformation, EngineErrorType.PARSER);
        }
        return mappingTestInputDataParser.parse(ctx, this.walkerSourceInformation);
    }

    private Lambda visitMappingTestQuery(MappingParserGrammar.CombinedExpressionContext ctx, Mapping mapping)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(mapping.getPath(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionWalkerSourceInformation, null);
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
