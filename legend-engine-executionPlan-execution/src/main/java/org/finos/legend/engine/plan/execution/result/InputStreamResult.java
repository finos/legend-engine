// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.builder.stream.StreamBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class InputStreamResult extends StreamingResult
{
    private InputStream inputStream;

    public InputStreamResult(InputStream inputStream)
    {
        super(Lists.mutable.empty());
        this.inputStream = inputStream;
    }

    public InputStreamResult(InputStream inputStream, List<ExecutionActivity> activities)
    {
        super(activities);
        this.inputStream = inputStream;
    }

    public InputStream getInputStream()
    {
        return this.inputStream;
    }

    public Builder getResultBuilder()
    {
        return new StreamBuilder();
    }

    @Override
    public <V> V accept(ResultVisitor<V> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    public void stream(OutputStream outputStream, SerializationFormat format)
    {
        throw new UnsupportedOperationException("Streaming InputStreamResult result is not supported. Please raise a issue with dev team");
    }

    public void stream(OutputStream outputStream, Serializer serializer)
    {
        throw new UnsupportedOperationException("Streaming InputStreamResult result is not supported. Please raise a issue with dev team");
    }

    public String flush(Serializer serializer)
    {
        throw new UnsupportedOperationException("Streaming InputStreamResult result is not supported. Please raise a issue with dev team");
    }

    public Serializer getSerializer(SerializationFormat format)
    {
        throw new UnsupportedOperationException("Streaming InputStreamResult result is not supported. Please raise a issue with dev team");
    }
}
