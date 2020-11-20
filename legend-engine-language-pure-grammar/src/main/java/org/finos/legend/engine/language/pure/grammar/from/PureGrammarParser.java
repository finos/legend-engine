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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.CodeLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.CodeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionParser;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingParser;
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class PureGrammarParser
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final String DEFAULT_SECTION_BEGIN = "\n###" + DomainParser.name + "\n";

    private final DEPRECATED_PureGrammarParserLibrary parsers;
    private final List<PureGrammarParserExtension> extensions;

    private PureGrammarParser(List<PureGrammarParserExtension> extensions)
    {
        this.parsers = new DEPRECATED_PureGrammarParserLibrary(Lists.immutable.with(
                new DomainParser(),
                MappingParser.newInstance(),
                ConnectionParser.newInstance(),
                new RuntimeParser()
        ));
        this.extensions = extensions;
    }

    public static PureGrammarParser newInstance()
    {
        return new PureGrammarParser(PureGrammarParserExtensionLoader.extensions());
    }

    public PureModelContextData parseModel(String code)
    {
        return this.parse(code, this.parsers, this.extensions);
    }

    public Lambda parseLambda(String code, String lambdaId)
    {
        return new DomainParser().parseLambda(code, lambdaId);
    }

    public PureModelContextData parse(String code, DEPRECATED_PureGrammarParserLibrary parserLibrary, List<PureGrammarParserExtension> extensions)
    {
        String fullCode = DEFAULT_SECTION_BEGIN + code;
        PureGrammarParserContext parserContext = new PureGrammarParserContext(new HashMap<>());
        ParseTreeWalkerSourceInformation walkerSourceInformation = ParseTreeWalkerSourceInformation.DEFAULT_WALKER_SOURCE_INFORMATION();
        // init the parser
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        CodeLexerGrammar lexer = new CodeLexerGrammar(CharStreams.fromString(fullCode));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        CodeParserGrammar parser = new CodeParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        // create the PureModelContextData builder
        PureModelContextData.Builder builder = new PureModelContextData.Builder();
        // create the section index
        SectionIndex sectionIndex = new SectionIndex();
        // NOTE: we intentionally set section index name and package like this since we don't want to expose this feature yet to end user
        // in the consumer, we should ensure this does not leak and gets persisted to SDLC or Services per se
        sectionIndex.name = "SectionIndex";
        sectionIndex._package = "__internal__";
        sectionIndex.sections = ListIterate.collect(parser.definition().section(), sectionCtx -> this.visitSection(sectionCtx, parserLibrary, extensions, walkerSourceInformation, parserContext, builder::addElement));
        return builder.withElement(sectionIndex).build();
    }

    private Section visitSection(CodeParserGrammar.SectionContext ctx, DEPRECATED_PureGrammarParserLibrary parserLibrary, List<PureGrammarParserExtension> extensions, ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext parserContext, Consumer<PackageableElement> elementConsumer)
    {
        List<Function3<SectionSourceCode, Consumer<PackageableElement>, PureGrammarParserContext, Section>> extraSectionParsers = ListIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraSectionParsers);
        String parserName = ctx.SECTION_START().getText().substring(4); // the prefix is `\n###` hence 4 characters
        SourceInformation parserNameSourceInformation = walkerSourceInformation.getSourceInformation(ctx.SECTION_START().getSymbol());
        int lineOffset = ctx.SECTION_START().getSymbol().getLine() - 2; // since the CODE_BLOCK_START is `\n###` we have to subtract 1 more line than usual
        ParseTreeWalkerSourceInformation sectionWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder("", lineOffset, 0).build();
        SourceInformation sectionSourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        if (ctx.sectionContent() != null)
        {
            try
            {
                StringBuilder codeBuilder = new StringBuilder();
                for (CodeParserGrammar.SectionContentContext tn : ctx.sectionContent())
                {
                    codeBuilder.append(tn.getText());
                }
                SectionSourceCode codeSection = new SectionSourceCode(codeBuilder.toString(), parserName, sectionSourceInformation, sectionWalkerSourceInformation);
                Section section = extraSectionParsers.stream().map(sectionParser -> sectionParser.value(codeSection, elementConsumer, parserContext)).filter(Objects::nonNull).findFirst().orElse(null);
                if (section == null)
                {
                    DEPRECATED_SectionGrammarParser parser = parserLibrary.getParser(parserName, parserNameSourceInformation);
                    if (parser == null)
                    {
                        throw new EngineException("'" + parserName + "' is not a known section parser", parserNameSourceInformation, EngineErrorType.PARSER);
                    }
                    section = parser.parse(parser.getParserInfo(codeSection.code, codeSection.sourceInformation, codeSection.walkerSourceInformation), elementConsumer, parserContext);
                }
                // remove duplicates in imports and content of the section
                section.elements = ListIterate.distinct(section.elements);
                if (section instanceof ImportAwareCodeSection)
                {
                    ((ImportAwareCodeSection) section).imports = ListIterate.distinct(((ImportAwareCodeSection) section).imports);
                }
                return section;
            }
            catch (RuntimeException e)
            {
                EngineException engineException = EngineException.findException(e);
                if (engineException != null && engineException.getSourceInformation() != null)
                {
                    throw engineException;
                }
                String message = e instanceof UnsupportedOperationException && (e.getMessage() == null || e.getMessage().isEmpty())
                        ? "Unsupported syntax"
                        : e instanceof NullPointerException ? "An exception of type 'NullPointerException' occurred, please notify developer" : e.getMessage();
                LOGGER.error(new LogInfo(null, LoggingEventType.GRAMMAR_PARSING_ERROR, message).toString(), e);
                throw new EngineException(message, sectionSourceInformation, EngineErrorType.PARSER);
            }
        }
        // create default section for empty section
        Section section = new DefaultCodeSection();
        section.parserName = parserName;
        section.sourceInformation = sectionSourceInformation;
        return section;
    }
}
