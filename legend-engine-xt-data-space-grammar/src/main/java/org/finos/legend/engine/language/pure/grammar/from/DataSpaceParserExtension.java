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
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingIncludeParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.IncludeStoreDataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.MappingIncludeDataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.IncludedStore;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DataSpaceParserExtension implements IRelationalGrammarParserExtension
{
    public static final String NAME = "DataSpace";

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
        DataSpaceParseTreeWalker walker = new DataSpaceParseTreeWalker(parserInfo.input, parserInfo.walkerSourceInformation, elementConsumer, section);
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
        List<MappingParserGrammar.StoreSubPathContext> storeSubPathContextList = ctx.storeSubPath();
        if (storeSubPathContextList.size() == 1)
        {
            MappingParserGrammar.StoreSubPathContext storeSubPathContext = storeSubPathContextList.get(0);
            mappingIncludeDataSpace.sourceDatabasePath =
                    PureGrammarParserUtility.fromQualifiedName(storeSubPathContext.sourceStore().qualifiedName().packagePath() == null ? Collections.emptyList() : storeSubPathContext.sourceStore().qualifiedName().packagePath().identifier(), storeSubPathContext.sourceStore().qualifiedName().identifier());
            mappingIncludeDataSpace.targetDatabasePath =
                    PureGrammarParserUtility.fromQualifiedName(storeSubPathContext.targetStore().qualifiedName().packagePath() == null ? Collections.emptyList() : storeSubPathContext.targetStore().qualifiedName().packagePath().identifier(), storeSubPathContext.targetStore().qualifiedName().identifier());
        }
        else
        {
            mappingIncludeDataSpace.targetDatabasePath = null;
        }

        return mappingIncludeDataSpace;
    }

    @Override
    public List<Function<String, IncludedStore>> getExtraIncludedStoreParsers()
    {
        return Lists.mutable.of(this::parseIncludedStore);
    }

    private IncludedStore parseIncludedStore(String type)
    {
        if (type.equals("dataspace"))
        {
            return new IncludeStoreDataSpace();
        }
        else
        {
            return null;
        }
    }
}
