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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class EMITModelDiscovery
{
    private static final String EMIT_YAML_SUFFIX = ".emit.yaml";

    private static final String CATALOG_ROOT = "META-INF/emit-catalog";

    private static final String CATALOG_INDEX = CATALOG_ROOT + "/index.txt";

    private static final String EMIT_MODELS_SUBPATH = "src/test/resources/emit-models";

    private static final String EMIT_MODELS_MARKER = "/" + EMIT_MODELS_SUBPATH + "/";

    private static final Pattern EXCLUDED_DIR_SEGMENTS = Pattern.compile(
            ".*/(target|node_modules|\\.git|\\.idea|build)/.*");

    private static final Pattern EXCLUDED_MODULES = Pattern.compile(
            ".*/(legend-engine-emit-junit)/.*");

    private static final Pattern REPO_ROOT_ARTIFACT_ID = Pattern.compile(
            "<artifactId>\\s*legend-engine\\s*</artifactId>");

    private EMITModelDiscovery()
    {
    }

    public static List<EMITModelDescriptor> fromClasspath(ClassLoader classLoader)
    {
        ClassLoader cl = (classLoader == null) ? getClassLoader() : classLoader;
        EMITModelLoader loader = new EMITModelLoader();
        List<EMITModelDescriptor> result = new ArrayList<>();
        try
        {
            Enumeration<URL> indices = cl.getResources(CATALOG_INDEX);
            while (indices.hasMoreElements())
            {
                URL indexUrl = indices.nextElement();
                for (String entry : readIndex(indexUrl))
                {
                    String resourcePath = CATALOG_ROOT + "/" + entry;
                    URL yamlUrl = cl.getResource(resourcePath);
                    if (yamlUrl == null)
                    {
                        throw new UncheckedIOException(new IOException(
                                "EMIT catalog index at " + indexUrl + " lists entry '" + entry
                                        + "' but the classloader cannot find it at '" + resourcePath + "'"));
                    }
                    EMITModelDescriptor descriptor = loader.parseDescriptor(yamlUrl);
                    descriptor.setResourcePath(entry);
                    descriptor.setModule(deriveModule(entry));
                    result.add(descriptor);
                }
            }
            return result;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to read EMIT catalog from the classpath", e);
        }
    }

    private static List<String> readIndex(URL indexUrl) throws IOException
    {
        List<String> entries = new ArrayList<>();
        try (InputStream in = indexUrl.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                String trimmed = line.trim();
                if (!trimmed.isEmpty())
                {
                    entries.add(trimmed);
                }
            }
        }
        return entries;
    }

    public static List<EMITModelDescriptor> fromFileSystem(Path repoRoot)
    {
        if (repoRoot == null || !Files.isDirectory(repoRoot))
        {
            throw new IllegalArgumentException("Repo root is not a directory: " + repoRoot);
        }
        List<Path> yamls = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(repoRoot))
        {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(EMIT_YAML_SUFFIX))
                    .forEach(p ->
                    {
                        String rel = "/" + repoRoot.relativize(p).toString().replace(java.io.File.separatorChar, '/');
                        if (rel.contains(EMIT_MODELS_MARKER)
                                && !EXCLUDED_DIR_SEGMENTS.matcher(rel).matches()
                                && !EXCLUDED_MODULES.matcher(rel).matches())
                        {
                            yamls.add(p);
                        }
                    });
        }
        catch (IOException e)
        {
            throw new UncheckedIOException("Failed to walk repo root '" + repoRoot + "'", e);
        }
        yamls.sort(Comparator.comparing(p -> repoRoot.relativize(p).toString().replace(java.io.File.separatorChar, '/')));

        EMITModelLoader loader = new EMITModelLoader();
        List<EMITModelDescriptor> result = new ArrayList<>(yamls.size());
        for (Path yaml : yamls)
        {
            EMITModelDescriptor descriptor;
            try
            {
                descriptor = loader.parseDescriptor(yaml);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to parse EMIT descriptor: " + yaml, e);
            }
            String rel = repoRoot.relativize(yaml).toString().replace(java.io.File.separatorChar, '/');
            descriptor.setResourcePath(rel);
            descriptor.setModule(deriveModule(rel));
            result.add(descriptor);
        }
        return result;
    }

    public static Path findRepoRoot() throws IOException
    {
        Path start = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path candidate = start; candidate != null; candidate = candidate.getParent())
        {
            Path pom = candidate.resolve("pom.xml");
            if (Files.isRegularFile(pom) && isLegendEngineRootPom(pom))
            {
                return candidate;
            }
        }
        throw new IOException("Could not locate legend-engine repo root walking up from " + start
                + " (no ancestor pom.xml declares <artifactId>legend-engine</artifactId>)");
    }

    private static boolean isLegendEngineRootPom(Path pom)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(pom);
            String head = new String(bytes, 0, Math.min(bytes.length, 4096), StandardCharsets.UTF_8);
            Matcher m = REPO_ROOT_ARTIFACT_ID.matcher(head);
            return m.find();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    public static Path findEmitYaml(String classpathRoot, String name)
    {
        return findEmitYaml(getClassLoader(), classpathRoot, name);
    }

    public static Path findEmitYaml(ClassLoader classLoader, String classpathRoot, String name)
    {
        String normalizedRoot = normalizeRoot(classpathRoot);
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
        return findEmitYamls(getClassLoader(), classpathRoot);
    }

    public static List<Path> findEmitYamls(ClassLoader classLoader, String classpathRoot)
    {
        String normalizedRoot = normalizeRoot(classpathRoot);
        try
        {
            List<Path> result = new ArrayList<>();
            Enumeration<URL> urls = classLoader.getResources(normalizedRoot);
            while (urls.hasMoreElements())
            {
                walkRoot(urls.nextElement(), result::add);
            }
            result.sort(null);
            return result;
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
                walk.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(EMIT_YAML_SUFFIX)).forEach(consumer);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException("Failed to walk classpath root '" + root + "'", e);
            }
        }
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

    private static String deriveModule(String resourcePath)
    {
        if (resourcePath == null)
        {
            return null;
        }
        int idx = resourcePath.indexOf("/src/test/resources/");
        if (idx <= 0)
        {
            return null;
        }
        int prev = resourcePath.lastIndexOf('/', idx - 1);
        return (prev < 0) ? resourcePath.substring(0, idx) : resourcePath.substring(prev + 1, idx);
    }
}
