// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit.junit;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class EMITModelDiscovery
{
    private EMITModelDiscovery()
    {
    }

    static MutableList<Path> findEmitYamls(String classpathRoot)
    {
        if (classpathRoot == null || classpathRoot.isEmpty())
        {
            throw new IllegalArgumentException("classpathRoot must be non-empty");
        }
        String normalised = classpathRoot.endsWith("/") ? classpathRoot.substring(0, classpathRoot.length() - 1) : classpathRoot;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null)
        {
            classLoader = EMITModelDiscovery.class.getClassLoader();
        }
        try
        {
            MutableList<Path> result = Lists.mutable.empty();
            Enumeration<URL> urls = classLoader.getResources(normalised);
            while (urls.hasMoreElements())
            {
                walkRoot(urls.nextElement(), result::add);
            }
            return result.sortThis();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to enumerate classpath resources under '" + classpathRoot + "'", e);
        }
    }

    private static void walkRoot(URL url, Consumer<? super Path> consumer)
    {
        if (!"file".equals(url.getProtocol()))
        {
            throw new UnsupportedOperationException("EMIT JUnit discovery currently only supports file: classpath roots, but got: " + url);
        }
        Path root;
        try
        {
            root = Paths.get(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Invalid classpath URL: " + url, e);
        }
        if (Files.isDirectory(root))
        {
            try (Stream<Path> walk = Files.walk(root))
            {
                walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".emit.yaml")).forEach(consumer);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to walk classpath root '" + root + "'", e);
            }
        }
    }
}
