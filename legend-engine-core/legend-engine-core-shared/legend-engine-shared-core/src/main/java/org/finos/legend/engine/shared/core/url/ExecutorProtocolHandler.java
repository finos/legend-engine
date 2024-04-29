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

package org.finos.legend.engine.shared.core.url;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class ExecutorProtocolHandler extends URLStreamHandler implements UrlProtocolHandler
{
    public static final String EXECUTOR_PROTOCOL_NAME = "executor";

    @Override
    public boolean handles(String protocol)
    {
        return EXECUTOR_PROTOCOL_NAME.equals(protocol);
    }

    @Override
    public URLStreamHandler getHandler(String protocol)
    {
        if (!EXECUTOR_PROTOCOL_NAME.equals(protocol))
        {
            throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }
        return this;
    }

    @Override
    protected URLConnection openConnection(URL u)
    {
        return new ExecutorUrlConnection(u);
    }

    private static class ExecutorUrlConnection extends URLConnection
    {
        private ExecutorUrlConnection(URL url)
        {
            super(url);
        }

        @Override
        public void connect()
        {
            getInputStream();
        }

        @Override
        public InputStream getInputStream()
        {
            StreamProvider streamProvider = StreamProviderHolder.streamProviderThreadLocal.get();
            if (streamProvider != null)
            {
                InputStream stream = streamProvider.getInputStream(getURL().getPath());
                if (stream != null)
                {
                    return stream;
                }
            }
            throw new RuntimeException("Input stream was not provided");
        }
    }
}
