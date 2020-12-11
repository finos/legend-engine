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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.util.Objects;

public class UrlFactory
{
    public static URL create(String spec) throws MalformedURLException
    {
        Objects.requireNonNull(spec, "URL spec cannot be null");
        int pos = spec.indexOf(":");
        if (pos < 2)
        {
            throw new MalformedURLException("Absolute URL spec does not include protocol");
        }
        String protocol = spec.substring(0, pos);
        URLStreamHandler handler = EngineUrlStreamHandlerFactory.INSTANCE.createURLStreamHandler(protocol);

        return handler == null
            ? new URL(spec)
            : new URL(null, spec, handler);
    }
}
