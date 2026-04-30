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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

public class EMITModelLoader
{
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    public EMITModelDescriptor parseDescriptor(Path emitYaml) throws IOException
    {
        EMITModelDescriptor descriptor = YAML.readValue(emitYaml.toFile(), EMITModelDescriptor.class);
        descriptor.setSource(emitYaml.toAbsolutePath().normalize());
        return descriptor;
    }

    public EMITSourceSet load(Path emitYaml) throws IOException
    {
        return load(parseDescriptor(emitYaml));
    }

    public EMITSourceSet load(EMITModelDescriptor descriptor) throws IOException
    {
        if (descriptor.getSource() == null)
        {
            throw new IllegalArgumentException("Descriptor has no source path; load it via parseDescriptor or set descriptor source explicitly");
        }
        if (descriptor.getModelSources() == null || descriptor.getModelSources().getModel() == null)
        {
            throw new IllegalArgumentException("Descriptor at " + descriptor.getSource() + " has no modelSources.model section");
        }

        Path baseDir = descriptor.getSource().getParent();
        MutableList<EMITSourceFile> modelFiles = resolveSource(baseDir, descriptor.getModelSources().getModel(), true);

        MutableList<EMITSourceFile> dependencyFiles = Lists.mutable.empty();
        for (EMITModelDescriptor.Dependency dep : descriptor.getModelSources().getDependencies())
        {
            dependencyFiles.addAll(resolveDependency(baseDir, dep));
        }

        validateNoClashes(modelFiles, dependencyFiles);

        return EMITSourceSet.newSourceSet(descriptor, modelFiles, dependencyFiles);
    }

    private MutableList<EMITSourceFile> resolveSource(Path baseDir, EMITModelDescriptor.Source source, boolean primary) throws IOException
    {
        if (source.getRoot() == null)
        {
            throw new IllegalArgumentException("Source has no 'root' field");
        }
        Path rootDir = baseDir.resolve(source.getRoot()).normalize();
        MutableList<EMITSourceFile> resolved = Lists.mutable.empty();
        for (String file : source.getFiles())
        {
            Path absolute = rootDir.resolve(file).normalize();
            if (!Files.exists(absolute))
            {
                throw new IOException("Source file does not exist: " + absolute + " (declared as '" + file + "' under root '" + rootDir + "')");
            }
            resolved.add(new EMITSourceFile(file, absolute, primary));
        }
        return resolved;
    }

    private List<EMITSourceFile> resolveDependency(Path baseDir, EMITModelDescriptor.Dependency dep) throws IOException
    {
        if (dep.isInline())
        {
            EMITModelDescriptor.Source inline = EMITModelDescriptor.Source.newSource(dep.getRoot(), dep.getFiles());
            return resolveSource(baseDir, inline, false);
        }
        if (dep.isReference())
        {
            Path referencedYaml = baseDir.resolve(dep.getSource()).normalize();
            EMITSourceSet referenced = load(referencedYaml);
            ListIterable<PathMatcher> excludeMatchers = compileGlobs(dep.getExcludes());

            MutableList<EMITSourceFile> result = Lists.mutable.empty();
            referenced.forEachFile(f ->
            {
                if (!matchesAny(excludeMatchers, baseDir, f))
                {
                    result.add(f);
                }
            });
            return result;
        }
        throw new IllegalArgumentException("Dependency must specify either ('root' + 'files') or 'source' (with optional 'excludes')");
    }

    private ListIterable<PathMatcher> compileGlobs(List<String> patterns)
    {
        if (patterns == null)
        {
            return Lists.immutable.empty();
        }
        FileSystem fs = FileSystems.getDefault();
        return ListIterate.collect(patterns, p -> fs.getPathMatcher("glob:" + p));
    }

    private boolean matchesAny(ListIterable<PathMatcher> matchers, Path baseDir, EMITSourceFile file)
    {
        return matchesAny(matchers, baseDir, file.getAbsolutePath());
    }

    private boolean matchesAny(ListIterable<PathMatcher> matchers, Path baseDir, Path absolute)
    {
        if (matchers.isEmpty())
        {
            return false;
        }
        Path relative = tryRelativize(baseDir, absolute);
        return matchers.anySatisfy((relative == null) ? m -> m.matches(absolute) : m -> m.matches(relative) || m.matches(absolute));
    }

    private Path tryRelativize(Path baseDir, Path absolute)
    {
        try
        {
            return baseDir.relativize(absolute);
        }
        catch (IllegalArgumentException ignore)
        {
            return null;
        }
    }

    private void validateNoClashes(ListIterable<EMITSourceFile> modelFiles, ListIterable<EMITSourceFile> dependencyFiles)
    {
        MutableMap<String, Path> seen = Maps.mutable.empty();
        MutableSet<String> clashes = modelFiles.asLazy().concatenate(dependencyFiles).collectIf(f ->
        {
            Path prior = seen.put(f.getVirtualPath(), f.getAbsolutePath());
            return (prior != null) && !prior.equals(f.getAbsolutePath());
        }, EMITSourceFile::getVirtualPath, Sets.mutable.empty());
        if (clashes.notEmpty())
        {
            throw new IllegalStateException(clashes.toSortedList().makeString("Source file virtual-path clashes: ", ", ", ""));
        }
    }
}
