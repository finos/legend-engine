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

package org.finos.legend.engine.shared.core.operational.opentracing;

import io.opentracing.propagation.TextMap;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.Iterator;
import java.util.Map;

public class HttpRequestHeaderMap implements TextMap
{
    private final HttpRequestBase request;

    public HttpRequestHeaderMap(HttpRequestBase request)
    {
        this.request = request;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator()
    {
        HeaderIterator headerIterator = request.headerIterator();
        return new Iterator<Map.Entry<String, String>>()
        {
            @Override
            public boolean hasNext()
            {
                return headerIterator.hasNext();
            }

            @Override
            public Map.Entry<String, String> next()
            {
                Header header = headerIterator.nextHeader();
                return new Map.Entry<String, String>()
                {
                    @Override
                    public String getKey()
                    {
                        return header.getName();
                    }

                    @Override
                    public String getValue()
                    {
                        return header.getValue();
                    }

                    @Override
                    public String setValue(String value)
                    {
                        throw new RuntimeException("Cannot modify header in place");
                    }
                };
            }
        };
    }

    @Override
    public void put(String key, String value)
    {
        request.addHeader(key, value);
    }
}
