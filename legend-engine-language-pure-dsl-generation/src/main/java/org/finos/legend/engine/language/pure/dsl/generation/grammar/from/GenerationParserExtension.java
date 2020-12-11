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

package org.finos.legend.engine.language.pure.dsl.generation.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FileGenerationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FileGenerationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.GenerationSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.GenerationSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.List;
import java.util.function.Consumer;

public class GenerationParserExtension implements PureGrammarParserExtension
{
    public static final String FILE_GENERATION_SECTION_NAME = "FileGeneration";
    public static final String GENERATION_SPECIFICATION_SECTION_NAME = "GenerationSpecification";

    @Override
    public List<Function3<SectionSourceCode, Consumer<PackageableElement>, PureGrammarParserContext, Section>> getExtraSectionParsers()
    {
        return Lists.mutable.with(
                (sectionSourceCode, elementConsumer, context) ->
                {
                    if (!FILE_GENERATION_SECTION_NAME.equals(sectionSourceCode.sectionType))
                    {
                        return null;
                    }
                    SourceCodeParserInfo parserInfo = getFileGenerationParserInfo(sectionSourceCode);
                    ImportAwareCodeSection section = new ImportAwareCodeSection();
                    section.parserName = sectionSourceCode.sectionType;
                    section.sourceInformation = parserInfo.sourceInformation;
                    FileGenerationParseTreeWalker walker = new FileGenerationParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
                    walker.visit((FileGenerationParserGrammar.DefinitionContext) parserInfo.rootContext);
                    return section;
                },
                (sectionSourceCode, elementConsumer, context) ->
                {
                    if (!GENERATION_SPECIFICATION_SECTION_NAME.equals(sectionSourceCode.sectionType))
                    {
                        return null;
                    }
                    SourceCodeParserInfo parserInfo = getGenerationSpecificationParserInfo(sectionSourceCode);
                    ImportAwareCodeSection section = new ImportAwareCodeSection();
                    section.parserName = sectionSourceCode.sectionType;
                    section.sourceInformation = parserInfo.sourceInformation;
                    GenerationSpecificationParseTreeWalker walker = new GenerationSpecificationParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
                    walker.visit((GenerationSpecificationParserGrammar.DefinitionContext) parserInfo.rootContext);
                    return section;
                }
        );
    }

    private static SourceCodeParserInfo getFileGenerationParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        FileGenerationLexerGrammar lexer = new FileGenerationLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        FileGenerationParserGrammar parser = new FileGenerationParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getGenerationSpecificationParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        GenerationSpecificationLexerGrammar lexer = new GenerationSpecificationLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        GenerationSpecificationParserGrammar parser = new GenerationSpecificationParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
