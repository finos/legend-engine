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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.PureProtocolObjectMapperFactory;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The Rust parser's result for one section, read from the JSON {@link NativeGrammarParser#parseSection} returns and
 * deserialized into protocol objects.
 */
final class RustSectionResult
{
    /**
     * Reads and writes protocol JSON the way the engine does (protocol subtypes registered, properties sorted, nulls
     * omitted), but reads decimals exactly, digits and scale, as the Java parser builds them: the standard mapper reads
     * them through doubles ({@code 30000.00} becomes {@code 30000.0}). Other floats, in untyped ({@code Object}) or
     * {@code Number} fields such as a relational literal's value, are read as the standard mapper reads them, as
     * {@code Double}s: the Java parser builds doubles there, and reading every float exactly would make them
     * {@code BigDecimal}s.
     */
    static final ObjectMapper MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .setNodeFactory(JsonNodeFactory.withExactBigDecimals(true))
            .registerModule(new SimpleModule("rustParserFloats")
                    .addDeserializer(Object.class, new UntypedFloatsAsDoubles())
                    .addDeserializer(Number.class, new NumberFloatsAsDoubles()));

    /**
     * The class instance types the protocol extensions on this classpath map to protocol classes.
     */
    private static final Set<String> CLASS_INSTANCE_TYPES = PureProtocolObjectMapperFactory.getClassInstanceTypeMappings().keySet();

    static final String PARSER_ERROR = "parser";
    static final String UNSUPPORTED = "unsupported";
    static final String INTERNAL_ERROR = "internal";

    final List<PackageableElement> elements;
    /**
     * The section's imports, or null when its grammar has none: the section is then a {@link DefaultCodeSection},
     * not an {@link ImportAwareCodeSection}.
     */
    final List<String> imports;
    final String errorKind;
    final String errorMessage;
    final SourceInformation errorSourceInformation;

    private RustSectionResult(List<PackageableElement> elements, List<String> imports, String errorKind, String errorMessage, SourceInformation errorSourceInformation)
    {
        this.elements = elements;
        this.imports = imports;
        this.errorKind = errorKind;
        this.errorMessage = errorMessage;
        this.errorSourceInformation = errorSourceInformation;
    }

    boolean isError()
    {
        return this.errorKind != null;
    }

    boolean isUnsupported()
    {
        return UNSUPPORTED.equals(this.errorKind);
    }

    /**
     * Parses {@code code} with the Rust parser, using the section's walker source information for positions.
     */
    static RustSectionResult parse(SectionSourceCode code, boolean returnSourceInfo)
    {
        ParseTreeWalkerSourceInformation walker = code.walkerSourceInformation;
        String json = NativeGrammarParser.parseSection(code.sectionType, code.code, walker.getSourceId(), walker.getLineOffset(), walker.getColumnOffset(), returnSourceInfo);
        if (json == null)
        {
            return internalError("the Rust parser returned no result");
        }
        try
        {
            JsonNode root = MAPPER.readTree(json);
            JsonNode error = root.get("error");
            if (error != null)
            {
                JsonNode si = error.get("sourceInformation");
                return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), error.path("kind").asText(INTERNAL_ERROR), error.path("message").asText(""), (si == null) ? null : MAPPER.treeToValue(si, SourceInformation.class));
            }
            String absentType = absentClassInstanceType(root.path("elements"));
            if (absentType != null)
            {
                // An island whose engine extension is not on this classpath: the Java parser reports that.
                return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), UNSUPPORTED, "no protocol extension for class instance type '" + absentType + "'", null);
            }
            List<PackageableElement> elements = new ArrayList<>();
            for (JsonNode element : root.path("elements"))
            {
                elements.add(MAPPER.treeToValue(element, PackageableElement.class));
            }
            List<String> imports = null;
            if (root.has("imports"))
            {
                imports = new ArrayList<>();
                for (JsonNode i : root.get("imports"))
                {
                    imports.add(i.asText());
                }
            }
            return new RustSectionResult(elements, imports, null, null, null);
        }
        catch (InvalidTypeIdException e)
        {
            // A type an engine extension defines, such as embedded data, and that extension is not on this classpath.
            return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), UNSUPPORTED, "no protocol extension for type '" + e.getTypeId() + "'", null);
        }
        catch (JsonProcessingException e)
        {
            // The Rust JSON does not deserialize into the engine's protocol classes: a protocol divergence.
            return internalError("Rust protocol JSON does not match the engine protocol: " + e.getOriginalMessage());
        }
    }

    /**
     * The first class instance type in {@code node} that no protocol extension on this classpath maps, if any. Islands
     * such as {@code #TDS{…}#} belong to engine extensions: the Rust library may parse one this engine can't.
     */
    static String absentClassInstanceType(JsonNode node)
    {
        if ("classInstance".equals(node.path("_type").asText()) && !CLASS_INSTANCE_TYPES.contains(node.path("type").asText()))
        {
            return node.path("type").asText();
        }
        for (JsonNode child : node)
        {
            String type = absentClassInstanceType(child);
            if (type != null)
            {
                return type;
            }
        }
        return null;
    }

    /**
     * The imports of a section the Java parser returned, or null when it is not an {@link ImportAwareCodeSection}.
     */
    static List<String> imports(Section section)
    {
        return (section instanceof ImportAwareCodeSection) ? ((ImportAwareCodeSection) section).imports : null;
    }

    /**
     * The elements after a round trip through the protocol mapper. Deserialization applies the engine's protocol
     * converters (e.g. {@code new} of {@code BasicColumnSpecification} becomes {@code col}); the Rust result always
     * goes through it, so the Java parser's result must too before the two are compared or recorded. The trip goes
     * through JSON text, as protocol travels: a JSON tree would drop trailing zeros from decimals.
     */
    static List<PackageableElement> roundTrip(List<PackageableElement> elements)
    {
        List<PackageableElement> result = new ArrayList<>(elements.size());
        try
        {
            for (PackageableElement element : elements)
            {
                result.add(MAPPER.readValue(MAPPER.writeValueAsString(element), PackageableElement.class));
            }
        }
        catch (JsonProcessingException e)
        {
            throw new IllegalStateException("the engine protocol does not round-trip its own element", e);
        }
        return result;
    }

    /**
     * The elements as protocol JSON, each with its {@code _type}. Serializing the list itself would drop the type ids:
     * Jackson can't see the element type of a root-level list.
     */
    static ArrayNode toJson(List<PackageableElement> elements)
    {
        ArrayNode array = MAPPER.createArrayNode();
        for (PackageableElement element : elements)
        {
            array.add(MAPPER.<JsonNode>valueToTree(element));
        }
        return array;
    }

    /**
     * Untyped values, with floats as doubles, in nested maps and lists too.
     */
    private static final class UntypedFloatsAsDoubles extends UntypedObjectDeserializer
    {
        private UntypedFloatsAsDoubles()
        {
            super(null, null);
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException
        {
            return parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT) ? (Object) parser.getDoubleValue() : super.deserialize(parser, context);
        }
    }

    /**
     * {@code Number} values, with floats as doubles.
     */
    private static final class NumberFloatsAsDoubles extends StdDeserializer<Number>
    {
        private NumberFloatsAsDoubles()
        {
            super(Number.class);
        }

        @Override
        public Number deserialize(JsonParser parser, DeserializationContext context) throws IOException
        {
            return parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT) ? (Number) parser.getDoubleValue() : (Number) NumberDeserializers.NumberDeserializer.instance.deserialize(parser, context);
        }
    }

    private static RustSectionResult internalError(String message)
    {
        return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), INTERNAL_ERROR, message, null);
    }
}
