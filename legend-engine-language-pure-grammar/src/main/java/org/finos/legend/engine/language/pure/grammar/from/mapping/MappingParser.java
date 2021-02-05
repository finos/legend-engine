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
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.language.pure.grammar.from.DEPRECATED_SectionGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingTestInputDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensionLoader;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MappingParser implements DEPRECATED_SectionGrammarParser
{
    public static final String name = "Mapping";

    private final Map<String, MappingElementParser> extraMappingElementParsersByType;
    private final Map<String, MappingTestInputDataParser> extraMappingTestInputDataParsersByType;

    private MappingParser(List<PureGrammarParserExtension> extensions)
    {
        this.extraMappingElementParsersByType = indexExtraMappingElementParsers(extensions);
        this.extraMappingTestInputDataParsersByType = indexExtraMappingTestInputDataParsers(extensions);
    }

    public static MappingParser newInstance()
    {
        return new MappingParser(PureGrammarParserExtensionLoader.extensions());
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public SourceCodeParserInfo getParserInfo(String code, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        MappingLexerGrammar lexer = new MappingLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MappingParserGrammar parser = new MappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }

    @Override
    public Section parse(SourceCodeParserInfo sectionParserInfo, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = this.getName();
        section.sourceInformation = sectionParserInfo.sourceInformation;
        MappingParseTreeWalker walker = new MappingParseTreeWalker(sectionParserInfo.input, this.extraMappingElementParsersByType, this.extraMappingTestInputDataParsersByType, sectionParserInfo.walkerSourceInformation, elementConsumer, parserContext, section);
        walker.visitDefinition((MappingParserGrammar.DefinitionContext) sectionParserInfo.rootContext);
        return section;
    }

    private static Map<String, MappingElementParser> indexExtraMappingElementParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraMappingElementParsers),
                MappingElementParser::getElementTypeName,
                "Conflicting parsers for mapping element type");
    }

    private static Map<String, MappingTestInputDataParser> indexExtraMappingTestInputDataParsers(Iterable<? extends PureGrammarParserExtension> extensions)
    {
        return indexByKey(LazyIterate.flatCollect(extensions, PureGrammarParserExtension::getExtraMappingTestInputDataParsers),
                MappingTestInputDataParser::getInputDataTypeName,
                "Conflicting parsers for mapping test input data type");
    }

    private static <T> Map<String, T> indexByKey(Iterable<? extends T> elements, Function<? super T, String> keyFn, String conflictMessagePrefix)
    {
        MutableMap<String, T> index = Maps.mutable.empty();
        elements.forEach(e ->
        {
            String key = keyFn.apply(e);
            if (index.put(key, e) != null)
            {
                throw new IllegalArgumentException(conflictMessagePrefix + ": " + key);
            }
        });
        return index.asUnmodifiable();
    }
}
