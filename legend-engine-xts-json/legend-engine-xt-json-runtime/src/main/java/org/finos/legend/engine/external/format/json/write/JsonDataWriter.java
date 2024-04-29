//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json.write;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatWriter;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class JsonDataWriter<T> extends ExternalFormatWriter
{
    private final IJsonExternalizeExecutionNodeSpecifics specifics;
    private final Stream<T> inputStream;
    private final IExecutionNodeContext context;

    public JsonDataWriter(IJsonExternalizeExecutionNodeSpecifics specifics, Stream<T> inputStream, IExecutionNodeContext context)
    {
        this.specifics = specifics;
        this.inputStream = inputStream;
        this.context = context;
    }

    @Override
    public void writeData(OutputStream stream) throws IOException
    {
        try (JsonGenerator generator = this.createGenerator(stream))
        {
            IJsonSerializer<T> serializer = this.createSerializer(generator);
            try
            {
                Iterator<T> iter = this.inputStream.iterator();
                if (iter.hasNext())
                {
                    T first = iter.next();
                    if (iter.hasNext())
                    {
                        generator.writeStartArray();
                    }
                    serializer.serialize(first);
                    if (iter.hasNext())
                    {
                        iter.forEachRemaining(serializer::serialize);
                        generator.writeEndArray();
                    }
                }
                else
                {
                    generator.writeStartArray();
                    generator.writeEndArray();
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private IJsonSerializer<T> createSerializer(JsonGenerator generator)
    {
        return this.specifics.createSerializer(new JsonWriter(generator), this.context);
    }

    private JsonGenerator createGenerator(OutputStream stream) throws IOException
    {
        return new JsonFactory().createGenerator(stream)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT)
                .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN)
                .setCodec(new ObjectMapper());
    }

    private static class JsonWriter implements IJsonWriter
    {
        private final JsonGenerator generator;

        private final Stack<Integer> objectsToCloseOnEnd = new Stack<>();

        public static final String PURE_ANY = "meta::pure::metamodel::type::Any";
        private Consumer<String> typeWriter = s ->
        {
        };
        private boolean includeEnumType = false;
        private String dateTimeFormat;
        private boolean removePropertiesWithEmptySets = false;
        private boolean removePropertiesWithNullValues = false;

        public JsonWriter(JsonGenerator generator)
        {
            this.generator = generator;
        }

        public void setTypeWriter(boolean includeType, String typeKeyName, boolean fullyQualifiedTypePath)
        {
            if (includeType)
            {
                if (fullyQualifiedTypePath)
                {
                    typeWriter = s ->
                    {
                        if (!s.equals(PURE_ANY))
                        {
                            writeFieldName(typeKeyName);
                            writeString(s);
                        }
                    };
                }
                else
                {
                    typeWriter = s ->
                    {
                        if (!s.equals(PURE_ANY))
                        {
                            writeFieldName(typeKeyName);
                            int pos = s.lastIndexOf("::");
                            writeString(pos == -1 ? s : s.substring(pos + 2));
                        }
                    };
                }
            }
        }

        public void setIncludeEnumType(boolean includeEnumType)
        {
            this.includeEnumType = includeEnumType;
        }

        public void setDateTimeFormat(String dateTimeFormat)
        {
            this.dateTimeFormat = dateTimeFormat;
        }

        public void setRemovePropertiesWithEmptySets(boolean removePropertiesWithEmptySets)
        {
            this.removePropertiesWithEmptySets = removePropertiesWithEmptySets;
        }

        public void setRemovePropertiesWithNullValues(boolean removePropertiesWithNullValues)
        {
            this.removePropertiesWithNullValues = removePropertiesWithNullValues;
        }

        @Override
        public void startObject(String typePath)
        {
            writeStartObject();
            this.typeWriter.accept(typePath);
            this.objectsToCloseOnEnd.push(1);
        }

        @Override
        public void endObject()
        {
            for (int i = this.objectsToCloseOnEnd.pop(); i > 0; i--)
            {
                writeEndObject();
            }
        }

        @Override
        public void writeBooleanProperty(String name, boolean value)
        {
            writeFieldName(name);
            writeBoolean(value);
        }

        @Override
        public void writeBooleanProperty(String name, Boolean value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeBoolean(value);
            }
        }

        @Override
        public void writeBooleanProperty(String name, List<Boolean> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeBoolean);
                writeEndArray();
            }
        }

        @Override
        public void writeIntegerProperty(String name, long value)
        {
            writeFieldName(name);
            writeNumber(value);
        }

        @Override
        public void writeIntegerProperty(String name, Long value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeNumber(value);
            }
        }

        @Override
        public void writeIntegerProperty(String name, List<Long> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeNumber);
                writeEndArray();
            }
        }

        @Override
        public void writeFloatProperty(String name, double value)
        {
            writeFieldName(name);
            writeNumber(value);
        }

        @Override
        public void writeFloatProperty(String name, Double value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeNumber(value);
            }
        }

        @Override
        public void writeFloatProperty(String name, List<Double> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeNumber);
                writeEndArray();
            }
        }

        @Override
        public void writeDecimalProperty(String name, BigDecimal value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeNumber(value);
            }
        }

        @Override
        public void writeDecimalProperty(String name, List<BigDecimal> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeNumber);
                writeEndArray();
            }
        }

        @Override
        public void writeNumberProperty(String name, Number value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeNumber(value);
            }
        }

        @Override
        public void writeNumberProperty(String name, List<Number> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeNumber);
                writeEndArray();
            }
        }

        @Override
        public void writeStringProperty(String name, String value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeString(value);
            }
        }

        @Override
        public void writeStringProperty(String name, List<String> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(this::writeString);
                writeEndArray();
            }
        }

        @Override
        public void writeStrictDateProperty(String name, PureDate value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeString(value.toString());
            }
        }

        @Override
        public void writeStrictDateProperty(String name, List<PureDate> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(v -> writeString(v.toString()));
                writeEndArray();
            }
        }

        @Override
        public void writeDateTimeProperty(String name, PureDate value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                if (this.dateTimeFormat != null)
                {
                    writeString(value.format(this.dateTimeFormat));
                }
                else
                {
                    writeString(value.toString());
                }
            }
        }

        @Override
        public void writeDateTimeProperty(String name, List<PureDate> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                if (this.dateTimeFormat != null)
                {
                    values.forEach(v -> writeString(v.format(this.dateTimeFormat)));
                }
                else
                {
                    values.forEach(v -> writeString(v.toString()));
                }
                writeEndArray();
            }
        }

        @Override
        public void writeDateProperty(String name, PureDate value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeString(value.toString());
            }
        }

        @Override
        public void writeDateProperty(String name, List<PureDate> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(v -> writeString(v.toString()));
                writeEndArray();
            }
        }

        @Override
        public void writeEnumProperty(String name, String path, String value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeEnum(path, value);
            }
        }

        @Override
        public void writeEnumProperty(String name, String path, List<String> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(s -> writeEnum(path, s));
                writeEndArray();
            }
        }

        @Override
        public void writeUnitProperty(String name, String path, Number value)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeUnit(path, value);
            }
        }

        @Override
        public void writeUnitProperty(String name, String path, List<Number> values)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(n -> writeUnit(path, n));
                writeEndArray();
            }
        }

        @Override
        public <T> void writeComplexProperty(String name, T value, Consumer<T> writeOne)
        {
            if (value == null)
            {
                writeEmptyPropertyZeroOne(name);
            }
            else
            {
                writeFieldName(name);
                writeOne.accept(value);
            }
        }

        @Override
        public <T> void writeComplexProperty(String name, List<T> values, Consumer<T> writeOne)
        {
            if (values == null || values.isEmpty())
            {
                writeEmptyPropertyMany(name);
            }
            else
            {
                writeFieldName(name);
                writeStartArray();
                values.forEach(writeOne);
                writeEndArray();
            }
        }

        private void writeEmptyPropertyZeroOne(String name)
        {
            if (!this.removePropertiesWithNullValues)
            {
                writeFieldName(name);
                writeNull();
            }
        }

        private void writeEmptyPropertyMany(String name)
        {
            if (!this.removePropertiesWithEmptySets)
            {
                writeFieldName(name);
                writeStartArray();
                writeEndArray();
            }
        }

        private void writeStartObject()
        {
            try
            {
                generator.writeStartObject();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeEndObject()
        {
            try
            {
                generator.writeEndObject();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeStartArray()
        {
            try
            {
                generator.writeStartArray();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeEndArray()
        {
            try
            {
                generator.writeEndArray();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeFieldName(String name)
        {
            try
            {
                generator.writeFieldName(name);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeNumber(long value)
        {
            try
            {
                generator.writeNumber(value);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeNumber(double value)
        {
            try
            {
                generator.writeNumber(value);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeNumber(BigDecimal value)
        {
            try
            {
                generator.writeNumber(value);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeNumber(Number value)
        {
            if (value instanceof Long)
            {
                writeNumber(value.longValue());
            }
            else if (value instanceof Double)
            {
                writeNumber(value.doubleValue());
            }
            else if (value instanceof BigDecimal)
            {
                writeNumber((BigDecimal) value);
            }
            else
            {
                throw new IllegalArgumentException("Unhandled number type: " + value.getClass().getSimpleName());
            }
        }

        private void writeBoolean(boolean value)
        {
            try
            {
                generator.writeBoolean(value);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeEnum(String path, String value)
        {
            if (this.includeEnumType)
            {
                writeString((path == null || path.isEmpty() ? "" : path + ".") + value);
            }
            else
            {
                writeString(value);
            }
        }

        private void writeUnit(String path, Number value)
        {
            writeStartObject();

            writeFieldName("unit");
            writeStartArray();

            writeStartObject();
            writeFieldName("unitId");
            writeString(path);
            writeFieldName("exponentValue");
            writeNumber(1L);
            writeEndObject();

            writeEndArray();

            writeFieldName("value");
            writeNumber(value);

            writeEndObject();
        }

        private void writeString(String value)
        {
            try
            {
                generator.writeString(value);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }

        private void writeNull()
        {
            try
            {
                generator.writeNull();
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
    }
}
