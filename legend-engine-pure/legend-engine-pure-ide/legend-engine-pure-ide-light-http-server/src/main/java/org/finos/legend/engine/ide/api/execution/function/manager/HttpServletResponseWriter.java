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

package org.finos.legend.engine.ide.api.execution.function.manager;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

public class HttpServletResponseWriter implements HttpResponseWriter
{
    private final HttpServletResponse response;

    public HttpServletResponseWriter(HttpServletResponse response)
    {
        this.response = response;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Override
    public void setContentType(String type)
    {
        this.response.setContentType(type);
    }

    @Override
    public void setContentDisposition(String disposition)
    {
        this.response.setHeader("Content-Disposition", disposition);
    }

    @Override
    public void setHeader(String name, String value)
    {
        this.response.setHeader(name, value);
    }

    @Override
    public void setIsStreamingResponse(boolean streamingResponse)
    {
    }
}
