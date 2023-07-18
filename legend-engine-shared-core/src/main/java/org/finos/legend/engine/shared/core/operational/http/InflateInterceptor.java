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

package org.finos.legend.engine.shared.core.operational.http;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

public class InflateInterceptor implements ReaderInterceptor
{
    public static final String APPLICATION_ZLIB = "application/zlib";

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
    {
        if (context.getMediaType().getSubtype().equals("zlib"))
        {
            InflaterInputStream iis = new InflaterInputStream(context.getInputStream());
            BufferedInputStream bis = new BufferedInputStream(iis);
            context.setInputStream(bis);
            context.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        }
        return context.proceed();
    }
}
