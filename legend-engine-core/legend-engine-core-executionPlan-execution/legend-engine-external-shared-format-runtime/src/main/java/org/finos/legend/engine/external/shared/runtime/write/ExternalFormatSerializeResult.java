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

package org.finos.legend.engine.external.shared.runtime.write;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;

public class ExternalFormatSerializeResult extends StreamingResult
{
    private final ExternalFormatWriter externalFormatWriter;
    private final Result childResult;

    public ExternalFormatSerializeResult(ExternalFormatWriter externalFormatWriter, Result childResult)
    {
        this(externalFormatWriter, childResult, "");
    }

    public ExternalFormatSerializeResult(ExternalFormatWriter externalFormatWriter, Result childResult, String resultFormat)
    {
        super(Lists.mutable.empty());
        this.externalFormatWriter = externalFormatWriter;
        this.childResult = childResult;
        this.resultFormat = resultFormat;
    }

    @Override
    public Builder getResultBuilder()
    {
        return null;
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        return new ExternalFormatDefaultSerializer(externalFormatWriter, this);
    }

    @Override
    public String flush(Serializer serializer)
    {
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            externalFormatWriter.writeDataAsString(bos);
            return bos.toString(StandardCharsets.UTF_8.name());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
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
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
    }
}
