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

package org.finos.legend.engine.server.core;

import io.opentracing.util.GlobalTracer;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

// TODO move this to legend-engine-shared-core. Needs tracing to be referenced from core
public class BodySpanDecorator implements ReaderInterceptor
{
    private static final int MAX_LENGTH = 10240;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(BodySpanDecorator.class);

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
    {
        try
        {
            InputStream is = context.getInputStream();
            if (GlobalTracer.get() != null && GlobalTracer.get().activeSpan() != null && is != null)
            {
                ByteArrayOutputStream os = new ByteArrayOutputStream(MAX_LENGTH + 2);
                IOUtils.copyLarge(is, os, 0, MAX_LENGTH + 1L);
                String body = new String(os.toByteArray());
                if (body.length() > MAX_LENGTH)
                {
                    body = body.substring(0, MAX_LENGTH) + " (truncated)";
                }
                GlobalTracer.get().activeSpan().setTag("body", body);
                context.setInputStream(new SequenceInputStream(new ByteArrayInputStream(os.toByteArray()), is));
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("Unable to trace request body", e);
        }
        return context.proceed();
    }
}
