// Copyright 2023 Goldman Sachs
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

import org.apache.commons.io.output.ProxyOutputStream;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RealizedJsonResult extends ConstantResult
{
    // 1 MB
    private static final long DEFAULT_BYTE_LIMIT = 1000000;
    public static final String BYTE_LIMIT_PROPERTY_NAME = "org.finos.legend.engine.realizedJsonResultByteLimit";
    
    public RealizedJsonResult(JsonStreamingResult jsonStreamingResult) throws IOException
    {
        super(processStreamingResult(jsonStreamingResult));
    }

    private static String processStreamingResult(JsonStreamingResult jsonStreamingResult) throws IOException
    {
        ByteArrayOutputStream delegate = new ByteArrayOutputStream();
        MaxSizeOutputStream outputStream = new MaxSizeOutputStream(delegate, getByteLimit());
        jsonStreamingResult.getSerializer(SerializationFormat.PURE).stream(outputStream);

        String value = delegate.toString();
        outputStream.close();
        return value;
    }

    private static long getByteLimit()
    {
        return Long.getLong(BYTE_LIMIT_PROPERTY_NAME, DEFAULT_BYTE_LIMIT);
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
    }

    private static class MaxSizeOutputStream extends ProxyOutputStream
    {
        private final long maxBytesForGeneration;
        
        private long bytesUsed = 0L;
        
        public MaxSizeOutputStream(OutputStream proxy, long maxBytesForGeneration)
        {
            super(proxy);
            this.maxBytesForGeneration = maxBytesForGeneration;
        }

        @Override
        protected void beforeWrite(int n) throws IOException
        {
            this.increment(n);
            this.checkMaxBytes();
        }

        private void increment(int n)
        {
            this.bytesUsed += n;
        }

        private void checkMaxBytes() throws IOException
        {
            if (this.bytesUsed >= this.maxBytesForGeneration)
            {
                this.close();
                throw new IOException(String.format("Maximum bytes for generation [%s] exceeded!", this.maxBytesForGeneration));
            }
        }
    }
}
