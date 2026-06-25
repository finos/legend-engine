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

package org.finos.legend.engine.test.emit.maven;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class EmitCatalogBuilder
{
    public static final String YAML_SUFFIX = ".emit.yaml";

    private static final Pattern EXCLUDED_DIR_SEGMENTS = Pattern.compile(".*/(target|node_modules|\\.git|\\.idea|build)/.*");

    private static final Pattern EXCLUDED_MODULES = Pattern.compile(".*/(legend-engine-emit-junit)/.*");

    private EmitCatalogBuilder()
    {
    }

    public static List<Path> findYamls(Path repoRoot, String emitModelsSubPath) throws IOException
    {
        if (repoRoot == null || !Files.isDirectory(repoRoot))
        {
            throw new IOException("Repo root is not a directory: " + repoRoot);
        }
        if (emitModelsSubPath == null || emitModelsSubPath.isEmpty())
        {
            throw new IllegalArgumentException("emitModelsSubPath must be non-empty");
        }
        String marker = "/" + emitModelsSubPath.replace(File.separatorChar, '/') + "/";
        List<Path> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(repoRoot))
        {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(YAML_SUFFIX))
                    .forEach(p ->
                    {
                        String rel = "/" + repoRoot.relativize(p).toString().replace(File.separatorChar, '/');
                        if (rel.contains(marker)
                                && !EXCLUDED_DIR_SEGMENTS.matcher(rel).matches()
                                && !EXCLUDED_MODULES.matcher(rel).matches())
                        {
                            result.add(p);
                        }
                    });
        }
        result.sort(Comparator.comparing(p -> repoRoot.relativize(p).toString().replace(File.separatorChar, '/')));
        return result;
    }

    public static List<String> writeCatalog(List<Path> yamls,
                                            Path repoRoot,
                                            Path outputDirectory,
                                            String indexFileName) throws IOException
    {
        if (indexFileName == null || indexFileName.isEmpty())
        {
            throw new IllegalArgumentException("indexFileName must be non-empty");
        }
        Files.createDirectories(outputDirectory);
        List<String> entries = new ArrayList<>(yamls.size());
        for (Path yaml : yamls)
        {
            String rel = repoRoot.relativize(yaml).toString().replace(File.separatorChar, '/');
            Path target = outputDirectory.resolve(rel);
            Path parent = target.getParent();
            if (parent != null)
            {
                Files.createDirectories(parent);
            }
            Files.copy(yaml, target, StandardCopyOption.REPLACE_EXISTING);
            entries.add(rel);
        }
        StringBuilder sb = new StringBuilder();
        for (String entry : entries)
        {
            sb.append(entry).append('\n');
        }
        Files.write(outputDirectory.resolve(indexFileName), sb.toString().getBytes(StandardCharsets.UTF_8));
        return entries;
    }
}

