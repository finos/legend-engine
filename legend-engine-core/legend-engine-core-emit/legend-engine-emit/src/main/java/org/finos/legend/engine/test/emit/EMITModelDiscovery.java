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

package org.finos.legend.engine.test.emit;

import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class EMITModelDiscovery
{
    private static final String EMIT_YAML_SUFFIX = ".emit.yaml";

    private EMITModelDiscovery()
    {
    }

    public static Path findEmitYaml(String classpathRoot, String name)
    {
        String normalizedRoot = normalizeRoot(classpathRoot);
        ClassLoader classLoader = getClassLoader();
        URL url = classLoader.getResource(normalizedRoot + "/" + name + EMIT_YAML_SUFFIX);
        if (url == null)
        {
            return null;
        }
        try
        {
            return Paths.get(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new IllegalStateException("Invalid classpath URL: " + url, e);
        }
    }

    public static List<Path> findEmitYamls(String classpathRoot)
    {
        String normalizedRoot = normalizeRoot(classpathRoot);
        ClassLoader classLoader = getClassLoader();
        try
        {
            List<Path> result = new ArrayList<>();
            Enumeration<URL> urls = classLoader.getResources(normalizedRoot);
            while (urls.hasMoreElements())
            {
                walkClasspathRoot(urls.nextElement(), result::add);
            }
            result.sort(null);
            return result;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to enumerate classpath resources under '" + classpathRoot + "'", e);
        }
    }

    public static List<EMITModelDescriptor> findDescriptorsViaSPI()
    {
        List<EMITModelDescriptor> result = new ArrayList<>();
        for (EMITModelProvider provider : ServiceLoader.load(EMITModelProvider.class))
        {
            String module = provider.getModule();
            for (EMITModelDescriptor descriptor : provider.getDescriptors())
            {
                if (module != null && descriptor.getModule() == null)
                {
                    descriptor.setModule(module);
                }
                result.add(descriptor);
            }
        }
        return result;
    }

    private static String normalizeRoot(String classpathRoot)
    {
        if (classpathRoot == null || classpathRoot.isEmpty())
        {
            throw new IllegalArgumentException("classpathRoot must be non-empty");
        }
        return classpathRoot.endsWith("/") ? classpathRoot.substring(0, classpathRoot.length() - 1) : classpathRoot;
    }

    private static ClassLoader getClassLoader()
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return (classLoader == null) ? EMITModelDiscovery.class.getClassLoader() : classLoader;
    }

    private static void walkClasspathRoot(URL url, Consumer<? super Path> consumer)
    {
        if (!"file".equals(url.getProtocol()))
        {
            throw new UnsupportedOperationException("EMIT discovery currently only supports file: classpath roots, but got: " + url);
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
                walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(EMIT_YAML_SUFFIX)).forEach(consumer);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to walk classpath root '" + root + "'", e);
            }
        }
    }
}

