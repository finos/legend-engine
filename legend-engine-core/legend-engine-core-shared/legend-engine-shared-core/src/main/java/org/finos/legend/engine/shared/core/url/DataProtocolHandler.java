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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.util.Base64;

public class DataProtocolHandler extends URLStreamHandler implements UrlProtocolHandler
{
    public static final String DATA_PROTOCOL_NAME = "data";

    @Override
    public boolean handles(String protocol)
    {
        return DATA_PROTOCOL_NAME.equals(protocol);
    }

    @Override
    public URLStreamHandler getHandler(String protocol)
    {
        if (!DATA_PROTOCOL_NAME.equals(protocol))
        {
            throw new IllegalArgumentException("Invalid protocol: " + protocol);
        }
        return this;
    }

    @Override
    protected URLConnection openConnection(URL url) throws IOException
    {
        return new DataUrlConnection(url);
    }

    private static class DataUrlConnection extends URLConnection
    {
        private boolean connected = false;
        private byte[] data;

        DataUrlConnection(URL url)
        {
            super(url);
        }

        @Override
        public void connect() throws IOException
        {
            String path = getURL().toExternalForm().substring(DATA_PROTOCOL_NAME.length() + 1);
            int pos = 0;
            boolean base64 = false;
            while (pos < path.length() && path.charAt(pos) != ',' && path.charAt(pos) != ';')
            {
                pos++;
            }

            if (path.charAt(pos) == ';')
            {
                if (path.length() < pos + 7 || !";base64,".equals(path.substring(pos, pos + 8)))
                {
                    throw new MalformedURLException("Expected ;Base64, or ,");
                }
                pos += 7;
                base64 = true;
            }

            pos++;

            data = base64
                    ? Base64.getDecoder().decode(path.substring(pos))
                    : URLDecoder.decode(path.substring(pos), "UTF-8").getBytes();
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException
        {
            if (!connected)
            {
                connect();
            }
            return new ByteArrayInputStream(data);
        }
    }
}