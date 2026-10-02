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

package org.finos.legend.pure.rust.parser;

/**
 * Java binding for the Rust Legend grammar parser.
 *
 * <p>The native library {@code legend_pure_rust_parser} is built from {@code legend-engine-rust/crates/jni}. It is
 * loaded from the absolute path in the {@value #LIBRARY_PATH_PROPERTY} system property when set, otherwise from
 * {@code java.library.path}. Loading never throws: check {@link #isAvailable()} before calling the native
 * methods.</p>
 *
 * <p>The methods return JSON as UTF-8 bytes: one copy into a Java array, which Jackson reads as it is, where a Java
 * string would be converted twice on its way out of the library. The surface is generic: which grammars are available
 * depends only on what was linked into the native library (see {@link #capabilities()}).</p>
 */
public final class NativeGrammarParser
{
    /**
     * System property holding the absolute path of the native library.
     */
    public static final String LIBRARY_PATH_PROPERTY = "legend.pure.rust.parser.lib";

    /**
     * Library name used with {@link System#loadLibrary(String)} when {@value #LIBRARY_PATH_PROPERTY} is not set.
     */
    public static final String LIBRARY_NAME = "legend_pure_rust_parser";

    private static final String LOAD_FAILURE = load();

    private NativeGrammarParser()
    {
    }

    /**
     * Whether the native library loaded.
     *
     * @return true when the native methods can be called
     */
    public static boolean isAvailable()
    {
        return LOAD_FAILURE == null;
    }

    /**
     * Why the native library did not load.
     *
     * @return the failure message, or null when it loaded
     */
    public static String getLoadFailure()
    {
        return LOAD_FAILURE;
    }

    /**
     * The grammars linked into the native library.
     *
     * @return JSON {@code {"sections":[...],"islands":[...]}}, UTF-8
     */
    public static native byte[] capabilities();

    /**
     * Parses one grammar section body into Protocol v1 elements.
     *
     * <p>{@code code} is the section body without its {@code ###} header; positions are always reported, in document
     * coordinates using the offsets, as the engine's section parsers receive them.</p>
     *
     * @param sectionType      the section type from the header, e.g. {@code Pure}
     * @param code             the section body
     * @param sourceId         the source id reported in source information
     * @param lineOffset       added to every line number
     * @param columnOffset     added to column numbers on the body's first line
     * @return JSON {@code {"elements":[...],"imports":[...]}} ({@code imports} absent when the section's grammar has
     *     none), or {@code {"error":{"kind","message",...}}} where kind is {@code parser}, {@code unsupported} or
     *     {@code internal}; UTF-8; null only if the result could not be allocated
     */
    public static native byte[] parseSection(String sectionType, String code, String sourceId, int lineOffset, int columnOffset);

    /**
     * Parses a standalone expression into Protocol v1, as the engine's {@code PureGrammarParser} parses lambdas
     * ({@code parseLambda}), value specifications ({@code parseValueSpecification}) and graph fetch trees
     * ({@code parseGraphFetch}), with positions.
     *
     * @param kind             {@code lambda}, {@code valueSpecification} or {@code graphFetch}
     * @param code             the expression's text
     * @param sourceId         the source id reported in source information
     * @param lineOffset       added to every line number
     * @param columnOffset     added to column numbers on the text's first line
     * @return JSON {@code {"value":...}} (a lambda, a value specification or a root graph fetch tree), or the error
     *     JSON {@link #parseSection} returns; UTF-8; null only if the result could not be allocated
     */
    public static native byte[] parseExpression(String kind, String code, String sourceId, int lineOffset, int columnOffset);

    /**
     * Parses a whole model into Protocol v1 elements, as the engine's {@code PureGrammarParser.parseModel} does: the
     * code split into sections as the engine splits it (the first section's {@code ###Pure} header optional), every
     * section parsed, then the section index.
     *
     * @param code         the model's code
     * @param sourceId     the source id reported in source information
     * @param lineOffset   added to every line number
     * @param columnOffset added to column numbers on the code's first line
     * @return JSON {@code {"elements":[...]}} (the section index last), or the error JSON {@link #parseSection}
     *     returns; UTF-8; null only if the result could not be allocated
     */
    public static native byte[] parseModel(String code, String sourceId, int lineOffset, int columnOffset);

    private static String load()
    {
        String path = System.getProperty(LIBRARY_PATH_PROPERTY);
        try
        {
            if (path == null || path.isEmpty())
            {
                System.loadLibrary(LIBRARY_NAME);
            }
            else
            {
                System.load(path);
            }
            return null;
        }
        catch (UnsatisfiedLinkError | SecurityException e)
        {
            return (e.getMessage() == null) ? e.getClass().getName() : e.getMessage();
        }
    }
}
