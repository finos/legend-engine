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

package org.finos.legend.engine.language.pure.dsl.service.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedSingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTag;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

public class ServiceParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserContext context;

    public ServiceParseTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, PureGrammarParserContext context)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
    }

    public void visit(ServiceParserGrammar.DefinitionContext ctx)
    {
        this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
        ctx.service().stream().map(this::visitService).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    public Service visitService(ServiceParserGrammar.ServiceContext ctx)
    {
        Service service = new Service();
        service.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        service._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        service.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // pattern
        ServiceParserGrammar.ServicePatternContext patternContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.servicePattern(), "pattern", service.sourceInformation);
        service.pattern = PureGrammarParserUtility.fromGrammarString(patternContext.STRING().getText(), true);
        // documentation
        ServiceParserGrammar.ServiceDocumentationContext documentationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceDocumentation(), "documentation", service.sourceInformation);
        service.documentation = PureGrammarParserUtility.fromGrammarString(documentationContext.STRING().getText(), true);
        // auto activate update flag (optional)
        ServiceParserGrammar.ServiceAutoActivateUpdatesContext autoActivateUpdatesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceAutoActivateUpdates(), "autoActivateUpdates", service.sourceInformation);
        service.autoActivateUpdates = autoActivateUpdatesContext != null && Boolean.parseBoolean(autoActivateUpdatesContext.BOOLEAN().getText());
        // owners (optional)
        ServiceParserGrammar.ServiceOwnersContext ownersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceOwners(), "owners", service.sourceInformation);
        service.owners = ownersContext != null && ownersContext.STRING() != null ? ListIterate.collect(ownersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)) : new ArrayList<>();
        // tags (optional)
        ServiceParserGrammar.ServiceTagsContext serviceTagsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTags(), "tags", service.sourceInformation);
        service.tags = serviceTagsContext != null && serviceTagsContext.SERVICE_TAGS() != null ? ListIterate.collect(serviceTagsContext.tagDescription(), this::visitServiceTagDescription) : new ArrayList<>();
        // execution
        ServiceParserGrammar.ServiceExecContext execContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceExec(), "execution", service.sourceInformation);
        service.execution = this.visitExecution(execContext);
        // test
        ServiceParserGrammar.ServiceTestContext testContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceTest(), "test", service.sourceInformation);
        service.test = this.visitTest(testContext);
        return service;
    }

    private ServiceTag visitServiceTagDescription(ServiceParserGrammar.TagDescriptionContext ctx)
    {
        ServiceTag serviceTag = new ServiceTag();
        serviceTag.name = ctx.serviceTagName().STRING().getText();
        serviceTag.value = ctx.serviceTagValue().STRING().getText();
        return serviceTag;
    }

    private Execution visitExecution(ServiceParserGrammar.ServiceExecContext ctx)
    {
        if (ctx.singleExec() != null)
        {
            ServiceParserGrammar.SingleExecContext pureSingleExecContext = ctx.singleExec();
            PureSingleExecution pureSingleExecution = new PureSingleExecution();
            pureSingleExecution.sourceInformation = walkerSourceInformation.getSourceInformation(pureSingleExecContext);
            // function/query
            ServiceParserGrammar.ServiceFuncContext funcContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureSingleExecContext.serviceFunc(), "query", pureSingleExecution.sourceInformation);
            pureSingleExecution.func = visitLambda(funcContext.combinedExpression());
            // mapping
            ServiceParserGrammar.ServiceMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureSingleExecContext.serviceMapping(), "mapping", pureSingleExecution.sourceInformation);
            pureSingleExecution.mapping = PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier());
            pureSingleExecution.mappingSourceInformation = walkerSourceInformation.getSourceInformation(mappingContext.qualifiedName());
            // runtime
            ServiceParserGrammar.ServiceRuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureSingleExecContext.serviceRuntime(), "runtime", pureSingleExecution.sourceInformation);
            pureSingleExecution.runtime = this.visitRuntime(runtimeContext);
            return pureSingleExecution;
        }
        else if (ctx.multiExec() != null)
        {
            ServiceParserGrammar.MultiExecContext pureMultiExecContext = ctx.multiExec();
            PureMultiExecution pureMultiExecution = new PureMultiExecution();
            pureMultiExecution.sourceInformation = walkerSourceInformation.getSourceInformation(pureMultiExecContext);
            // function/query
            ServiceParserGrammar.ServiceFuncContext funcContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureMultiExecContext.serviceFunc(), "query", pureMultiExecution.sourceInformation);
            pureMultiExecution.func = visitLambda(funcContext.combinedExpression());
            // execution key
            ServiceParserGrammar.ExecKeyContext execKeyContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureMultiExecContext.execKey(), "key", pureMultiExecution.sourceInformation);
            pureMultiExecution.executionKey = PureGrammarParserUtility.fromGrammarString(execKeyContext.STRING().getText(), true);
            // execution parameters (indexed by execution key)
            pureMultiExecution.executionParameters = ListIterate.collect(pureMultiExecContext.execParameter(), this::visitKeyedExecutionParameter);
            return pureMultiExecution;
        }
        throw new UnsupportedOperationException();
    }

    private Runtime visitRuntime(ServiceParserGrammar.ServiceRuntimeContext serviceRuntimeContext)
    {
        if (serviceRuntimeContext.runtimePointer() != null)
        {
            RuntimePointer runtimePointer = new RuntimePointer();
            if (serviceRuntimeContext.runtimePointer().qualifiedName() != null)
            {
                runtimePointer.runtime = PureGrammarParserUtility.fromQualifiedName(serviceRuntimeContext.runtimePointer().qualifiedName().packagePath() == null ? Collections.emptyList() : serviceRuntimeContext.runtimePointer().qualifiedName().packagePath().identifier(), serviceRuntimeContext.runtimePointer().qualifiedName().identifier());
                runtimePointer.sourceInformation = walkerSourceInformation.getSourceInformation(serviceRuntimeContext.runtimePointer().qualifiedName());
            }
            return runtimePointer;
        }
        else if (serviceRuntimeContext.embeddedRuntime() != null)
        {
            StringBuilder embeddedRuntimeText = new StringBuilder();
            for (ServiceParserGrammar.EmbeddedRuntimeContentContext fragment : serviceRuntimeContext.embeddedRuntime().embeddedRuntimeContent())
            {
                embeddedRuntimeText.append(fragment.getText());
            }
            String embeddedRuntimeParsingText = embeddedRuntimeText.length() > 0 ? embeddedRuntimeText.substring(0, embeddedRuntimeText.length() - 2) : embeddedRuntimeText.toString();
            RuntimeParser runtimeParser = RuntimeParser.newInstance(this.context.getPureGrammarParserExtensions());
            // prepare island grammar walker source information
            int startLine = serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getSymbol().getCharPositionInLine() + serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getText().length();
            ParseTreeWalkerSourceInformation embeddedRuntimeWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation embeddedRuntimeSourceInformation = walkerSourceInformation.getSourceInformation(serviceRuntimeContext.embeddedRuntime());
            return runtimeParser.parseEmbeddedRuntime(embeddedRuntimeParsingText, embeddedRuntimeWalkerSourceInformation, embeddedRuntimeSourceInformation);
        }
        throw new UnsupportedOperationException();
    }

    private KeyedExecutionParameter visitKeyedExecutionParameter(ServiceParserGrammar.ExecParameterContext ctx)
    {
        KeyedExecutionParameter keyedExecutionParameter = new KeyedExecutionParameter();
        keyedExecutionParameter.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // execution key value
        keyedExecutionParameter.key = PureGrammarParserUtility.fromGrammarString(ctx.execParameterSignature().STRING().getText(), true);
        // mapping
        ServiceParserGrammar.ServiceMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceMapping(), "mapping", keyedExecutionParameter.sourceInformation);
        keyedExecutionParameter.mapping = PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier());
        keyedExecutionParameter.mappingSourceInformation = walkerSourceInformation.getSourceInformation(mappingContext.qualifiedName());
        // runtime
        ServiceParserGrammar.ServiceRuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceRuntime(), "runtime", keyedExecutionParameter.sourceInformation);
        keyedExecutionParameter.runtime = this.visitRuntime(runtimeContext);
        return keyedExecutionParameter;
    }

    private ServiceTest visitTest(ServiceParserGrammar.ServiceTestContext ctx)
    {
        if (ctx.singleTest() != null)
        {
            ServiceParserGrammar.SingleTestContext singleTestContext = ctx.singleTest();
            SingleExecutionTest singleExecutionTest = new SingleExecutionTest();
            singleExecutionTest.sourceInformation = walkerSourceInformation.getSourceInformation(singleTestContext);
            // test data
            ServiceParserGrammar.TestDataContext testDataContext = PureGrammarParserUtility.validateAndExtractRequiredField(singleTestContext.testData(), "data", singleExecutionTest.sourceInformation);
            singleExecutionTest.data = PureGrammarParserUtility.fromGrammarString(testDataContext.STRING().getText(), true);
            // test asserts (optional)
            ServiceParserGrammar.TestAssertsContext assertsContext = PureGrammarParserUtility.validateAndExtractOptionalField(singleTestContext.testAsserts(), "asserts", singleExecutionTest.sourceInformation);
            singleExecutionTest.asserts = assertsContext != null ? ListIterate.collect(assertsContext.testAssert(), this::visitTestContainer) : new ArrayList<>();
            return singleExecutionTest;
        }
        else if (ctx.multiTest() != null)
        {
            ServiceParserGrammar.MultiTestContext multiTestContext = ctx.multiTest();
            MultiExecutionTest multiExecutionTest = new MultiExecutionTest();
            multiExecutionTest.sourceInformation = walkerSourceInformation.getSourceInformation(multiTestContext);
            // tests (indexed by execution key)
            multiExecutionTest.tests = ListIterate.collect(multiTestContext.multiTestElement(), this::visitKeyedSingleExecutionTest);
            return multiExecutionTest;
        }
        throw new UnsupportedOperationException();
    }

    private TestContainer visitTestContainer(ServiceParserGrammar.TestAssertContext ctx)
    {
        TestContainer testContainer = new TestContainer();
        testContainer.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // TODO parameters values support
        testContainer.parametersValues = new ArrayList<>();
        testContainer._assert = this.visitLambda(ctx.combinedExpression());
        return testContainer;
    }

    private KeyedSingleExecutionTest visitKeyedSingleExecutionTest(ServiceParserGrammar.MultiTestElementContext ctx)
    {
        KeyedSingleExecutionTest keyedSingleExecutionTest = new KeyedSingleExecutionTest();
        keyedSingleExecutionTest.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // execution key value
        keyedSingleExecutionTest.key = PureGrammarParserUtility.fromGrammarString(ctx.multiTestElementSignature().STRING().getText(), true);
        // test data
        ServiceParserGrammar.TestDataContext testDataContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.testData(), "data", keyedSingleExecutionTest.sourceInformation);
        keyedSingleExecutionTest.data = PureGrammarParserUtility.fromGrammarString(testDataContext.STRING().getText(), true);
        // test asserts (optional)
        ServiceParserGrammar.TestAssertsContext assertsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.testAsserts(), "asserts", keyedSingleExecutionTest.sourceInformation);
        keyedSingleExecutionTest.asserts = assertsContext != null ? ListIterate.collect(assertsContext.testAssert(), this::visitTestContainer) : new ArrayList<>();
        return keyedSingleExecutionTest;
    }

    private Lambda visitLambda(ServiceParserGrammar.CombinedExpressionContext ctx)
    {
        DomainParser parser = new DomainParser();
        // prepare island grammar walker source information
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation combineExpressionSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        String lambdaString = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parseCombinedExpression(lambdaString, combineExpressionSourceInformation, null);
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
