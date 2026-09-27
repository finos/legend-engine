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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.extension.ExpressionParserOverride;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParserOverride;

import java.util.Arrays;

/**
 * Takes over section types with Rust-backed parsers ({@link RustSectionParser}), each keeping the Java parser it
 * replaces, whether the engine's own or another extension's, and standalone expressions likewise
 * ({@link RustExpressionParser}). In the default mode ({@code off}) everything parses exactly as without this
 * extension. The section types are fixed when the extension loads; the mode is read on every parse
 * (see {@link RustGrammarMode}).
 */
public class RustGrammarParserExtension implements PureGrammarParserExtension
{
    /**
     * System property: the section types taken over, comma-separated; {@code Pure} by default. Read when the
     * extensions load.
     */
    public static final String SECTIONS_PROPERTY = "legend.grammar.rust.sections";

    private final ImmutableList<String> sectionTypes;

    /**
     * Takes over the section types {@value #SECTIONS_PROPERTY} lists.
     */
    public RustGrammarParserExtension()
    {
        this(System.getProperty(SECTIONS_PROPERTY, "Pure").split(","));
    }

    /**
     * @param sectionTypes the section types to take over
     */
    public RustGrammarParserExtension(String... sectionTypes)
    {
        this.sectionTypes = Lists.immutable.withAll(Arrays.asList(sectionTypes)).collect(String::trim).select(type -> !type.isEmpty()).distinct();
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
