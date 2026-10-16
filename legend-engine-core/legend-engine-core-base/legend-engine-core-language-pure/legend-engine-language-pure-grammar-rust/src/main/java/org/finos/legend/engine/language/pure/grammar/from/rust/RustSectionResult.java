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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.UnknownClassInstanceTypeException;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.rust.parser.NativeGrammarParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The Rust parser's result for one section, read from the JSON {@link NativeGrammarParser#parseSection} returns and
 * deserialized into protocol objects. The JSON, UTF-8 bytes, is read as a stream: each element goes straight into its
 * protocol object, with no JSON tree in between.
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
     * {@link #MAPPER}, reading every source information as null: the Rust parser always reports positions, and the
     * Java parser builds none when its caller asks for none.
     */
    static final ObjectMapper MAPPER_WITHOUT_SOURCE_INFORMATION = MAPPER.copy()
            .registerModule(new SimpleModule("rustParserWithoutSourceInformation")
                    .addDeserializer(SourceInformation.class, new SourceInformationAsNull()));

    private static final TypeReference<List<String>> STRINGS = new TypeReference<List<String>>()
    {
    };

    static final String PARSER_ERROR = "parser";
    static final String UNSUPPORTED = "unsupported";
    static final String INTERNAL_ERROR = "internal";

    /**
     * How an unsupported result whose type no protocol extension on this classpath defines begins.
     */
    static final String NO_PROTOCOL_EXTENSION = "no protocol extension for ";

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
        return read(nativeParse(code), returnSourceInfo);
    }

    /**
     * The JSON {@link NativeGrammarParser#parseSection} returns for {@code code}.
     */
    static byte[] nativeParse(SectionSourceCode code)
    {
        ParseTreeWalkerSourceInformation walker = code.walkerSourceInformation;
        return NativeGrammarParser.parseSection(code.sectionType, code.code, walker.getSourceId(), walker.getLineOffset(), walker.getColumnOffset());
    }

    /**
     * Reads the JSON {@link NativeGrammarParser#parseSection} returns.
     */
    static RustSectionResult read(byte[] json, boolean returnSourceInfo)
    {
        if (json == null)
        {
            return internalError("the Rust parser returned no result");
        }
        ObjectMapper mapper = mapper(returnSourceInfo);
        try (JsonParser parser = mapper.getFactory().createParser(json))
        {
            List<PackageableElement> elements = new ArrayList<>();
            List<String> imports = null;
            for (String field = firstField(parser); field != null; field = nextField(parser))
            {
                if ("error".equals(field))
                {
                    RustError error = RustError.read(parser);
                    return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), error.kind, error.message, error.sourceInformation);
                }
                if ("elements".equals(field))
                {
                    readElements(parser, mapper, elements);
                }
                else if ("imports".equals(field))
                {
                    imports = mapper.readValue(parser, STRINGS);
                }
                else
                {
                    parser.skipChildren();
                }
            }
            return new RustSectionResult(elements, imports, null, null, null);
        }
        catch (UnknownClassInstanceTypeException e)
        {
            // An island whose engine extension is not on this classpath: the Java parser reports that.
            return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), UNSUPPORTED, unsupported(e), null);
        }
        catch (InvalidTypeIdException e)
        {
            // A type an engine extension defines, such as embedded data, and that extension is not on this classpath.
            return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), UNSUPPORTED, unsupported(e), null);
        }
        catch (IOException e)
        {
            // The Rust JSON does not deserialize into the engine's protocol classes: a protocol divergence.
            return internalError(divergence(e));
        }
    }

    /**
     * The mapper reading the Rust parser's JSON: with or without the source information the Rust parser always reports.
     */
    static ObjectMapper mapper(boolean returnSourceInfo)
    {
        return returnSourceInfo ? MAPPER : MAPPER_WITHOUT_SOURCE_INFORMATION;
    }

    /**
     * The first field of the JSON object {@code parser} reads, which is then on the field's value; null for an empty
     * object.
     */
    static String firstField(JsonParser parser) throws IOException
    {
        if (parser.nextToken() != JsonToken.START_OBJECT)
        {
            throw new JsonParseException(parser, "the Rust parser's result is not a JSON object");
        }
        return nextField(parser);
    }

    /**
     * The next field of the JSON object {@code parser} reads, which is then on the field's value; null at the object's
     * end.
     */
    static String nextField(JsonParser parser) throws IOException
    {
        if (parser.nextToken() != JsonToken.FIELD_NAME)
        {
            return null;
        }
        String field = parser.currentName();
        parser.nextToken();
        return field;
    }

    /**
     * Reads the array of elements {@code parser} is on, each straight into its protocol object.
     */
    static void readElements(JsonParser parser, ObjectMapper mapper, List<PackageableElement> elements) throws IOException
    {
        while (parser.nextToken() == JsonToken.START_OBJECT)
        {
            elements.add(readValue(parser, mapper, PackageableElement.class));
        }
    }

    /**
     * Reads the value {@code parser} is on. The engine's reader fails on a class instance of a type no protocol extension
     * on this classpath maps with an {@link UnknownClassInstanceTypeException}; Jackson wraps it when the class
     * instance is within an object, so it is rethrown as it is, however deep it was.
     */
    static <T> T readValue(JsonParser parser, ObjectMapper mapper, Class<T> type) throws IOException
    {
        try
        {
            return mapper.readValue(parser, type);
        }
        catch (JsonMappingException e)
        {
            if (e.getCause() instanceof UnknownClassInstanceTypeException)
            {
                throw (UnknownClassInstanceTypeException) e.getCause();
            }
            throw e;
        }
    }

    static String unsupported(InvalidTypeIdException e)
    {
        return NO_PROTOCOL_EXTENSION + "type '" + e.getTypeId() + "'";
    }

    static String unsupported(UnknownClassInstanceTypeException e)
    {
        return NO_PROTOCOL_EXTENSION + "class instance type '" + e.getType() + "'";
    }

    static String divergence(IOException e)
    {
        return "Rust protocol JSON does not match the engine protocol: " + ((e instanceof JsonProcessingException) ? ((JsonProcessingException) e).getOriginalMessage() : e.getMessage());
    }

    /**
     * An error the Rust parser reports: {@code {"kind","message","sourceInformation"?}}.
     */
    static final class RustError
    {
        final String kind;
        final String message;
        final SourceInformation sourceInformation;

        private RustError(String kind, String message, SourceInformation sourceInformation)
        {
            this.kind = kind;
            this.message = message;
            this.sourceInformation = sourceInformation;
        }

        /**
         * Reads the error object {@code parser} is on, keeping its position whatever the caller asked for.
         */
        static RustError read(JsonParser parser) throws IOException
        {
            JsonNode error = MAPPER.readTree(parser);
            JsonNode si = error.get("sourceInformation");
            return new RustError(error.path("kind").asText(INTERNAL_ERROR), error.path("message").asText(""), (si == null) ? null : MAPPER.treeToValue(si, SourceInformation.class));
        }
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

    /**
     * Source information, skipped and read as null.
     */
    private static final class SourceInformationAsNull extends StdDeserializer<SourceInformation>
    {
        private SourceInformationAsNull()
        {
            super(SourceInformation.class);
        }

        @Override
        public SourceInformation deserialize(JsonParser parser, DeserializationContext context) throws IOException
        {
            parser.skipChildren();
            return null;
        }
    }

    private static RustSectionResult internalError(String message)
    {
        return new RustSectionResult(Collections.emptyList(), Collections.emptyList(), INTERNAL_ERROR, message, null);
    }
}
