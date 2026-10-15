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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shadow-mode comparison of the Java and Rust parsers' results for one section, standalone expression or whole model.
 *
 * <p>Both results are compared as protocol objects: the Rust JSON is deserialized into the engine's protocol classes
 * and both sides are serialized with the same mapper, so field order, omitted empty lists and nulls don't matter.
 * When the Java parser fails, only the error's start position is compared (not ANTLR's message wording). When the
 * objects match and positions were asked for, the Rust JSON as the parser wrote it is compared too, with the Java
 * result's JSON: deserialization can't hide a field the engine's reader drops or alters there.</p>
 *
 * <p>A difference the accepted list names (see {@link #ACCEPTED_PROPERTY}) is {@link Outcome#ACCEPTED}, and a grammar
 * the library doesn't link is {@link Outcome#UNLINKED} when the list names it; anything else that is not a match is
 * unexpected (see {@link #unexpected()}).</p>
 *
 * <ul>
 *     <li>{@value #FAIL_ON_MISMATCH_PROPERTY}{@code =true}: an unexpected mismatch throws an {@link AssertionError};</li>
 *     <li>{@value #REPORT_DIR_PROPERTY}{@code =<dir>}: unexpected mismatches ({@code <dir>/mismatch}) and unsupported
 *     input ({@code <dir>/unsupported}) are written there, one file per input, and, when the JVM exits, a summary
 *     ({@code <dir>/summary}) of the counts, the unexpected entries, the accepted list's keys hit and the unlinked
 *     grammars;</li>
 *     <li>a summary of the counts is printed to stderr when the JVM exits, whole models' apart.</li>
 * </ul>
 */
public final class RustGrammarParity
{
    /**
     * System property: throw on an unexpected mismatch.
     */
    public static final String FAIL_ON_MISMATCH_PROPERTY = "legend.grammar.rust.failOnMismatch";

    /**
     * System property: directory for mismatch and unsupported-input reports, and the summary.
     */
    public static final String REPORT_DIR_PROPERTY = "legend.grammar.rust.reportDir";

    /**
     * System property: the accepted list to use instead of the module's own ({@value #ACCEPTED_RESOURCE}). Each line
     * is a key, then its category and reason: an input's hash ({@code model-<hash>} for a whole model), or
     * {@code unlinked:<kind>:<type>} for a grammar the library doesn't link. Blank lines and {@code #} comments are
     * ignored.
     */
    public static final String ACCEPTED_PROPERTY = "legend.grammar.rust.parityAccepted";

    static final String ACCEPTED_RESOURCE = "legend-grammar-rust/parity-accepted.txt";

    /**
     * System property, set when the build provides the native library: then a comparison it couldn't make is
     * unexpected.
     */
    static final String REQUIRED_PROPERTY = "legend.pure.rust.parser.required";

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
         * The protocol objects match, but the Rust JSON differs from the Java result's: the engine's reader drops or
         * alters something the Rust parser wrote.
         */
        RAW_MISMATCH,
        /**
         * A difference the accepted list names.
         */
        ACCEPTED,
        /**
         * The Rust parser reported a grammar the library doesn't link, and the accepted list names it; or the Rust
         * result has a type no protocol extension on this classpath defines, and the Java parser rejected the input too.
         */
        UNLINKED,
        /**
         * The Rust parser reported the input unsupported otherwise.
         */
        UNSUPPORTED,
        /**
         * The native library is not available, so nothing was compared.
         */
        UNAVAILABLE
    }

    /**
     * How the Rust parser reports a grammar the library doesn't link: its kind, then its type.
     */
    private static final Pattern UNLINKED_GRAMMAR = Pattern.compile("^no (.+?) grammar is linked for '(.+?)'");

    private static final Map<Outcome, AtomicLong> COUNTS = newCounts();

    /**
     * Whole models' comparisons: in shadow mode, the engine's parser compares its sections too.
     */
    private static final Map<Outcome, AtomicLong> MODEL_COUNTS = newCounts();

    private static final Map<String, String> ACCEPTED = loadAccepted();

    private static final Set<String> ACCEPTED_HIT = ConcurrentHashMap.newKeySet();

    private static final Map<String, AtomicLong> UNLINKED = new ConcurrentHashMap<>();

    private static final Map<String, Unexpected> UNEXPECTED = Collections.synchronizedMap(new LinkedHashMap<>());

    static
    {
        Runtime.getRuntime().addShutdownHook(new Thread(RustGrammarParity::summarize));
    }

    private static Map<Outcome, AtomicLong> newCounts()
    {
        Map<Outcome, AtomicLong> counts = new EnumMap<>(Outcome.class);
        for (Outcome outcome : Outcome.values())
        {
            counts.put(outcome, new AtomicLong());
        }
        return counts;
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
        return snapshot(COUNTS);
    }

    /**
     * The whole models' comparisons counted so far in this JVM.
     *
     * @return the count for each outcome
     */
    public static Map<Outcome, Long> modelCounts()
    {
        return snapshot(MODEL_COUNTS);
    }

    /**
     * The unexpected comparisons so far in this JVM, one per key: what the accepted list would have to name, or what
     * must be fixed.
     *
     * @return the unexpected comparisons, in the order they happened
     */
    public static List<Unexpected> unexpected()
    {
        synchronized (UNEXPECTED)
        {
            return Collections.unmodifiableList(new ArrayList<>(UNEXPECTED.values()));
        }
    }

    private static Map<Outcome, Long> snapshot(Map<Outcome, AtomicLong> counters)
    {
        Map<Outcome, Long> counts = new EnumMap<>(Outcome.class);
        counters.forEach((outcome, count) -> counts.put(outcome, count.get()));
        return Collections.unmodifiableMap(counts);
    }

    static void compare(SectionSourceCode code, boolean returnSourceInfo, List<PackageableElement> javaElements, Section javaSection, RuntimeException javaError)
    {
        if (!available(COUNTS))
        {
            return;
        }
        Input input = Input.of(code);
        byte[] json = RustSectionResult.nativeParse(code);
        RustSectionResult rust = RustSectionResult.read(json, returnSourceInfo);
        if (rust.isUnsupported())
        {
            unsupported(COUNTS, input, returnSourceInfo, rust.errorMessage, javaError);
            return;
        }
        JsonNode rustResult = rust.isError() ? null : render(rust.elements, rust.imports);
        if (javaError != null)
        {
            compareErrors(COUNTS, input, returnSourceInfo, javaError, rust.errorKind, rust.errorMessage, rust.errorSourceInformation, rustResult);
            return;
        }
        JsonNode java = render(RustSectionResult.roundTrip(javaElements), RustSectionResult.imports(javaSection));
        compareResults(COUNTS, input, returnSourceInfo, java, rust.errorKind, rust.errorMessage, rustResult, java, json);
    }

    static <T> void compareExpression(RustExpression<T> expression, boolean returnSourceInfo, T javaValue, RuntimeException javaError)
    {
        if (!available(COUNTS))
        {
            return;
        }
        Input input = Input.of(expression);
        byte[] json = RustExpressionResult.nativeParse(expression);
        RustExpressionResult<T> rust = RustExpressionResult.read(json, expression.type, returnSourceInfo);
        if (rust.isUnsupported())
        {
            unsupported(COUNTS, input, returnSourceInfo, rust.errorMessage, javaError);
            return;
        }
        JsonNode rustResult = rust.isError() ? null : render(rust.value, expression.type);
        if (javaError != null)
        {
            compareErrors(COUNTS, input, returnSourceInfo, javaError, rust.errorKind, rust.errorMessage, rust.errorSourceInformation, rustResult);
            return;
        }
        JsonNode java = render(roundTrip(javaValue, expression.type), expression.type);
        compareResults(COUNTS, input, returnSourceInfo, java, rust.errorKind, rust.errorMessage, rustResult, wrap("value", java), json);
    }

    /**
     * Compares a whole model: all its elements, the section index included.
     */
    static void compareModel(RustModel model, boolean returnSourceInfo, PureModelContextData javaModel, RuntimeException javaError)
    {
        if (!available(MODEL_COUNTS))
        {
            return;
        }
        Input input = Input.of(model);
        byte[] json = RustModelResult.nativeParse(model);
        RustModelResult rust = RustModelResult.read(json, returnSourceInfo);
        if (rust.isUnsupported())
        {
            unsupported(MODEL_COUNTS, input, returnSourceInfo, rust.errorMessage, javaError);
            return;
        }
        JsonNode rustResult = rust.isError() ? null : RustSectionResult.toJson(rust.elements);
        if (javaError != null)
        {
            compareErrors(MODEL_COUNTS, input, returnSourceInfo, javaError, rust.errorKind, rust.errorMessage, rust.errorSourceInformation, rustResult);
            return;
        }
        JsonNode java = RustSectionResult.toJson(RustSectionResult.roundTrip(javaModel.getElements()));
        compareResults(MODEL_COUNTS, input, returnSourceInfo, java, rust.errorKind, rust.errorMessage, rustResult, wrap("elements", java), json);
    }

    /**
     * Whether the native library is available; when it isn't, the comparison is counted, and unexpected when the build
     * provides the library.
     */
    private static boolean available(Map<Outcome, AtomicLong> counts)
    {
        if (NativeGrammarParser.isAvailable())
        {
            return true;
        }
        counts.get(Outcome.UNAVAILABLE).incrementAndGet();
        if (Boolean.getBoolean(REQUIRED_PROPERTY))
        {
            recordUnexpected("unavailable", Outcome.UNAVAILABLE, "the Rust parser is not available: " + NativeGrammarParser.getLoadFailure(), null);
        }
        return false;
    }

    /**
     * An input the Rust parser reports unsupported: a grammar the library doesn't link, a result type this classpath
     * can't read, or anything else, by its hash.
     */
    private static void unsupported(Map<Outcome, AtomicLong> counts, Input input, boolean returnSourceInfo, String message, RuntimeException javaError)
    {
        Matcher unlinked = UNLINKED_GRAMMAR.matcher(message);
        if (unlinked.find())
        {
            String key = "unlinked:" + unlinked.group(1).replace(' ', '-') + ":" + unlinked.group(2);
            if (ACCEPTED.containsKey(key))
            {
                unlinked(counts, key);
            }
            else
            {
                differs(counts, Outcome.UNSUPPORTED, key, input, returnSourceInfo, message, null, null);
            }
            return;
        }
        if (message.startsWith(RustSectionResult.NO_PROTOCOL_EXTENSION) && javaError != null)
        {
            // Neither parser has the extension the input needs on this classpath.
            unlinked(counts, "unlinked:protocol:" + message.substring(RustSectionResult.NO_PROTOCOL_EXTENSION.length()));
            return;
        }
        differs(counts, Outcome.UNSUPPORTED, input.name, input, returnSourceInfo, message, null, null);
    }

    private static void unlinked(Map<Outcome, AtomicLong> counts, String key)
    {
        counts.get(Outcome.UNLINKED).incrementAndGet();
        UNLINKED.computeIfAbsent(key, k -> new AtomicLong()).incrementAndGet();
        if (ACCEPTED.containsKey(key))
        {
            ACCEPTED_HIT.add(key);
        }
    }

    /**
     * Compares the rendered results when Java parsed the input: Rust must have too ({@code rust} is null when it
     * reported an error), with the same result. When positions were asked for, Rust's JSON ({@code rustJson}) must
     * also be Java's ({@code javaJson}).
     */
    private static void compareResults(Map<Outcome, AtomicLong> counts, Input input, boolean returnSourceInfo, JsonNode java, String rustErrorKind, String rustErrorMessage, JsonNode rust, JsonNode javaJson, byte[] rustJson)
    {
        if (rust == null)
        {
            differs(counts, Outcome.MISMATCH, input.name, input, returnSourceInfo, "Java parsed the input; Rust reported " + rustErrorKind + ": " + rustErrorMessage, java, null);
            return;
        }
        if (!java.equals(rust))
        {
            differs(counts, Outcome.MISMATCH, input.name, input, returnSourceInfo, "results differ at " + firstDifference(java, rust, ""), java, rust);
            return;
        }
        if (returnSourceInfo)
        {
            JsonNode javaRaw = canonical(compactPositions(javaJson));
            JsonNode rustRaw = canonical(readTree(rustJson));
            if (!javaRaw.equals(rustRaw))
            {
                differs(counts, Outcome.RAW_MISMATCH, input.name, input, returnSourceInfo, "raw JSON differs at " + firstDifference(javaRaw, rustRaw, ""), javaRaw, rustRaw);
                return;
            }
        }
        counts.get(Outcome.MATCH).incrementAndGet();
    }

    /**
     * Compares the errors when Java rejected the input: Rust must report a parser error ({@code rust} is its result
     * when it parsed the input), at the same start position.
     */
    private static void compareErrors(Map<Outcome, AtomicLong> counts, Input input, boolean returnSourceInfo, RuntimeException javaError, String rustErrorKind, String rustErrorMessage, SourceInformation rustPosition, JsonNode rust)
    {
        EngineException engineException = EngineException.findException(javaError);
        String javaMessage = (engineException == null) ? String.valueOf(javaError.getMessage()) : engineException.getMessage();
        if (!RustSectionResult.PARSER_ERROR.equals(rustErrorKind))
        {
            String rustOutcome = (rust == null) ? (rustErrorKind + ": " + rustErrorMessage) : "parsed it";
            differs(counts, Outcome.MISMATCH, input.name, input, returnSourceInfo, "Java rejected the input (" + javaMessage + "); Rust " + rustOutcome, null, rust);
            return;
        }
        SourceInformation javaPosition = (engineException == null) ? null : engineException.getSourceInformation();
        if (returnSourceInfo && javaPosition != null && (rustPosition == null || javaPosition.startLine != rustPosition.startLine || javaPosition.startColumn != rustPosition.startColumn))
        {
            differs(counts, Outcome.MISMATCH, input.name, input, returnSourceInfo, "error position differs: Java " + position(javaPosition) + " (" + javaMessage + "), Rust " + position(rustPosition) + " (" + rustErrorMessage + ")", null, null);
            return;
        }
        counts.get(Outcome.ERROR_MATCH).incrementAndGet();
    }

    /**
     * A comparison that is not a match: {@link Outcome#ACCEPTED} when the accepted list names {@code key}, otherwise
     * {@code outcome}, unexpected and reported.
     */
    private static void differs(Map<Outcome, AtomicLong> counts, Outcome outcome, String key, Input input, boolean returnSourceInfo, String reason, JsonNode java, JsonNode rust)
    {
        if (ACCEPTED.containsKey(key))
        {
            counts.get(Outcome.ACCEPTED).incrementAndGet();
            ACCEPTED_HIT.add(key);
            return;
        }
        counts.get(outcome).incrementAndGet();
        recordUnexpected(key, outcome, reason, input);
        Path file = report((outcome == Outcome.UNSUPPORTED) ? "unsupported" : "mismatch", input, returnSourceInfo, reason, java, rust);
        if (outcome != Outcome.UNSUPPORTED && Boolean.getBoolean(FAIL_ON_MISMATCH_PROPERTY))
        {
            throw new AssertionError("Rust parser mismatch (returnSourceInfo=" + returnSourceInfo + "): " + reason
                    + ((file == null) ? "" : ("\nreport: " + file))
                    + "\n--- input ---\n" + input.code);
        }
    }

    private static void recordUnexpected(String key, Outcome outcome, String reason, Input input)
    {
        UNEXPECTED.putIfAbsent(key, new Unexpected(key, outcome, reason, (input == null) ? null : input.type, (input == null) ? null : input.sourceId));
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

    private static JsonNode wrap(String field, JsonNode value)
    {
        ObjectNode node = RustSectionResult.MAPPER.createObjectNode();
        node.set(field, value);
        return node;
    }

    private static JsonNode readTree(byte[] json)
    {
        try
        {
            return RustSectionResult.MAPPER.readTree(json);
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * {@code node} with every position object (exactly {@code sourceId}, {@code startLine}, {@code startColumn},
     * {@code endLine} and {@code endColumn}) in the compact form the Rust parser writes:
     * {@code sourceId:line:column-column}, or {@code sourceId:line:column-line:column} across lines.
     */
    static JsonNode compactPositions(JsonNode node)
    {
        if (node.isObject())
        {
            if (isPosition(node))
            {
                int startLine = node.get("startLine").asInt();
                int endLine = node.get("endLine").asInt();
                return RustSectionResult.MAPPER.getNodeFactory().textNode(node.get("sourceId").asText() + ":" + startLine + ":" + node.get("startColumn").asInt() + "-"
                        + ((startLine == endLine) ? "" : (endLine + ":")) + node.get("endColumn").asInt());
            }
            ObjectNode result = RustSectionResult.MAPPER.createObjectNode();
            node.fields().forEachRemaining(field -> result.set(field.getKey(), compactPositions(field.getValue())));
            return result;
        }
        if (node.isArray())
        {
            ArrayNode result = RustSectionResult.MAPPER.createArrayNode();
            node.forEach(item -> result.add(compactPositions(item)));
            return result;
        }
        return node;
    }

    private static boolean isPosition(JsonNode node)
    {
        return node.size() == 5 && node.path("sourceId").isTextual() && node.path("startLine").isIntegralNumber() && node.path("startColumn").isIntegralNumber()
                && node.path("endLine").isIntegralNumber() && node.path("endColumn").isIntegralNumber();
    }

    /**
     * {@code node} without nulls and empty arrays, which the engine reads back as absent, and with every number as
     * its value alone: the engine's mapper and the Rust parser may write the same number differently, and a decimal's
     * scale is compared with the protocol objects.
     */
    static JsonNode canonical(JsonNode node)
    {
        if (node.isObject())
        {
            ObjectNode result = RustSectionResult.MAPPER.createObjectNode();
            node.fields().forEachRemaining(field ->
            {
                JsonNode value = canonical(field.getValue());
                if (!value.isNull() && !(value.isArray() && value.size() == 0))
                {
                    result.set(field.getKey(), value);
                }
            });
            return result;
        }
        if (node.isArray())
        {
            ArrayNode result = RustSectionResult.MAPPER.createArrayNode();
            node.forEach(item -> result.add(canonical(item)));
            return result;
        }
        if (node.isNumber() && !(node.isDouble() && (Double.isNaN(node.doubleValue()) || Double.isInfinite(node.doubleValue()))))
        {
            BigDecimal value = node.decimalValue();
            return RustSectionResult.MAPPER.getNodeFactory().numberNode((value.signum() == 0) ? BigDecimal.ZERO : value.stripTrailingZeros());
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
     * The accepted list: {@value #ACCEPTED_PROPERTY}'s file, or the module's own.
     */
    private static Map<String, String> loadAccepted()
    {
        String file = System.getProperty(ACCEPTED_PROPERTY);
        try (InputStream in = (file == null || file.isEmpty()) ? RustGrammarParity.class.getClassLoader().getResourceAsStream(ACCEPTED_RESOURCE) : Files.newInputStream(Paths.get(file)))
        {
            return (in == null) ? Collections.emptyMap() : parseAccepted(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("cannot read the Rust parser's accepted list", e);
        }
    }

    /**
     * Each line's key and the rest, its category and reason; blank lines and {@code #} comments skipped.
     */
    static Map<String, String> parseAccepted(BufferedReader reader) throws IOException
    {
        Map<String, String> accepted = new LinkedHashMap<>();
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            String text = line.trim();
            if (text.isEmpty() || text.startsWith("#"))
            {
                continue;
            }
            String[] keyAndReason = text.split("\\s+", 2);
            accepted.put(keyAndReason[0], (keyAndReason.length > 1) ? keyAndReason[1] : "");
        }
        return accepted;
    }

    private static void summarize()
    {
        if (COUNTS.values().stream().mapToLong(AtomicLong::get).sum() > 0)
        {
            System.err.println("[rust-parser parity] " + counts());
        }
        if (MODEL_COUNTS.values().stream().mapToLong(AtomicLong::get).sum() > 0)
        {
            System.err.println("[rust-parser parity: models] " + modelCounts());
        }
        List<Unexpected> unexpected = unexpected();
        if (!unexpected.isEmpty())
        {
            System.err.println("[rust-parser parity] " + unexpected.size() + " unexpected: " + unexpected);
        }
        String dir = System.getProperty(REPORT_DIR_PROPERTY);
        if (dir != null && !dir.isEmpty())
        {
            writeSummary(Paths.get(dir, "summary", UUID.randomUUID() + ".json"), unexpected);
        }
    }

    private static void writeSummary(Path file, List<Unexpected> unexpected)
    {
        ObjectNode summary = RustSectionResult.MAPPER.createObjectNode();
        summary.set("counts", RustSectionResult.MAPPER.valueToTree(counts()));
        summary.set("modelCounts", RustSectionResult.MAPPER.valueToTree(modelCounts()));
        ArrayNode entries = summary.putArray("unexpected");
        unexpected.forEach(entry -> entries.add(RustSectionResult.MAPPER.createObjectNode()
                .put("key", entry.key)
                .put("outcome", entry.outcome.name())
                .put("reason", entry.reason)
                .put("input", entry.input)
                .put("sourceId", entry.sourceId)));
        ArrayNode hit = summary.putArray("acceptedHit");
        new TreeSet<>(ACCEPTED_HIT).forEach(hit::add);
        Map<String, Long> unlinked = new TreeMap<>();
        UNLINKED.forEach((key, count) -> unlinked.put(key, count.get()));
        summary.set("unlinked", RustSectionResult.MAPPER.valueToTree(unlinked));
        try
        {
            Files.createDirectories(file.getParent());
            Files.write(file, RustSectionResult.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(summary));
        }
        catch (IOException e)
        {
            System.err.println("[rust-parser parity] cannot write the summary " + file + ": " + e);
        }
    }

    /**
     * An unexpected comparison: its key (an input's hash, or {@code unlinked:<kind>:<type>}), outcome and reason, and
     * the input that first showed it.
     */
    public static final class Unexpected
    {
        public final String key;
        public final Outcome outcome;
        public final String reason;
        public final String input;
        public final String sourceId;

        Unexpected(String key, Outcome outcome, String reason, String input, String sourceId)
        {
            this.key = key;
            this.outcome = outcome;
            this.reason = reason;
            this.input = input;
            this.sourceId = sourceId;
        }

        @Override
        public String toString()
        {
            return this.key + " " + this.outcome + " (" + this.input + ", " + this.sourceId + "): " + this.reason;
        }
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

        static Input of(RustModel model)
        {
            return new Input("model-" + model.hash(), "model", model.sourceId, model.lineOffset, model.code);
        }
    }
}
