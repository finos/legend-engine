// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.rust;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.extension.ExpressionParserOverride;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParserOverride;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Takes over, with Rust-backed parsers ({@link RustSectionParser}), the section types the Rust library parses, each
 * keeping the Java parser it replaces, whether the engine's own or another extension's, and standalone expressions
 * likewise ({@link RustExpressionParser}). In the default mode ({@code off}) everything parses exactly as without this
 * extension. The section types are fixed when the extension loads; the mode is read on every parse
 * (see {@link RustGrammarMode}).
 */
public class RustGrammarParserExtension implements PureGrammarParserExtension
{
    /**
     * System property, for corpus dumps ({@value RustGrammarCorpus#CORPUS_DIR_PROPERTY}): section types to take over
     * besides those the Rust library parses, comma-separated, so that the Java parser's results for a grammar can be
     * recorded before its Rust port exists. The Rust library reports such a section unsupported, so the Java parser
     * parses it in every mode. Read when the extensions load.
     */
    public static final String CORPUS_SECTIONS_PROPERTY = "legend.grammar.rust.corpusSections";

    private final ImmutableList<String> sectionTypes;

    /**
     * Takes over the section types the Rust library parses, as its capabilities report them, and those
     * {@value #CORPUS_SECTIONS_PROPERTY} adds. Without the library, {@code Pure}, which every Rust library parses: the
     * {@code off} mode parses with Java regardless, and {@code on} and {@code shadow} fail on the first section rather
     * than quietly parse with Java.
     */
    public RustGrammarParserExtension()
    {
        this(defaultSectionTypes());
    }

    /**
     * @param sectionTypes the section types to take over
     */
    public RustGrammarParserExtension(String... sectionTypes)
    {
        this.sectionTypes = Lists.immutable.withAll(Arrays.asList(sectionTypes)).collect(String::trim).select(type -> !type.isEmpty()).distinct();
    }

    private static String[] defaultSectionTypes()
    {
        List<String> types = new ArrayList<>();
        if (NativeGrammarParser.isAvailable())
        {
            try
            {
                new ObjectMapper().readTree(NativeGrammarParser.capabilities()).path("sections").forEach(type -> types.add(type.asText()));
            }
            catch (JsonProcessingException e)
            {
                throw new IllegalStateException("cannot read the Rust parser's capabilities", e);
            }
        }
        else
        {
            types.add("Pure");
        }
        types.addAll(Arrays.asList(System.getProperty(CORPUS_SECTIONS_PROPERTY, "").split(",")));
        return types.toArray(new String[0]);
    }

    /**
     * The section types taken over.
     */
    public ImmutableList<String> getSectionTypes()
    {
        return this.sectionTypes;
    }

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Core", "Rust");
    }

    @Override
    public Iterable<? extends SectionParserOverride> getSectionParserOverrides()
    {
        return this.sectionTypes.collect(RustGrammarParserExtension::override);
    }

    @Override
    public Iterable<? extends ExpressionParserOverride> getExpressionParserOverrides()
    {
        return Lists.immutable.with(RustExpressionParser::new);
    }

    private static SectionParserOverride override(String sectionType)
    {
        return new SectionParserOverride()
        {
            @Override
            public String getSectionTypeName()
            {
                return sectionType;
            }

            @Override
            public SectionParser override(SectionParser replaced)
            {
                return new RustSectionParser(sectionType, replaced);
            }
        };
    }
}
