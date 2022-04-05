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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.DataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.DataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.aggregationAware.AggregationAwareLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.aggregationAware.AggregationAwareParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.xStoreAssociationMapping.XStoreAssociationMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.xStoreAssociationMapping.XStoreAssociationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.ModelConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.data.DataParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ExternalFormatEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ModelStoreEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ReferenceEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.*;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.*;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.EqualToGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.test.assertion.EqualToJsonGrammarParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregateSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.aggregationAware.AggregationAwareClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.xStore.XStoreAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.function.Consumer;

public class CorePureGrammarParser implements PureGrammarParserExtension
{
    public static final String NAME = "Pure";
    public static final String JSON_MODEL_CONNECTION_TYPE = "JsonModelConnection";
    public static final String XML_MODEL_CONNECTION_TYPE = "XmlModelConnection";
    public static final String MODEL_CHAIN_CONNECTION_TYPE = "ModelChainConnection";
    public static final String ENUMERATION_MAPPING_TYPE = "EnumerationMapping";
    public static final String OPERATION_CLASS_MAPPING_TYPE = "Operation";
    public static final String PURE_INSTANCE_CLASS_MAPPING_TYPE = "Pure";
    public static final String OBJECT_TEST_DATA_INPUT_TYPE = "Object";
    public static final String XSTORE_ASSOCIATION_MAPPING_TYPE = "XStore";
    public static final String AGGREGATION_AWARE_MAPPING_TYPE = "AggregationAware";
    public static final String AGGREGATE_SPECIFICATION = "AggregateSpecification";

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Lists.immutable.with(
                MappingElementParser.newParser(ENUMERATION_MAPPING_TYPE, CorePureGrammarParser::parseEnumerationMapping),
                MappingElementParser.newParser(OPERATION_CLASS_MAPPING_TYPE, CorePureGrammarParser::parseOperationClassMapping),
                MappingElementParser.newParser(PURE_INSTANCE_CLASS_MAPPING_TYPE, CorePureGrammarParser::parsePureClassMapping),
                MappingElementParser.newParser(XSTORE_ASSOCIATION_MAPPING_TYPE, CorePureGrammarParser::parseXStoreAssociationMapping),
                MappingElementParser.newParser(AGGREGATION_AWARE_MAPPING_TYPE, CorePureGrammarParser::parseAggregationAwareMapping),
                MappingElementParser.newParser(AGGREGATE_SPECIFICATION, CorePureGrammarParser::parseAggregateSpecification));
    }

    @Override
    public Iterable<? extends MappingTestInputDataParser> getExtraMappingTestInputDataParsers()
    {
        return Lists.immutable.with(MappingTestInputDataParser.newParser(OBJECT_TEST_DATA_INPUT_TYPE, CorePureGrammarParser::parseObjectInputData));
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Lists.immutable.with(
                ConnectionValueParser.newParser(JSON_MODEL_CONNECTION_TYPE, CorePureGrammarParser::parseJsonModelConnection),
                ConnectionValueParser.newParser(XML_MODEL_CONNECTION_TYPE, CorePureGrammarParser::parseXmlModelConnection),
                ConnectionValueParser.newParser(MODEL_CHAIN_CONNECTION_TYPE, CorePureGrammarParser::parseModelChainConnection));
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.immutable.with(SectionParser.newParser("Data", CorePureGrammarParser::parseDataSection));
    }

    @Override
    public Iterable<? extends EmbeddedDataParser> getExtraEmbeddedDataParsers()
    {
        return Lists.immutable.with(
                new ExternalFormatEmbeddedDataParser(),
                new ModelStoreEmbeddedDataParser(),
                new ReferenceEmbeddedDataParser());
    }

    @Override
    public Iterable<? extends TestAssertionParser> getExtraTestAssertionParsers()
    {
        return Lists.immutable.with(
                new EqualToGrammarParser(),
                new EqualToJsonGrammarParser());
    }

    private static Connection parseJsonModelConnection(ConnectionValueSourceCode connectionValueSourceCode)
    {
        JsonModelConnection connectionValue = new JsonModelConnection();
        connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
        SourceCodeParserInfo parserInfo = getModelConnectionParserInfo(connectionValueSourceCode);
        ModelConnectionParseTreeWalker walker = new ModelConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
        walker.visitJsonModelConnectionValue((ModelConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
        return connectionValue;
    }

    private static Connection parseXmlModelConnection(ConnectionValueSourceCode connectionValueSourceCode)
    {
        XmlModelConnection connectionValue = new XmlModelConnection();
        connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
        SourceCodeParserInfo parserInfo = getModelConnectionParserInfo(connectionValueSourceCode);
        ModelConnectionParseTreeWalker walker = new ModelConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
        walker.visitXmlModelConnectionValue((ModelConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
        return connectionValue;
    }

    private static Connection parseModelChainConnection(ConnectionValueSourceCode connectionValueSourceCode)
    {
        ModelChainConnection connectionValue = new ModelChainConnection();
        connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
        SourceCodeParserInfo parserInfo = getModelConnectionParserInfo(connectionValueSourceCode);
        ModelConnectionParseTreeWalker walker = new ModelConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
        walker.visitModelChainConnection((ModelConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
        return connectionValue;
    }

    private static EnumerationMapping parseEnumerationMapping(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
        SourceCodeParserInfo parserInfo = getEnumerationMappingParserInfo(mappingElementSourceCode);
        EnumerationMappingParseTreeWalker walker = new EnumerationMappingParseTreeWalker(parserInfo.walkerSourceInformation);
        EnumerationMapping enumerationMapping = new EnumerationMapping();
        enumerationMapping.enumeration = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        enumerationMapping.id = ctx.mappingElementName() != null ? ctx.mappingElementName().getText() : null;
        enumerationMapping.sourceInformation = parserInfo.sourceInformation;
        walker.visitEnumerationMapping((EnumerationMappingParserGrammar.EnumerationMappingContext) parserInfo.rootContext, enumerationMapping);
        return enumerationMapping;
    }

    private static ClassMapping parseOperationClassMapping(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
        SourceCodeParserInfo parserInfo = getOperationClassMappingParserInfo(mappingElementSourceCode);
        OperationClassMappingParseTreeWalker walker = new OperationClassMappingParseTreeWalker(parserInfo.walkerSourceInformation, parserContext);

        OperationClassMappingParserGrammar.OperationClassMappingContext operationCtx = (OperationClassMappingParserGrammar.OperationClassMappingContext) parserInfo.rootContext;
        if (operationCtx.mergeParameters() != null)
        {
            MergeOperationClassMapping classMapping = new MergeOperationClassMapping();
            classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
            classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
            classMapping.root = ctx.STAR() != null;
            classMapping.sourceInformation = parserInfo.sourceInformation;
            classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
            walker.visitMergeOperationClassMapping((OperationClassMappingParserGrammar.OperationClassMappingContext) parserInfo.rootContext, classMapping);
            return classMapping;

        } else
        {
            OperationClassMapping classMapping = new OperationClassMapping();
            classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
            classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
            classMapping.root = ctx.STAR() != null;
            classMapping.sourceInformation = parserInfo.sourceInformation;
            classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
            walker.visitOperationClassMapping((OperationClassMappingParserGrammar.OperationClassMappingContext) parserInfo.rootContext, classMapping);
            return classMapping;
        }
    }

    private static ClassMapping parsePureClassMapping(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
        SourceCodeParserInfo parserInfo = getPureInstanceClassMappingParserInfo(mappingElementSourceCode);
        PureInstanceClassMappingParseTreeWalker walker = new PureInstanceClassMappingParseTreeWalker(parserInfo.walkerSourceInformation, parserInfo.input, parserContext);
        PureInstanceClassMapping classMapping = new PureInstanceClassMapping();
        classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
        classMapping.root = ctx.STAR() != null;
        classMapping.extendsClassMappingId = ctx.superClassMappingId() != null ? ctx.superClassMappingId().getText() : null;
        classMapping.sourceInformation = parserInfo.sourceInformation;
        classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        walker.visitPureInstanceClassMapping((PureInstanceClassMappingParserGrammar.PureInstanceClassMappingContext) parserInfo.rootContext, ctx, classMapping);
        return classMapping;
    }

    private static AssociationMapping parseXStoreAssociationMapping(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
        SourceCodeParserInfo parserInfo = getXStoreAssociationMappingParserInfo(mappingElementSourceCode);
        XStoreAssociationMappingParseTreeWalker walker = new XStoreAssociationMappingParseTreeWalker(parserInfo.walkerSourceInformation, parserInfo.input, parserContext);
        XStoreAssociationMapping xStoreAssociationMapping = new XStoreAssociationMapping();
        xStoreAssociationMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
        xStoreAssociationMapping.association = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        xStoreAssociationMapping.sourceInformation = parserInfo.sourceInformation;
        walker.visitXStoreAssociationMapping((XStoreAssociationMappingParserGrammar.XStoreAssociationMappingContext) parserInfo.rootContext, xStoreAssociationMapping);
        return xStoreAssociationMapping;
    }

    private static ClassMapping parseAggregationAwareMapping(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
        SourceCodeParserInfo parserInfo = getAggregationAwareMappingParserInfo(mappingElementSourceCode);
        AggregationAwareMappingParseTreeWalker walker = new AggregationAwareMappingParseTreeWalker(parserInfo.walkerSourceInformation, parserInfo.input, parserContext, mappingElementSourceCode);
        AggregationAwareClassMapping aggregationAwareClassMapping = new AggregationAwareClassMapping();
        String className = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        aggregationAwareClassMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : className.replaceAll("::", "_");
        aggregationAwareClassMapping._class = className;
        aggregationAwareClassMapping.root = ctx.STAR() != null;
        aggregationAwareClassMapping.extendsClassMappingId = ctx.superClassMappingId() != null ? ctx.superClassMappingId().getText() : null;
        aggregationAwareClassMapping.sourceInformation = parserInfo.sourceInformation;
        walker.visitAggregationAwareMapping((AggregationAwareParserGrammar.AggregationAwareClassMappingContext) parserInfo.rootContext, aggregationAwareClassMapping);
        return aggregationAwareClassMapping;
    }

    private static AggregateSpecification parseAggregateSpecification(MappingElementSourceCode mappingElementSourceCode, PureGrammarParserContext parserContext)
    {
        SourceCodeParserInfo parserInfo = getAggregateSpecificationParserInfo(mappingElementSourceCode);
        AggregationAwareMappingParseTreeWalker walker = new AggregationAwareMappingParseTreeWalker(parserInfo.walkerSourceInformation, parserInfo.input, parserContext, mappingElementSourceCode);
        AggregateSpecification aggregateSpecification = new AggregateSpecification();
        walker.visitAggregateSpecification((AggregationAwareParserGrammar.AggregateSpecificationContext) parserInfo.rootContext, aggregateSpecification);
        return aggregateSpecification;
    }

    private static InputData parseObjectInputData(MappingParserGrammar.TestInputElementContext inputDataContext, ParseTreeWalkerSourceInformation sourceInformation)
    {
        SourceInformation testInputDataSourceInformation = sourceInformation.getSourceInformation(inputDataContext);
        ObjectInputData objectInputData = new ObjectInputData();
        objectInputData.sourceInformation = testInputDataSourceInformation;
        try
        {
            if (inputDataContext.testInputFormat() == null)
            {
                throw new EngineException("Mapping test object 'input type' is missing. Possible values: " + ArrayIterate.makeString(ObjectInputType.values(), ", "), testInputDataSourceInformation, EngineErrorType.PARSER);
            }
            objectInputData.inputType = ObjectInputType.valueOf(inputDataContext.testInputFormat().getText());
        } catch (IllegalArgumentException e)
        {
            throw new EngineException("Mapping test object input data does not support format '" + inputDataContext.testInputFormat().getText() + "'. Possible values: " + ArrayIterate.makeString(ObjectInputType.values(), ", "), sourceInformation.getSourceInformation(inputDataContext.testInputFormat()), EngineErrorType.PARSER);
        }
        objectInputData.sourceClass = PureGrammarParserUtility.fromQualifiedName(inputDataContext.testInputSrc().qualifiedName().packagePath() == null ? Collections.emptyList() : inputDataContext.testInputSrc().qualifiedName().packagePath().identifier(), inputDataContext.testInputSrc().qualifiedName().identifier());
        objectInputData.data = ListIterate.collect(inputDataContext.testInputDataContent().STRING(), x -> PureGrammarParserUtility.fromGrammarString(x.getText(), false)).makeString("");
        return objectInputData;
    }

    private static SourceCodeParserInfo getModelConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        ModelConnectionLexerGrammar lexer = new ModelConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ModelConnectionParserGrammar parser = new ModelConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getEnumerationMappingParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        EnumerationMappingLexerGrammar lexer = new EnumerationMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        EnumerationMappingParserGrammar parser = new EnumerationMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.enumerationMapping());
    }

    private static SourceCodeParserInfo getOperationClassMappingParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        OperationClassMappingLexerGrammar lexer = new OperationClassMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        OperationClassMappingParserGrammar parser = new OperationClassMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.operationClassMapping());
    }

    private static SourceCodeParserInfo getPureInstanceClassMappingParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        PureInstanceClassMappingLexerGrammar lexer = new PureInstanceClassMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        PureInstanceClassMappingParserGrammar parser = new PureInstanceClassMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.pureInstanceClassMapping());
    }

    private static SourceCodeParserInfo getXStoreAssociationMappingParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        XStoreAssociationMappingLexerGrammar lexer = new XStoreAssociationMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        XStoreAssociationMappingParserGrammar parser = new XStoreAssociationMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.xStoreAssociationMapping());
    }

    private static SourceCodeParserInfo getAggregationAwareMappingParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        AggregationAwareLexerGrammar lexer = new AggregationAwareLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AggregationAwareParserGrammar parser = new AggregationAwareParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        SourceInformation source = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, source, mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.aggregationAwareClassMapping());
    }

    private static SourceCodeParserInfo getAggregateSpecificationParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        AggregationAwareLexerGrammar lexer = new AggregationAwareLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AggregationAwareParserGrammar parser = new AggregationAwareParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        SourceInformation source = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext);
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, source, mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, parser.aggregateSpecification());
    }

    private static Section parseDataSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext pureGrammarParserContext)
    {
        SourceCodeParserInfo parserInfo = getDataParserInfo(sectionSourceCode);
        DefaultCodeSection section = new DefaultCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;
        DataParseTreeWalker walker = new DataParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section, pureGrammarParserContext.getPureGrammarParserExtensions());
        walker.visit((DataParserGrammar.DefinitionContext) parserInfo.rootContext);
        return section;
    }

    private static SourceCodeParserInfo getDataParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        DataLexerGrammar lexer = new DataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        DataParserGrammar parser = new DataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
