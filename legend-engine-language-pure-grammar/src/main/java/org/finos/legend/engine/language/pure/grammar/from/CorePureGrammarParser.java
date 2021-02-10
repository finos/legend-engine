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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.enumerationMapping.EnumerationMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.operationClassMapping.OperationClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.pureInstanceClassMapping.PureInstanceClassMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.ModelConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingTestInputDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.mapping.EnumerationMappingParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.mapping.OperationClassMappingParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.mapping.PureInstanceClassMappingParseTreeWalker;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.EnumerationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.OperationClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.ObjectInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.mapping.PureInstanceClassMapping;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;

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

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Lists.immutable.with(
                MappingElementParser.newParser(ENUMERATION_MAPPING_TYPE, CorePureGrammarParser::parseEnumerationMapping),
                MappingElementParser.newParser(OPERATION_CLASS_MAPPING_TYPE, CorePureGrammarParser::parseOperationClassMapping),
                MappingElementParser.newParser(PURE_INSTANCE_CLASS_MAPPING_TYPE, CorePureGrammarParser::parsePureClassMapping));
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
        OperationClassMappingParseTreeWalker walker = new OperationClassMappingParseTreeWalker();
        OperationClassMapping classMapping = new OperationClassMapping();
        classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
        classMapping.root = ctx.STAR() != null;
        classMapping.sourceInformation = parserInfo.sourceInformation;
        classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        walker.visitOperationClassMapping((OperationClassMappingParserGrammar.OperationClassMappingContext) parserInfo.rootContext, classMapping);
        return classMapping;
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

    private static InputData parseObjectInputData(MappingParserGrammar.TestInputElementContext inputDataContext, ParseTreeWalkerSourceInformation sourceInformation)
    {
        SourceInformation testInputDataSourceInformation = sourceInformation.getSourceInformation(inputDataContext);
        ObjectInputData objectInputData = new ObjectInputData();
        objectInputData.sourceInformation = testInputDataSourceInformation;
        objectInputData.sourceClass = PureGrammarParserUtility.fromQualifiedName(inputDataContext.testInputSrc().qualifiedName().packagePath() == null ? Collections.emptyList() : inputDataContext.testInputSrc().qualifiedName().packagePath().identifier(), inputDataContext.testInputSrc().qualifiedName().identifier());
        objectInputData.data = PureGrammarParserUtility.fromGrammarString(inputDataContext.testInputDataContent().STRING().getText(), false);
        if (inputDataContext.testInputFormat() == null)
        {
            throw new EngineException("Mapping test object input data format type is missing", testInputDataSourceInformation, EngineErrorType.PARSER);
        }
        try
        {
            objectInputData.inputType = ObjectInputType.valueOf(inputDataContext.testInputFormat().getText());
        }
        catch (IllegalArgumentException e)
        {
            throw new EngineException("Mapping test object input data does not support format '" + inputDataContext.testInputFormat().getText() + "'", sourceInformation.getSourceInformation(inputDataContext.testInputFormat()), EngineErrorType.PARSER);
        }
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
}
