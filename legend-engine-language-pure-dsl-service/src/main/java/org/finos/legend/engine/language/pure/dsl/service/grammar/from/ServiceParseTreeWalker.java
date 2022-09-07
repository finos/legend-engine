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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.HelperTestAssertionGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedExecutionParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.KeyedSingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ParameterValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureInlineExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PureList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        service.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        service.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

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
        // execution
        ServiceParserGrammar.ServiceExecContext execContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceExec(), "execution", service.sourceInformation);
        service.execution = this.visitExecution(execContext);
        // test suites
        // TODO: this should be marked required when every service is migrated
        ServiceParserGrammar.ServiceTestSuitesContext testSuitesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTestSuites(), "testSuites", service.sourceInformation);
        if (testSuitesContext != null)
        {
            service.testSuites = ListIterate.collect(testSuitesContext.serviceTestSuite(), this::visitServiceTestSuite);
        }
        // test
        ServiceParserGrammar.ServiceTestContext testContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTest(), "test", service.sourceInformation);
        if (testContext != null)
        {
            service.test = this.visitTest(testContext);
        }
        return service;
    }

    private ServiceTestSuite visitServiceTestSuite(ServiceParserGrammar.ServiceTestSuiteContext ctx)
    {
        ServiceTestSuite serviceTestSuite = new ServiceTestSuite();
        serviceTestSuite.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        serviceTestSuite.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        // data
        ServiceParserGrammar.ServiceTestSuiteDataContext testSuiteDataContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTestSuiteData(), "data", serviceTestSuite.sourceInformation);
        if (testSuiteDataContext != null)
        {
            serviceTestSuite.testData = visitServiceTestData(testSuiteDataContext);
        }

        // tests
        ServiceParserGrammar.ServiceTestSuiteTestsContext testSuiteTestsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceTestSuiteTests(), "tests", serviceTestSuite.sourceInformation);
        serviceTestSuite.tests = ListIterate.collect(testSuiteTestsContext.serviceTestBlock(), this::visitServiceTest);

        return serviceTestSuite;
    }

    private TestData visitServiceTestData(ServiceParserGrammar.ServiceTestSuiteDataContext ctx)
    {
        TestData testData = new TestData();

        testData.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        ServiceParserGrammar.ServiceTestConnectionsDataContext testConnectionsDataContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTestConnectionsData(), "connections", testData.sourceInformation);
        if (testConnectionsDataContext != null)
        {
            testData.connectionsTestData = ListIterate.collect(testConnectionsDataContext.serviceTestConnectionData(), this::visitServiceTestConnectionData);
        }

        return testData;
    }

    private ConnectionTestData visitServiceTestConnectionData(ServiceParserGrammar.ServiceTestConnectionDataContext ctx)
    {
        ConnectionTestData connectionData = new ConnectionTestData();

        connectionData.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);
        connectionData.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        connectionData.data = HelperEmbeddedDataGrammarParser.parseEmbeddedData(ctx.embeddedData(), this.walkerSourceInformation, this.context.getPureGrammarParserExtensions());

        return connectionData;
    }

    private ServiceTest visitServiceTest(ServiceParserGrammar.ServiceTestBlockContext ctx)
    {
        ServiceTest serviceTest = new ServiceTest();

        serviceTest.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        serviceTest.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        // serializationFormat
        ServiceParserGrammar.ServiceTestSerializationContext serializationFormatContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTestSerialization(), "serializationFormat", serviceTest.sourceInformation);
        if (serializationFormatContext != null)
        {
            serviceTest.serializationFormat = PureGrammarParserUtility.fromIdentifier(serializationFormatContext.identifier());
        }

        // parameters
        ServiceParserGrammar.ServiceTestParametersContext testParametersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceTestParameters(), "parameters", serviceTest.sourceInformation);
        if (testParametersContext != null)
        {
            serviceTest.parameters = ListIterate.collect(testParametersContext.serviceTestParameter(), this::visitServiceTestParameter);
        }

        // asserts
        ServiceParserGrammar.ServiceTestAssertsContext testAssertsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceTestAsserts(), "asserts", serviceTest.sourceInformation);
        serviceTest.assertions = ListIterate.collect(testAssertsContext.serviceTestAssert(), this::visitServiceTestAsserts);

        return serviceTest;
    }

    private ParameterValue visitServiceTestParameter(ServiceParserGrammar.ServiceTestParameterContext ctx)
    {
        ParameterValue parameterValue = new ParameterValue();

        parameterValue.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        parameterValue.value = this.visitTestParameter(ctx.primitiveValue());

        return parameterValue;
    }

    private TestAssertion visitServiceTestAsserts(ServiceParserGrammar.ServiceTestAssertContext ctx)
    {
        TestAssertion testAssertion = HelperTestAssertionGrammarParser.parseTestAssertion(ctx.testAssertion(), this.walkerSourceInformation, this.context.getPureGrammarParserExtensions());
        testAssertion.id = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        return testAssertion;
    }

    private List<TaggedValue> visitTaggedValues(ServiceParserGrammar.TaggedValuesContext ctx)
    {
        return ListIterate.collect(ctx.taggedValue(), taggedValueContext ->
        {
            TaggedValue taggedValue = new TaggedValue();
            TagPtr tagPtr = new TagPtr();
            taggedValue.tag = tagPtr;
            tagPtr.profile = PureGrammarParserUtility.fromQualifiedName(taggedValueContext.qualifiedName().packagePath() == null ? Collections.emptyList() : taggedValueContext.qualifiedName().packagePath().identifier(), taggedValueContext.qualifiedName().identifier());
            tagPtr.value = PureGrammarParserUtility.fromIdentifier(taggedValueContext.identifier());
            taggedValue.value = PureGrammarParserUtility.fromGrammarString(taggedValueContext.STRING().getText(), true);
            taggedValue.tag.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.qualifiedName());
            taggedValue.tag.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.identifier());
            taggedValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext);
            return taggedValue;
        });
    }

    private List<StereotypePtr> visitStereotypes(ServiceParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext);
            return stereotypePtr;
        });
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
        else if (ctx.inlineExec() != null)
        {
            ServiceParserGrammar.InlineExecContext pureInlineExecCtx = ctx.inlineExec();
            PureInlineExecution pureInlineExecution = new PureInlineExecution();
            pureInlineExecution.sourceInformation = walkerSourceInformation.getSourceInformation(pureInlineExecCtx);
            // function/query
            ServiceParserGrammar.ServiceFuncContext funcContext = PureGrammarParserUtility.validateAndExtractRequiredField(pureInlineExecCtx.serviceFunc(), "query", pureInlineExecution.sourceInformation);
            pureInlineExecution.func = visitLambda(funcContext.combinedExpression());

            return pureInlineExecution;
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

    private ServiceTest_Legacy visitTest(ServiceParserGrammar.ServiceTestContext ctx)
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
        testContainer.parametersValues = this.visitTestParameters(ctx.testParameters());
        testContainer._assert = this.visitLambda(ctx.combinedExpression());
        return testContainer;
    }

    private List<ValueSpecification> visitTestParameters(ServiceParserGrammar.TestParametersContext ctx)
    {
        List<ValueSpecification> testParameters = ctx != null && ctx.testParam() != null ? ListIterate.collect(ctx.testParam(), this::visitParam) : null;
        return testParameters;
    }

    private ValueSpecification visitParam(ServiceParserGrammar.TestParamContext ctx)
    {
        if (ctx != null)
        {
            if (ctx.testListValueParam() != null)
            {
                List<ValueSpecification> paramValues = ListIterate.collect(ctx.testListValueParam().primitiveValue(), this::visitTestParameter);
                PureList param = new PureList();
                param.values = paramValues;
                param.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.testListValueParam());
                return param;
            }
            else if (ctx.testSingleValueParam() != null)
            {
                return visitTestParameter(ctx.testSingleValueParam().primitiveValue());
            }
        }
        throw new UnsupportedOperationException();
    }

    private ValueSpecification visitTestParameter(ServiceParserGrammar.PrimitiveValueContext ctx)
    {
        DomainParser parser = new DomainParser();
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation serviceParamSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).build();
        String parameter = this.input.getText(new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex()));
        ValueSpecification valueSpecification = parser.parsePrimitiveValue(parameter, serviceParamSourceInformation, null);
        return valueSpecification;
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
