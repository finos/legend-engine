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

package org.finos.legend.engine.language.pure.grammar.from.postProcessors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.postProcessor.PostProcessorLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.postProcessor.PostProcessorParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.Mapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.MapperPostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.PostProcessor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.SchemaNameMapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.postprocessor.TableNameMapper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.List;

public class PostProcessorParseTreeWalker
{
    public static <T extends PostProcessor> T parsePostProcessor(PostProcessorSpecificationSourceCode code, Function<PostProcessorParserGrammar, T> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        PostProcessorLexerGrammar lexer = new PostProcessorLexerGrammar(input);
        PostProcessorParserGrammar parser = new PostProcessorParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    public MapperPostProcessor visitMapperPostProcessor(PostProcessorSpecificationSourceCode code, PostProcessorParserGrammar.MapperPostProcessorContext ctx)
    {
        List<Mapper> mappers = ListIterate.collect(ctx.mappers().mapper(), mapper -> visitMapper(code, mapper));

        MapperPostProcessor processor = new MapperPostProcessor();
        processor.mappers = mappers;

        return processor;
    }

    private Mapper visitMapper(PostProcessorSpecificationSourceCode code, PostProcessorParserGrammar.MapperContext mapper)
    {

        if (mapper.tableMapper() != null)
        {
            return visitTableMapper(code, mapper.tableMapper());
        }
        else if (mapper.schemaMapper() != null)
        {
            return visitSchemaMapper(code, mapper.schemaMapper());
        }

        throw new EngineException("Unsupported syntax", code.getWalkerSourceInformation().getSourceInformation(mapper), EngineErrorType.PARSER);
    }

    private SchemaNameMapper visitSchemaMapper(PostProcessorSpecificationSourceCode code, PostProcessorParserGrammar.SchemaMapperContext schemaMapper)
    {
        PostProcessorParserGrammar.MapperFromContext fromCtx = PureGrammarParserUtility.validateAndExtractRequiredField(schemaMapper.mapperFrom(), "from", code.getWalkerSourceInformation().getSourceInformation(schemaMapper));
        PostProcessorParserGrammar.MapperToContext toCtx = PureGrammarParserUtility.validateAndExtractRequiredField(schemaMapper.mapperTo(), "to", code.getWalkerSourceInformation().getSourceInformation(schemaMapper));

        SchemaNameMapper mapper = new SchemaNameMapper();
        mapper.from = PureGrammarParserUtility.fromGrammarString(fromCtx.STRING().getText(), true);
        mapper.to = PureGrammarParserUtility.fromGrammarString(toCtx.STRING().getText(), true);

        return mapper;
    }

    private TableNameMapper visitTableMapper(PostProcessorSpecificationSourceCode code, PostProcessorParserGrammar.TableMapperContext tableMapper)
    {
        SourceInformation sourceInformation = code.getWalkerSourceInformation().getSourceInformation(tableMapper);
        PostProcessorParserGrammar.MapperFromContext fromCtx = PureGrammarParserUtility.validateAndExtractRequiredField(tableMapper.mapperFrom(), "from", sourceInformation);
        PostProcessorParserGrammar.MapperToContext toCtx = PureGrammarParserUtility.validateAndExtractRequiredField(tableMapper.mapperTo(), "to", sourceInformation);

        PostProcessorParserGrammar.SchemaFromContext schemaFrom = PureGrammarParserUtility.validateAndExtractRequiredField(tableMapper.schemaFrom(), "schemaFrom", sourceInformation);
        PostProcessorParserGrammar.SchemaToContext schemaTo = PureGrammarParserUtility.validateAndExtractRequiredField(tableMapper.schemaTo(), "schemaTo", sourceInformation);


        SchemaNameMapper schemaMapper = new SchemaNameMapper();
        schemaMapper.from = PureGrammarParserUtility.fromGrammarString(schemaFrom.STRING().getText(), true);
        schemaMapper.to = schemaTo == null ? schemaMapper.from : PureGrammarParserUtility.fromGrammarString(schemaTo.STRING().getText(), true);

        TableNameMapper mapper = new TableNameMapper();
        mapper.from = PureGrammarParserUtility.fromGrammarString(fromCtx.STRING().getText(), true);
        mapper.to = PureGrammarParserUtility.fromGrammarString(toCtx.STRING().getText(), true);
        mapper.schema = schemaMapper;

        return mapper;
    }

    public static <T extends RuleContext> T required(List<T> contexts, String fieldName, SourceInformation sourceInformation)
    {
        return PureGrammarParserUtility.validateAndExtractRequiredField(contexts, fieldName, sourceInformation);
    }

    public static <T extends RuleContext> T optional(List<T> contexts, String fieldName, SourceInformation sourceInformation)
    {
        return PureGrammarParserUtility.validateAndExtractRequiredField(contexts, fieldName, sourceInformation);
    }

    private String string(TerminalNode text)
    {
        return PureGrammarParserUtility.fromGrammarString(text.getText(), true);
    }
}
