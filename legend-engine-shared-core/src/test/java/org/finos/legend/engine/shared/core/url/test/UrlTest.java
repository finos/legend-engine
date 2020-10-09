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

package org.finos.legend.engine.shared.core.url.test;

import org.apache.commons.io.IOUtils;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class UrlTest
{
    private static boolean factoryIsSet = false;

    @BeforeClass
    public static synchronized void setUpUlrFactory()
    {
        EngineUrlStreamHandlerFactory.initialize();
    }

    public String readUrl(String urlString)
    {
        try
        {
            URL url = new URL(urlString);
            try (InputStream stream = url.openStream())
            {
                return IOUtils.toString(stream);
            }
        }
        catch (IOException e)
        {
            throw new AssertionError("Failed to read URL as String", e);
        }
    }
}
