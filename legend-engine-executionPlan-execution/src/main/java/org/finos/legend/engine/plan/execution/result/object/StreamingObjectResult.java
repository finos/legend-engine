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

package org.finos.legend.engine.plan.execution.result.object;

import org.finos.legend.engine.plan.dependencies.store.shared.IResult;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.util.stream.Stream;

public class StreamingObjectResult<T> extends StreamingResult
{
    private Stream<T> objectStream;
    private final Builder resultBuilder;
    private final Result childResult;

    public StreamingObjectResult(Stream<T> objectStream)
    {
        this(objectStream, null, new ConstantResult(null));
    }

    public StreamingObjectResult(Stream<T> objectStream, Builder resultBuilder, IResult childResult)
    {
        this(objectStream, resultBuilder, (Result) childResult);
    }

    public StreamingObjectResult(Stream<T> objectStream, Builder resultBuilder, Result childResult)
    {
        super(childResult.activities);
        this.objectStream = objectStream;
        this.resultBuilder = resultBuilder;
        this.childResult = childResult;
    }

    @Override
    public <V> V accept(ResultVisitor<V> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    public Stream<T> getObjectStream()
    {
        return this.objectStream;
    }

    public Result getChildResult()
    {
        return this.childResult;
    }

    @Override
    public Builder getResultBuilder()
    {
        return this.resultBuilder;
    }

    @Override
    public void close()
    {
        this.objectStream = null;
        this.childResult.close();
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        if (format.equals(SerializationFormat.DEFAULT))
        {
            return new StreamingObjectResultJSONSerializer(this);
        }
        else
        {
            this.close();
            throw new RuntimeException(format.toString() + " format not currently supported with StreamingObjectResult");
        }
    }
}
