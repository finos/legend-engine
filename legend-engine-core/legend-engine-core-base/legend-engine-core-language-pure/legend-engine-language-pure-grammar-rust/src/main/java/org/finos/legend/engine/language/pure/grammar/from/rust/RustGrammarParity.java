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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shadow-mode comparison of the Java and Rust parsers' results for one section or standalone expression.
 *
 * <p>Both results are compared as protocol objects: the Rust JSON is deserialized into the engine's protocol classes
 * and both sides are serialized with the same mapper, so field order, omitted empty lists and nulls don't matter.
 * When the Java parser fails, only the error's start position is compared (not ANTLR's message wording).</p>
 *
 * <ul>
 *     <li>{@value #FAIL_ON_MISMATCH_PROPERTY}{@code =true}: a mismatch throws an {@link AssertionError};</li>
 *     <li>{@value #REPORT_DIR_PROPERTY}{@code =<dir>}: mismatches ({@code <dir>/mismatch}) and unsupported input
 *     ({@code <dir>/unsupported}) are written there, one file per section;</li>
 *     <li>a summary of the counts is printed to stderr when the JVM exits.</li>
 * </ul>
 */
public final class RustGrammarParity
{
    /**
     * System property: throw on a mismatch.
     */
    public static final String FAIL_ON_MISMATCH_PROPERTY = "legend.grammar.rust.failOnMismatch";

    /**
     * System property: directory for mismatch and unsupported-input reports.
     */
    public static final String REPORT_DIR_PROPERTY = "legend.grammar.rust.reportDir";

    /**
     * The outcome of one comparison.
     */
    public enum Outcome
    {
        /**
         * Same elements, imports and section class.
         */
        MATCH,
        /**
         * Both parsers rejected the section, at the same position.
         */
        ERROR_MATCH,
        /**
         * The results differ.
         */
        MISMATCH,
        /**
         * The Rust parser reported the section as unsupported.
         */
        UNSUPPORTED,
        /**
         * The native library is not available, so nothing was compared.
         */
        UNAVAILABLE
    }

    private static final Map<Outcome, AtomicLong> COUNTS = new EnumMap<>(Outcome.class);

    static
    {
        for (Outcome outcome : Outcome.values())
        {
            COUNTS.put(outcome, new AtomicLong());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(RustGrammarParity::printSummary));
    }

    private RustGrammarParity()
    {
    }

    /**
     * The comparisons counted so far in this JVM.
     *
     * @return the count for each outcome
     */
    public static Map<Outcome, Long> counts()
    {
        Map<Outcome, Long> counts = new EnumMap<>(Outcome.class);
        COUNTS.forEach((outcome, count) -> counts.put(outcome, count.get()));
        return Collections.unmodifiableMap(counts);
    }

    static void compare(SectionSourceCode code, boolean returnSourceInfo, List<PackageableElement> javaElements, Section javaSection, RuntimeException javaError)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            COUNTS.get(Outcome.UNAVAILABLE).incrementAndGet();
            return;
        }
        Input input = Input.of(code);
        RustSectionResult rust = RustSectionResult.parse(code, returnSourceInfo);
        if (rust.isUnsupported())
        {
            unsupported(input, returnSourceInfo, rust.errorMessage);
            return;
        }
        JsonNode rustResult = rust.isError() ? null : render(rust.elements, rust.imports);
        if (javaError != null)
        {
            compareErrors(input, returnSourceInfo, javaError, rust.errorKind, rust.errorMessage, rust.errorSourceInformation, rustResult);
            return;
        }
        compareResults(input, returnSourceInfo, render(RustSectionResult.roundTrip(javaElements), RustSectionResult.imports(javaSection)), rust.errorKind, rust.errorMessage, rustResult);
    }

    static <T> void compareExpression(RustExpression<T> expression, boolean returnSourceInfo, T javaValue, RuntimeException javaError)
    {
        if (!NativeGrammarParser.isAvailable())
        {
            COUNTS.get(Outcome.UNAVAILABLE).incrementAndGet();
            return;
        }
        Input input = Input.of(expression);
        RustExpressionResult<T> rust = RustExpressionResult.parse(expression, returnSourceInfo);
        if (rust.isUnsupported())
        {
            unsupported(input, returnSourceInfo, rust.errorMessage);
            return;
        }
        JsonNode rustResult = rust.isError() ? null : render(rust.value, expression.type);
        if (javaError != null)
        {
            compareErrors(input, returnSourceInfo, javaError, rust.errorKind, rust.errorMessage, rust.errorSourceInformation, rustResult);
            return;
        }
        compareResults(input, returnSourceInfo, render(roundTrip(javaValue, expression.type), expression.type), rust.errorKind, rust.errorMessage, rustResult);
    }

    private static void unsupported(Input input, boolean returnSourceInfo, String message)
    {
        COUNTS.get(Outcome.UNSUPPORTED).incrementAndGet();
        report("unsupported", input, returnSourceInfo, message, null, null);
    }

    /**
     * Compares the rendered results when Java parsed the input: Rust must have too ({@code rust} is null when it
     * reported an error), with the same result.
     */
    private static void compareResults(Input input, boolean returnSourceInfo, JsonNode java, String rustErrorKind, String rustErrorMessage, JsonNode rust)
    {
        if (rust == null)
        {
            mismatch(input, returnSourceInfo, "Java parsed the input; Rust reported " + rustErrorKind + ": " + rustErrorMessage, java, null);
        }
        else if (java.equals(rust))
        {
            COUNTS.get(Outcome.MATCH).incrementAndGet();
        }
        else
        {
            mismatch(input, returnSourceInfo, "results differ at " + firstDifference(java, rust, ""), java, rust);
        }
    }

    /**
     * Compares the errors when Java rejected the input: Rust must report a parser error ({@code rust} is its result
     * when it parsed the input), at the same start position.
     */
    private static void compareErrors(Input input, boolean returnSourceInfo, RuntimeException javaError, String rustErrorKind, String rustErrorMessage, SourceInformation rustPosition, JsonNode rust)
    {
        EngineException engineException = EngineException.findException(javaError);
        String javaMessage = (engineException == null) ? String.valueOf(javaError.getMessage()) : engineException.getMessage();
        if (!RustSectionResult.PARSER_ERROR.equals(rustErrorKind))
        {
            String rustOutcome = (rust == null) ? (rustErrorKind + ": " + rustErrorMessage) : "parsed it";
            mismatch(input, returnSourceInfo, "Java rejected the input (" + javaMessage + "); Rust " + rustOutcome, null, rust);
            return;
        }
        SourceInformation javaPosition = (engineException == null) ? null : engineException.getSourceInformation();
        if (returnSourceInfo && javaPosition != null && (rustPosition == null || javaPosition.startLine != rustPosition.startLine || javaPosition.startColumn != rustPosition.startColumn))
        {
            mismatch(input, returnSourceInfo, "error position differs: Java " + position(javaPosition) + " (" + javaMessage + "), Rust " + position(rustPosition) + " (" + rustErrorMessage + ")", null, null);
            return;
        }
        COUNTS.get(Outcome.ERROR_MATCH).incrementAndGet();
    }

    private static void mismatch(Input input, boolean returnSourceInfo, String reason, JsonNode java, JsonNode rust)
    {
        COUNTS.get(Outcome.MISMATCH).incrementAndGet();
        Path file = report("mismatch", input, returnSourceInfo, reason, java, rust);
        if (Boolean.getBoolean(FAIL_ON_MISMATCH_PROPERTY))
        {
            throw new AssertionError("Rust parser mismatch (returnSourceInfo=" + returnSourceInfo + "): " + reason
                    + ((file == null) ? "" : ("\nreport: " + file))
                    + "\n--- input ---\n" + input.code);
        }
    }

    /**
     * {@code value} after a round trip through protocol JSON, as the Rust result makes one: deserialization applies the
     * engine's protocol converters. Through JSON text, which keeps decimals' trailing zeros.
     */
    static <T> T roundTrip(T value, Class<T> type)
    {
        try
        {
            return RustSectionResult.MAPPER.readValue(RustSectionResult.MAPPER.writerFor(type).writeValueAsString(value), type);
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("the engine protocol does not round-trip its own " + type.getSimpleName(), e);
        }
    }

    private static <T> JsonNode render(T value, Class<T> type)
    {
        try
        {
            return RustSectionResult.MAPPER.readTree(RustSectionResult.MAPPER.writerFor(type).writeValueAsString(value));
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("cannot render a " + type.getSimpleName(), e);
        }
    }

    /**
     * A section's elements, and its imports if it is an {@link ImportAwareCodeSection} ({@code imports} not null), so
     * that the section class is compared too.
     */
    private static JsonNode render(List<PackageableElement> elements, List<String> imports)
    {
        ObjectNode node = RustSectionResult.MAPPER.createObjectNode();
        node.set("elements", RustSectionResult.toJson(elements));
        if (imports != null)
        {
            node.set("imports", RustSectionResult.MAPPER.valueToTree(imports));
        }
        return node;
    }

    /**
     * The JSON path of the first difference between two trees, with both values.
     */
    static String firstDifference(JsonNode java, JsonNode rust, String path)
    {
        if (java == null || rust == null || java.getNodeType() != rust.getNodeType())
        {
            return describe(path, java, rust);
        }
        if (java.isObject())
        {
            for (Iterator<String> names = java.fieldNames(); names.hasNext(); )
            {
                String name = names.next();
                if (!java.get(name).equals(rust.get(name)))
                {
                    return firstDifference(java.get(name), rust.get(name), path + "/" + name);
                }
            }
            for (Iterator<String> names = rust.fieldNames(); names.hasNext(); )
            {
                String name = names.next();
                if (!java.has(name))
                {
                    return describe(path + "/" + name, null, rust.get(name));
                }
            }
        }
        else if (java.isArray())
        {
            for (int i = 0; i < Math.min(java.size(), rust.size()); i++)
            {
                if (!java.get(i).equals(rust.get(i)))
                {
                    return firstDifference(java.get(i), rust.get(i), path + "/" + i);
                }
            }
            if (java.size() != rust.size())
            {
                return describe(path + " (size)", java.size(), rust.size());
            }
        }
        return describe(path, java, rust);
    }

    private static String describe(String path, Object java, Object rust)
    {
        return (path.isEmpty() ? "/" : path) + ": java=" + abbreviate(java) + " rust=" + abbreviate(rust);
    }

    private static String abbreviate(Object value)
    {
        String text = String.valueOf(value);
        return (text.length() > 200) ? (text.substring(0, 200) + "...") : text;
    }

    private static String position(SourceInformation si)
    {
        return (si == null) ? "(none)" : (si.startLine + ":" + si.startColumn);
    }

    private static Path report(String kind, Input input, boolean returnSourceInfo, String reason, JsonNode java, JsonNode rust)
    {
        String dir = System.getProperty(REPORT_DIR_PROPERTY);
        if (dir == null || dir.isEmpty())
        {
            return null;
        }
        try
        {
            Path folder = Files.createDirectories(Paths.get(dir, kind));
            Path file = folder.resolve(input.name + (returnSourceInfo ? "-positions" : "") + ".txt");
            String text = "reason: " + reason + "\nreturnSourceInfo: " + returnSourceInfo
                    + "\ninput: " + input.type
                    + "\nsourceId: " + input.sourceId
                    + "\nlineOffset: " + input.lineOffset
                    + "\n--- input ---\n" + input.code
                    + "\n--- java ---\n" + pretty(java)
                    + "\n--- rust ---\n" + pretty(rust) + "\n";
            Files.write(file, text.getBytes(StandardCharsets.UTF_8));
            return file;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static String pretty(JsonNode node)
    {
        if (node == null)
        {
            return "(none)";
        }
        try
        {
            return RustSectionResult.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        }
        catch (JsonProcessingException e)
        {
            return node.toString();
        }
    }

    /**
     * A stable name for a section: SHA-256 of its type, body and offsets.
     */
    static String hash(SectionSourceCode code)
    {
        return RustExpression.hash(code.sectionType, code.walkerSourceInformation.getLineOffset(), code.walkerSourceInformation.getColumnOffset(), code.code);
    }

    /**
     * What was parsed, as the reports name and show it.
     */
    private static final class Input
    {
        final String name;
        final String type;
        final String sourceId;
        final int lineOffset;
        final String code;

        private Input(String name, String type, String sourceId, int lineOffset, String code)
        {
            this.name = name;
            this.type = type;
            this.sourceId = sourceId;
            this.lineOffset = lineOffset;
            this.code = code;
        }

        static Input of(SectionSourceCode code)
        {
            return new Input(hash(code), "section " + code.sectionType, code.walkerSourceInformation.getSourceId(), code.walkerSourceInformation.getLineOffset(), code.code);
        }

        static Input of(RustExpression<?> expression)
        {
            return new Input(expression.hash(), "expression " + expression.kind, expression.sourceId, expression.lineOffset, expression.code);
        }
    }

    private static void printSummary()
    {
        long total = COUNTS.values().stream().mapToLong(AtomicLong::get).sum();
        if (total > 0)
        {
            System.err.println("[rust-parser parity] " + counts());
        }
    }
}
