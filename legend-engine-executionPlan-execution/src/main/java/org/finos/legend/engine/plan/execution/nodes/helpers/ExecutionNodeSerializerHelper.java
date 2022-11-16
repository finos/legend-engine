// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.nodes.helpers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.eclipse.collections.impl.block.factory.Functions;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.dependencies.store.platform.IGraphSerializer;
import org.finos.legend.engine.plan.dependencies.store.platform.IPlatformPureExpressionExecutionNodeSerializeSpecifics;
import org.finos.legend.engine.plan.dependencies.store.platform.ISerializationWriter;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.object.StreamingObjectResult;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.SerializationConfig;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ExecutionNodeSerializerHelper
{
    private static IExecutionNodeContext context;

    public static <T> Result executeSerialize(IPlatformPureExpressionExecutionNodeSerializeSpecifics specifics, SerializationConfig config, Result childResult, IExecutionNodeContext context)
    {
        ExecutionNodeSerializerHelper.context = context;
        Stream<T> stream;
        if (childResult instanceof ConstantResult)
        {
            Object value = ((ConstantResult) childResult).getValue();
            if (value instanceof List)
            {
                value = ((List) value).stream();
            }
            if (!(value instanceof Stream))
            {
                value = Stream.of(value);
            }
            stream = (Stream<T>) value;
        }
        else if (childResult instanceof StreamingObjectResult)
        {
            stream = ((StreamingObjectResult) childResult).getObjectStream();
        }
        else
        {
            throw new IllegalArgumentException("Unexpected result: " + childResult.getClass().getName());
        }

        return new JsonStreamingResult(new Serializer<>(stream, specifics, config), childResult);
    }

    private static class Serializer<T> implements JsonStreamingResult.JsonStreamHandler
    {
        private final Stream<T> stream;
        private final IPlatformPureExpressionExecutionNodeSerializeSpecifics specifics;
        private final SerializationConfig config;

        Serializer(Stream<T> stream, IPlatformPureExpressionExecutionNodeSerializeSpecifics specifics, SerializationConfig config)
        {
            this.stream = stream;
            this.specifics = specifics;
            this.config = config;
        }

        private IGraphSerializer<T> createSerializer(JsonGenerator generator)
        {
            return (IGraphSerializer<T>) specifics.serializer(new Writer(generator, this.config), context);
        }

        @Override
        public Stream<ObjectNode> toStream()
        {
            TokenBuffer tokenBuffer = new TokenBuffer(ObjectMapperFactory.getNewStandardObjectMapper(), true);
            JsonParser jsonParser = tokenBuffer.asParser();
            IGraphSerializer<T> serializer = this.createSerializer(tokenBuffer);
            return this.stream.map(Functions.throwing(x ->
            {
                serializer.serialize(x);
                return jsonParser.readValueAs(ObjectNode.class);
            }));
        }

        @Override
        public void writeTo(JsonGenerator generator)
        {
            IGraphSerializer<T> serializer = this.createSerializer(generator);
            try
            {
                Iterator<T> iter = this.stream.iterator();
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
            finally
            {
                // TODO this.executionContext.getChildResult().close();
            }
        }
    }

    private static class Writer implements ISerializationWriter
    {
        public static final String PURE_ANY = "meta::pure::metamodel::type::Any";
        private final JsonGenerator generator;
        private final Consumer<String> typeWriter;
        private final boolean includeEnumType;
        private final boolean removePropertiesWithEmptySets;
        private final boolean removePropertiesWithNullValues;
        private final boolean includeObjectReference;
        private final Stack<Integer> objectsToCloseOnEnd = new Stack<>();

        Writer(JsonGenerator generator, SerializationConfig config)
        {
            this.generator = generator;
            this.includeEnumType = config != null && config.includeEnumType;
            this.removePropertiesWithEmptySets = config != null && config.removePropertiesWithEmptySets;
            this.removePropertiesWithNullValues = config != null && config.removePropertiesWithNullValues;
            this.includeObjectReference = config != null && config.includeObjectReference;

            if (config == null || !config.includeType)
            {
                typeWriter = s ->
                {
                };
            }
            else
            {
                String typeProperty = config.typeKeyName == null ? "@type" : config.typeKeyName;
                if (config.fullyQualifiedTypePath)
                {
                    typeWriter = s ->
                    {
                        if (!s.equals(PURE_ANY))
                        {
                            writeFieldName(typeProperty);
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
                            writeFieldName(typeProperty);
                            int pos = s.lastIndexOf("::");
                            writeString(pos == -1 ? s : s.substring(pos + 2));
                        }
                    };
                }
            }
        }

        @Override
        public void startObject(String typePath)
        {
            writeStartObject();
            this.typeWriter.accept(typePath);
            this.objectsToCloseOnEnd.push(1);
        }

        @Override
        public void startObject(String typePath, String objectRef)
        {
            if (includeObjectReference)
            {
                writeStartObject();
                writeStringProperty("objectReference", objectRef);
                writeFieldName("value");
                writeStartObject();
                this.typeWriter.accept(typePath);
                this.objectsToCloseOnEnd.push(2);
            }
            else
            {
                writeStartObject();
                this.typeWriter.accept(typePath);
                this.objectsToCloseOnEnd.push(1);
            }
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
                writeString(value.toString());
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
                values.forEach(v -> writeString(v.toString()));
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
