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
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DiagramLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DiagramParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.List;
import java.util.function.Consumer;

public class DiagramParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "Diagram";

    @Override
    public List<Function3<SectionSourceCode, Consumer<PackageableElement>, PureGrammarParserContext, Section>> getExtraSectionParsers()
    {
        return Lists.mutable.with((sectionSourceCode, elementConsumer, context) ->
        {
            if (!NAME.equals(sectionSourceCode.sectionType))
            {
                return null;
            }
            SourceCodeParserInfo parserInfo = getDiagramParserInfo(sectionSourceCode);
            ImportAwareCodeSection section = new ImportAwareCodeSection();
            section.parserName = sectionSourceCode.sectionType;
            section.sourceInformation = parserInfo.sourceInformation;
            DiagramParseTreeWalker walker = new DiagramParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
            walker.visit((DiagramParserGrammar.DefinitionContext) parserInfo.rootContext);
            return section;
        });
    }

    private static SourceCodeParserInfo getDiagramParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        DiagramLexerGrammar lexer = new DiagramLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        DiagramParserGrammar parser = new DiagramParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
