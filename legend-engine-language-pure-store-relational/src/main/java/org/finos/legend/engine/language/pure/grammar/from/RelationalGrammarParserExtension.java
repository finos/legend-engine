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
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.RelationalParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthenticationStrategyParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.datasource.DataSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategyParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.authentication.AuthenticationStrategySourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.datasource.DataSourceSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingTestInputDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.milestoning.MilestoningSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.postProcessors.PostProcessorSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.mappingTest.InputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RelationalAssociationMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.RootRelationalClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.mappingTest.RelationalInputType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.milestoning.Milestoning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.operation.RelationalOperationElement;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class RelationalGrammarParserExtension implements IRelationalGrammarParserExtension
{
    public static final String NAME = "Relational";
    public static final String RELATIONAL_MAPPING_ELEMENT_TYPE = "Relational";
    public static final String RELATIONAL_DATABASE_CONNECTION_TYPE = "RelationalDatabaseConnection";

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Collections.singletonList(SectionParser.newParser(NAME, (sectionSourceCode, elementConsumer, context) ->
        {
            SourceCodeParserInfo parserInfo = getRelationalParserInfo(sectionSourceCode);
            DefaultCodeSection section = new DefaultCodeSection();
            section.parserName = sectionSourceCode.sectionType;
            section.sourceInformation = parserInfo.sourceInformation;
            RelationalParseTreeWalker walker = new RelationalParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
            walker.visit((RelationalParserGrammar.DefinitionContext) parserInfo.rootContext);
            return section;
        }));
    }

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Collections.singletonList(MappingElementParser.newParser(RELATIONAL_MAPPING_ELEMENT_TYPE,
            (mappingElementSourceCode, parserContext) ->
            {
                MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
                SourceCodeParserInfo parserInfo = getRelationalMappingElementParserInfo(mappingElementSourceCode);
                RelationalParseTreeWalker walker = new RelationalParseTreeWalker(parserInfo.walkerSourceInformation);
                if (parserInfo.rootContext instanceof RelationalParserGrammar.ClassMappingContext)
                {
                    RootRelationalClassMapping classMapping = new RootRelationalClassMapping();
                    classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
                    classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
                    classMapping.root = ctx.STAR() != null;
                    classMapping.extendsClassMappingId = ctx.superClassMappingId() != null ? ctx.superClassMappingId().getText() : null;
                    classMapping.sourceInformation = parserInfo.sourceInformation;
                    classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                    walker.visitRootRelationalClassMapping((RelationalParserGrammar.ClassMappingContext) parserInfo.rootContext, classMapping, classMapping._class);
                    return classMapping;
                }
                if (parserInfo.rootContext instanceof RelationalParserGrammar.AssociationMappingContext)
                {
                    RelationalAssociationMapping associationMapping = new RelationalAssociationMapping();
                    associationMapping.association = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
                    associationMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
                    // TODO? stores
                    associationMapping.sourceInformation = parserInfo.sourceInformation;
                    walker.visitRelationalAssociationMapping((RelationalParserGrammar.AssociationMappingContext) parserInfo.rootContext, associationMapping);
                    return associationMapping;
                }
                throw new EngineException("Unknown relational mapping element type: " + parserInfo.rootContext.getClass().getName(), parserInfo.sourceInformation, EngineErrorType.PARSER);
            })
        );
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(RELATIONAL_DATABASE_CONNECTION_TYPE, connectionValueSourceCode ->
        {
            SourceCodeParserInfo parserInfo = getRelationalDatabaseConnectionParserInfo(connectionValueSourceCode);
            RelationalDatabaseConnectionParseTreeWalker walker = new RelationalDatabaseConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
            RelationalDatabaseConnection connectionValue = new RelationalDatabaseConnection();
            connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitRelationalDatabaseConnectionValue((RelationalDatabaseConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
            return connectionValue;
        }));
    }

    @Override
    public List<java.util.function.Function<DataSourceSpecificationSourceCode, DatasourceSpecification>> getExtraDataSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {

            DataSourceSpecificationParseTreeWalker walker = new DataSourceSpecificationParseTreeWalker();

            switch (code.getType())
            {
                case "LocalH2":
                    return parseDataSourceSpecification(code, p -> walker.visitLocalH2DatasourceSpecification(code, p.localH2DatasourceSpecification()));
                case "Static":
                    return parseDataSourceSpecification(code, p -> walker.visitStaticDatasourceSpecification(code, p.staticDatasourceSpecification()));
                case "EmbeddedH2":
                    return parseDataSourceSpecification(code, p -> walker.visitEmbeddedH2DatasourceSpecification(code, p.embeddedH2DatasourceSpecification()));
                case "Databricks":
                    return parseDataSourceSpecification(code, p -> walker.visitDatabricksDatasourceSpecification(code, p.databricksDatasourceSpecification()));
                case "Snowflake":
                    return parseDataSourceSpecification(code, p-> walker.visitSnowflakeDatasourceSpecification(code, p.snowflakeDatasourceSpecification()));
                case "BigQuery":
                    return parseDataSourceSpecification(code, p -> walker.visitBigQueryDatasourceSpecification(code, p.bigQueryDatasourceSpecification()));
                case "Redshift":
                    return parseDataSourceSpecification(code, p -> walker.visitRedshiftDatasourceSpecification(code, p.redshiftDatasourceSpecification()));

                default:
                    return null;
            }
        });
    }

    @Override
    public List<Function<AuthenticationStrategySourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code ->
        {
            AuthenticationStrategyParseTreeWalker walker = new AuthenticationStrategyParseTreeWalker();

            switch (code.getType())
            {
                case "DefaultH2":
                    return parseAuthenticationStrategy(code, p -> walker.visitDefaultH2AuthenticationStrategy(code, p.defaultH2Auth()));
                case "DelegatedKerberos":
                    return parseAuthenticationStrategy(code, p -> walker.visitDelegatedKerberosAuthenticationStrategy(code, p.delegatedKerberosAuth()));
                case "UserNamePassword":
                    return parseAuthenticationStrategy(code, p -> walker.visitUserNamePasswordAuthenticationStrategy(code, p.userNamePasswordAuth()));
                case "Test":
                    return parseAuthenticationStrategy(code, p -> walker.visitTestDatabaseAuthenticationStrategy(code, p.testDBAuth()));
                case "ApiToken":
                    return parseAuthenticationStrategy(code, p -> walker.visitApiTokenAuthenticationStrategy(code, p.apiTokenAuth()));
                case "SnowflakePublic":
                    return parseAuthenticationStrategy(code, p -> walker.visitSnowflakePublicAuthenticationStrategy(code, p.snowflakePublicAuth()));
                case "GCPApplicationDefaultCredentials":
                    return parseAuthenticationStrategy(code, p -> walker.visitGCPApplicationDefaultCredentialsAuthenticationStrategy(code, p.gcpApplicationDefaultCredentialsAuth() ));
                default:
                    return null;
            }
        });
    }

    @Override
    public List<Function<PostProcessorSpecificationSourceCode, PostProcessor>> getExtraPostProcessorParsers()
    {
        PostProcessorParseTreeWalker walker = new PostProcessorParseTreeWalker();
        return Collections.singletonList(code -> "mapper".equals(code.getType()) ? PostProcessorParseTreeWalker.parsePostProcessor(code, p -> walker.visitMapperPostProcessor(code, p.mapperPostProcessor())) : null);
    }

    @Override
    public List<Function<MilestoningSpecificationSourceCode, Milestoning>> getExtraMilestoningParsers()
    {
        return Collections.singletonList(code ->
        {
            MilestoningParseTreeWalker walker = new MilestoningParseTreeWalker();

            switch (code.getType())
            {
                case "business":
                    return parseMilestoning(code, p -> walker.visitBusinessMilestoning(code, p.businessMilestoning()));
                case "processing":
                    return parseMilestoning(code, p -> walker.visitProcessingMilestoning(code, p.processingMilestoning()));
                default:
                    return null;
            }
        });
    }

    @Override
    public Iterable<? extends MappingTestInputDataParser> getExtraMappingTestInputDataParsers()
    {
        return Lists.immutable.with(MappingTestInputDataParser.newParser("Relational", RelationalGrammarParserExtension::parseObjectInputData));
    }

    private static InputData parseObjectInputData(MappingParserGrammar.TestInputElementContext inputDataContext, ParseTreeWalkerSourceInformation sourceInformation)
    {
        SourceInformation testInputDataSourceInformation = sourceInformation.getSourceInformation(inputDataContext);
        RelationalInputData relationalInputData = new RelationalInputData();
        relationalInputData.sourceInformation = testInputDataSourceInformation;

        try
        {
            if (inputDataContext.testInputFormat() == null)
            {
                throw new EngineException("Mapping test relational 'input type' is missing. Possible values: " + ArrayIterate.makeString(RelationalInputType.values(), ", "), testInputDataSourceInformation, EngineErrorType.PARSER);
            }
            relationalInputData.inputType = RelationalInputType.valueOf(inputDataContext.testInputFormat().getText());
        }
        catch (IllegalArgumentException e)
        {
            throw new EngineException("Mapping test relational input data does not support format '" + inputDataContext.testInputFormat().getText() + "'. Possible values: " + ArrayIterate.makeString(RelationalInputType.values(), ", "), sourceInformation.getSourceInformation(inputDataContext.testInputFormat()), EngineErrorType.PARSER);
        }

        relationalInputData.database = inputDataContext.testInputSrc().getText();

        relationalInputData.data = ListIterate.collect(inputDataContext.testInputDataContent().STRING(), x->PureGrammarParserUtility.fromGrammarString(x.getText().replace("\\;","\\\\;"), true)).makeString("");
        return relationalInputData;
    }


    private AuthenticationStrategy parseAuthenticationStrategy(AuthenticationStrategySourceCode code, Function<AuthenticationStrategyParserGrammar, AuthenticationStrategy> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        AuthenticationStrategyLexerGrammar lexer = new AuthenticationStrategyLexerGrammar(input);
        AuthenticationStrategyParserGrammar parser = new AuthenticationStrategyParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private DatasourceSpecification parseDataSourceSpecification(DataSourceSpecificationSourceCode code, Function<DataSourceSpecificationParserGrammar, DatasourceSpecification> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        DataSourceSpecificationLexerGrammar lexer = new DataSourceSpecificationLexerGrammar(input);
        DataSourceSpecificationParserGrammar parser = new DataSourceSpecificationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private Milestoning parseMilestoning(MilestoningSpecificationSourceCode code, Function<RelationalParserGrammar, Milestoning> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        RelationalLexerGrammar lexer = new RelationalLexerGrammar(input);
        RelationalParserGrammar parser = new RelationalParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private static SourceCodeParserInfo getRelationalParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        RelationalLexerGrammar lexer = new RelationalLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        RelationalParserGrammar parser = new RelationalParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getRelationalMappingElementParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        RelationalLexerGrammar lexer = new RelationalLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        RelationalParserGrammar parser = new RelationalParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        RelationalParserGrammar.MappingContext mappingContext = parser.mapping();
        ParserRuleContext associationMappingCtx = mappingContext.associationMapping();
        ParserRuleContext classMappingCtx = mappingContext.classMapping();
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, associationMappingCtx != null ? associationMappingCtx : classMappingCtx);
    }

    private static SourceCodeParserInfo getRelationalDatabaseConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        RelationalDatabaseConnectionLexerGrammar lexer = new RelationalDatabaseConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        RelationalDatabaseConnectionParserGrammar parser = new RelationalDatabaseConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    public static RelationalOperationElement parseRelationalOperationElement(String code, boolean returnSourceInfo)
    {
        CharStream input = CharStreams.fromString(code);
        ParseTreeWalkerSourceInformation parseTreeWalkerSourceInformation= new ParseTreeWalkerSourceInformation.Builder("", 0, 0).withReturnSourceInfo(returnSourceInfo).build();
        ParserErrorListener errorListener = new ParserErrorListener(parseTreeWalkerSourceInformation);
        RelationalLexerGrammar lexer = new RelationalLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        RelationalParserGrammar parser = new RelationalParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        RelationalParseTreeWalker walker = new RelationalParseTreeWalker(parseTreeWalkerSourceInformation);
        return walker.visitOperation(parser.operation(), null);
    }
}
