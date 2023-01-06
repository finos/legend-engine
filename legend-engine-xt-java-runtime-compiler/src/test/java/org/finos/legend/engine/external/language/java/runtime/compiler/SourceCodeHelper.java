//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.language.java.runtime.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

public class SourceCodeHelper
{
    public static final String VALID_CLASS = "org.finos.legend.engine.external.language.java.runtime.compiler.shared.ValidJavaClass";
    public static final String INVALID_CLASS = "org.finos.legend.engine.external.language.java.runtime.compiler.shared.InvalidJavaClass";

    public static JavaFileObject loadSourceCode(String className)
    {
        URL url = getSourceCodeURL(className);
        String content = loadSourceCodeContent(url);

        URI uri;
        try
        {
            uri = url.toURI();
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }

        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE)
        {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors)
            {
                return content;
            }

            @Override
            public InputStream openInputStream()
            {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    public static String loadSourceCodeContent(String className)
    {
        return loadSourceCodeContent(getSourceCodeURL(className));
    }

    private static URL getSourceCodeURL(String className)
    {
        String sourceFile = className.replace('.', '/') + ".java";
        URL url = Thread.currentThread().getContextClassLoader().getResource(sourceFile);
        if (url == null)
        {
            throw new RuntimeException("Cannot find source: " + sourceFile);
        }
        return url;
    }

    private static String loadSourceCodeContent(URL url)
    {
        try (Reader reader = new InputStreamReader(url.openStream()))
        {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8096];
            int read;
            while ((read = reader.read(buffer)) != -1)
            {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }
}
