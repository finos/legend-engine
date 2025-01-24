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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingIncludeParser;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.MappingIncludeDataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.Collections;
import java.util.function.Consumer;

public class DataSpaceParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "DataSpace";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataSpace");
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.immutable.with(SectionParser.newParser(NAME, DataSpaceParserExtension::parseSection));
    }

    private static Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext pureGrammarParserContext)
    {
        SourceCodeParserInfo parserInfo = getDataSpaceParserInfo(sectionSourceCode);
        DefaultCodeSection section = new DefaultCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;
        DataSpaceParseTreeWalker walker = new DataSpaceParseTreeWalker(parserInfo.input, parserInfo.walkerSourceInformation, elementConsumer, section, pureGrammarParserContext);
        walker.visit((DataSpaceParserGrammar.DefinitionContext) parserInfo.rootContext);
        return section;
    }

    private static SourceCodeParserInfo getDataSpaceParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        DataSpaceLexerGrammar lexer = new DataSpaceLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        DataSpaceParserGrammar parser = new DataSpaceParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    @Override
    public Iterable<? extends MappingIncludeParser> getExtraMappingIncludeParsers()
    {
        return org.eclipse.collections.api.factory.Lists.immutable.with(
                MappingIncludeParser.newParser("dataspace", DataSpaceParserExtension::parseMappingInclude)
        );
    }

    private static MappingInclude parseMappingInclude(MappingParserGrammar.IncludeMappingContext ctx,
                                                      ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        MappingIncludeDataSpace mappingIncludeDataSpace = new MappingIncludeDataSpace();
        mappingIncludeDataSpace.includedDataSpace =
                PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        mappingIncludeDataSpace.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        return mappingIncludeDataSpace;
    }

    @Override
    public Iterable<? extends EmbeddedDataParser> getExtraEmbeddedDataParsers()
    {
        return org.eclipse.collections.api.factory.Lists.immutable.with(new DataspaceDataElementReferenceParser());
    }
}
