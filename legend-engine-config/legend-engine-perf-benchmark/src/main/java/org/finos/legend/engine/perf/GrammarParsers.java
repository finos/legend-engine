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

package org.finos.legend.engine.perf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensionLoader;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.rust.RustGrammarMode;
import org.finos.legend.engine.language.pure.grammar.from.rust.RustGrammarParserExtension;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

/**
 * The two grammar parsers a run can measure.
 *
 * The Java parser is the engine's own, built from every grammar extension but the Rust one, so the
 * parse phase measures exactly what it measured before legend-engine-language-pure-grammar-rust was
 * on this classpath.
 *
 * The Rust parser is that module's: with its native library loaded and legend.grammar.rust.mode=on,
 * PureGrammarParser.parseModel parses the whole model in Rust. It has two ways of quietly parsing with
 * Java instead - falling back when it fails, and parsing a model holding a section type it does not
 * take over with the engine's parser - and either would time Java under the Rust parser's name. So
 * its measurements run with fallback off, models with such a section are refused before measuring,
 * and the first parse of a workload must return the Java parser's elements.
 */
final class GrammarParsers
{
    /**
     * A section header, as the engine's grammar splits a model: a line break, ### and a name.
     */
    private static final Pattern SECTION_HEADER = Pattern.compile("\n###([A-Za-z0-9_][A-Za-z0-9_$]*)");

    private GrammarParsers()
    {
    }

    static PureGrammarParser java()
    {
        List<PureGrammarParserExtension> extensions = PureGrammarParserExtensionLoader.extensions().stream()
                .filter(extension -> !(extension instanceof RustGrammarParserExtension))
                .collect(Collectors.toList());
        return PureGrammarParser.newInstance(PureGrammarParserExtensions.fromExtensions(extensions));
    }

    static PureGrammarParser rust()
    {
        return PureGrammarParser.newInstance();
    }

    /**
     * Why the Rust parser cannot parse this grammar here, or null when it can.
     */
    static String whyRustCannotParse(String grammar)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            return "the Rust parser library is not loaded (" + NativeGrammarParser.getLoadFailure() + ")";
        }
        List<String> takenOver = rustSectionTypes();
        Set<String> notTakenOver = new TreeSet<>();
        // Code before any header is a Pure section, as the engine's parser reads it.
        Matcher header = SECTION_HEADER.matcher("\n###Pure\n" + grammar);
        while (header.find())
        {
            if (!takenOver.contains(header.group(1)))
            {
                notTakenOver.add(header.group(1));
            }
        }
        return notTakenOver.isEmpty() ? null : "the Rust parser does not take over section types " + notTakenOver;
    }

    /**
     * Runs {@code parse} with the Rust parser on and its fallback to Java off, then restores both
     * properties, which every PureGrammarParser in the JVM reads.
     */
    static <T> T withRust(Supplier<T> parse)
    {
        String mode = System.getProperty(RustGrammarMode.PROPERTY);
        String fallback = System.getProperty(RustGrammarMode.FALLBACK_PROPERTY);
        System.setProperty(RustGrammarMode.PROPERTY, "on");
        System.setProperty(RustGrammarMode.FALLBACK_PROPERTY, "false");
        try
        {
            return parse.get();
        }
        finally
        {
            restore(RustGrammarMode.PROPERTY, mode);
            restore(RustGrammarMode.FALLBACK_PROPERTY, fallback);
        }
    }

    /**
     * Fails unless both parsers returned the same elements.
     */
    static void checkSameElements(PureModelContextData java, PureModelContextData rust)
    {
        List<String> javaPaths = paths(java);
        List<String> rustPaths = paths(rust);
        if (!javaPaths.equals(rustPaths))
        {
            throw new IllegalStateException("The Rust parser returned other elements than the Java parser: Java " + javaPaths + ", Rust " + rustPaths);
        }
    }

    private static List<String> rustSectionTypes()
    {
        List<String> types = new ArrayList<>();
        for (PureGrammarParserExtension extension : PureGrammarParserExtensionLoader.extensions())
        {
            if (extension instanceof RustGrammarParserExtension)
            {
                for (String type : ((RustGrammarParserExtension) extension).getSectionTypes())
                {
                    types.add(type);
                }
            }
        }
        return types;
    }

    private static List<String> paths(PureModelContextData data)
    {
        return data.getElements().stream().map(PackageableElement::getPath).collect(Collectors.toList());
    }

    private static void restore(String property, String value)
    {
        if (value == null)
        {
            System.clearProperty(property);
        }
        else
        {
            System.setProperty(property, value);
        }
    }
}
