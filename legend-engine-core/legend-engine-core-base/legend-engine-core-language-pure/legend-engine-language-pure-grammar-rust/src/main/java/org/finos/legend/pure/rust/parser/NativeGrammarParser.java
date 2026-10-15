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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Java binding for the Rust Legend grammar parser.
 *
 * <p>The native library {@code legend_pure_rust_parser} is built from {@code legend-engine-rust/crates/jni} by this
 * module's build, which puts it in the jar as {@link #bundledLibrary()}. It is loaded from the absolute path in the
 * {@value #LIBRARY_PATH_PROPERTY} system property when set, otherwise from the jar (copied to a temporary file first),
 * otherwise from {@code java.library.path}. Loading never throws: check {@link #isAvailable()} before calling the
 * native methods.</p>
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

    /**
     * The jar directory holding the library, one directory per platform ({@code linux-x86_64}, {@code osx-aarch64},
     * ...).
     */
    public static final String BUNDLED_LIBRARY_DIRECTORY = "META-INF/native/";

    private static final String LOADED_FROM;
    private static final String LOAD_FAILURE;

    static
    {
        String loadedFrom = null;
        String failure = null;
        try
        {
            loadedFrom = load();
        }
        catch (UnsatisfiedLinkError | SecurityException | IOException e)
        {
            failure = (e.getMessage() == null) ? e.getClass().getName() : e.getMessage();
        }
        LOADED_FROM = loadedFrom;
        LOAD_FAILURE = failure;
    }

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
     * Where the native library loaded from: the {@value #LIBRARY_PATH_PROPERTY} path, the jar resource
     * {@link #bundledLibrary()}, or {@code java.library.path}.
     *
     * @return that, or null when it did not load
     */
    public static String getLoadedFrom()
    {
        return LOADED_FROM;
    }

    /**
     * The jar resource holding the library for this platform, such as
     * {@code META-INF/native/linux-x86_64/liblegend_pure_rust_parser.so}.
     *
     * @return the resource name, or null on an operating system or architecture the library is not built for
     */
    public static String bundledLibrary()
    {
        String platform = platform(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
        return (platform == null) ? null : BUNDLED_LIBRARY_DIRECTORY + platform + "/" + System.mapLibraryName(LIBRARY_NAME);
    }

    /**
     * The platform directory for a JVM's {@code os.name} and {@code os.arch}, as this module's build names it.
     *
     * @return {@code linux}, {@code osx} or {@code windows}, a dash, and {@code x86_64} or {@code aarch64}; null for
     *     anything else
     */
    static String platform(String osName, String osArch)
    {
        String os = osName.startsWith("Linux") ? "linux" : osName.startsWith("Mac") ? "osx" : osName.startsWith("Windows") ? "windows" : null;
        String arch = ("amd64".equals(osArch) || "x86_64".equals(osArch)) ? "x86_64" : ("aarch64".equals(osArch) || "arm64".equals(osArch)) ? "aarch64" : null;
        return (os == null || arch == null) ? null : os + "-" + arch;
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

    /**
     * Loads the library and says where from.
     */
    private static String load() throws IOException
    {
        String path = System.getProperty(LIBRARY_PATH_PROPERTY);
        if (path != null && !path.isEmpty())
        {
            System.load(path);
            return path;
        }
        String bundled = bundledLibrary();
        URL resource = (bundled == null) ? null : NativeGrammarParser.class.getResource("/" + bundled);
        if (resource != null)
        {
            System.load(copyToTemporaryFile(resource, bundled).toString());
            return bundled;
        }
        try
        {
            System.loadLibrary(LIBRARY_NAME);
            return "java.library.path";
        }
        catch (UnsatisfiedLinkError e)
        {
            String notBundled = (bundled == null)
                    ? "no native library is built for " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                    : "the jar has no " + bundled;
            UnsatisfiedLinkError error = new UnsatisfiedLinkError(notBundled + ", and " + e.getMessage());
            error.initCause(e);
            throw error;
        }
    }

    /**
     * A copy of the jar's library on disk, where {@link System#load(String)} can read it, deleted when the JVM exits.
     */
    private static Path copyToTemporaryFile(URL resource, String name) throws IOException
    {
        String fileName = name.substring(name.lastIndexOf('/') + 1);
        Path file = Files.createTempFile(LIBRARY_NAME + "-", fileName.substring(fileName.lastIndexOf('.')));
        file.toFile().deleteOnExit();
        try (InputStream library = resource.openStream())
        {
            Files.copy(library, file, StandardCopyOption.REPLACE_EXISTING);
        }
        return file;
    }
}
