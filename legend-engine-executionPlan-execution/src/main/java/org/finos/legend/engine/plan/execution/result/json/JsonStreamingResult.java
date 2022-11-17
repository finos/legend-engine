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

package org.finos.legend.engine.plan.execution.result.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class JsonStreamingResult extends StreamingResult
{
    private final JsonStreamHandler jsonStream;
    private final Result childResult;
    private final Builder builder;

    @Deprecated
    public JsonStreamingResult(JsonStreamHandler jsonStream)
    {
        this(jsonStream, null);
    }

    public JsonStreamingResult(JsonStreamHandler jsonStream, Result result)
    {
        super(Lists.mutable.empty());
        this.childResult = result;
        this.jsonStream = jsonStream;
        this.builder = new Builder("json");
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    @Override
    public Builder getResultBuilder()
    {
        return this.builder;
    }

    public Consumer<JsonGenerator> getJsonStream()
    {
        return jsonStream::writeTo;
    }

    @Override
    public void close()
    {
        if (this.childResult != null)
        {
            this.childResult.close();
        }
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        switch (format)
        {
            case PURE:
            case RAW:
                return new JsonStreamToPureFormatSerializer(this);
            case DEFAULT:
                return new JsonStreamToJsonDefaultSerializer(this);
            default:
                this.close();
                throw new RuntimeException(format + " format not currently supported with JsonStreamingResult");
        }
    }

    public Stream<ObjectNode> toStream()
    {
        return this.jsonStream.toStream().onClose(this::close);
    }

    public interface JsonStreamHandler
    {
        void writeTo(JsonGenerator generator);

        Stream<ObjectNode> toStream();
    }
}
