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

package org.finos.legend.engine.test.emit.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

import java.nio.file.Path;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EMITModelDescriptor
{
    private final String name;
    private final String title;
    private final String description;
    private final ModelSources modelSources;
    private final ImmutableList<String> features;
    private final ImmutableList<String> stores;
    private final String complexity;
    private final ImmutableList<String> tags;
    private Path source;

    private EMITModelDescriptor(String name,
                                String title,
                                String description,
                                ModelSources modelSources,
                                ImmutableList<String> features,
                                ImmutableList<String> stores,
                                String complexity,
                                ImmutableList<String> tags)
    {
        this.name = name;
        this.title = title;
        this.description = description;
        this.modelSources = modelSources;
        this.features = features;
        this.stores = stores;
        this.complexity = complexity;
        this.tags = tags;
    }

    @JsonCreator
    public static EMITModelDescriptor newDescriptor(@JsonProperty("name") String name,
                                                    @JsonProperty("title") String title,
                                                    @JsonProperty("description") String description,
                                                    @JsonProperty("modelSources") ModelSources modelSources,
                                                    @JsonProperty("features") List<String> features,
                                                    @JsonProperty("stores") List<String> stores,
                                                    @JsonProperty("complexity") String complexity,
                                                    @JsonProperty("tags") List<String> tags)
    {
        return new EMITModelDescriptor(
                name,
                title,
                description,
                modelSources,
                immutable(features),
                immutable(stores),
                complexity,
                immutable(tags));
    }

    public String getName()
    {
        return this.name;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getDescription()
    {
        return this.description;
    }

    public ModelSources getModelSources()
    {
        return this.modelSources;
    }

    public List<String> getFeatures()
    {
        return this.features.castToList();
    }

    public List<String> getStores()
    {
        return this.stores.castToList();
    }

    public String getComplexity()
    {
        return this.complexity;
    }

    public List<String> getTags()
    {
        return this.tags.castToList();
    }

    @JsonIgnore
    public Path getSource()
    {
        return this.source;
    }

    public void setSource(Path source)
    {
        this.source = source;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelSources
    {
        private final Source model;
        private final ImmutableList<Dependency> dependencies;

        private ModelSources(Source model, ImmutableList<Dependency> dependencies)
        {
            this.model = model;
            this.dependencies = dependencies;
        }

        @JsonCreator
        public static ModelSources newModelSources(@JsonProperty("model") Source model,
                                                   @JsonProperty("dependencies") List<Dependency> dependencies)
        {
            return new ModelSources(model, immutable(dependencies));
        }

        public Source getModel()
        {
            return this.model;
        }

        public List<Dependency> getDependencies()
        {
            return this.dependencies.castToList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Source
    {
        private final String root;
        private final ImmutableList<String> files;

        private Source(String root, ImmutableList<String> files)
        {
            this.root = root;
            this.files = files;
        }

        @JsonCreator
        public static Source newSource(@JsonProperty("root") String root,
                                       @JsonProperty("files") List<String> files)
        {
            return new Source(root, immutable(files));
        }

        public String getRoot()
        {
            return this.root;
        }

        public List<String> getFiles()
        {
            return this.files.castToList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dependency
    {
        private final String root;
        private final ImmutableList<String> files;
        private final String source;
        private final ImmutableList<String> excludes;

        private Dependency(String root, ImmutableList<String> files, String source, ImmutableList<String> excludes)
        {
            this.root = root;
            this.files = files;
            this.source = source;
            this.excludes = excludes;
        }

        public String getRoot()
        {
            return this.root;
        }

        public List<String> getFiles()
        {
            return this.files.castToList();
        }

        public String getSource()
        {
            return this.source;
        }

        public List<String> getExcludes()
        {
            return this.excludes.castToList();
        }

        public boolean isInline()
        {
            return this.root != null;
        }

        public boolean isReference()
        {
            return this.source != null;
        }

        @JsonCreator
        public static Dependency newDependency(@JsonProperty("root") String root,
                                               @JsonProperty("files") List<String> files,
                                               @JsonProperty("source") String source,
                                               @JsonProperty("excludes") List<String> excludes)
        {
            return new Dependency(root, immutable(files), source, immutable(excludes));
        }
    }

    private static <T> ImmutableList<T> immutable(List<T> list)
    {
        return (list == null) ? Lists.immutable.empty() : Lists.immutable.withAll(list);
    }
}
