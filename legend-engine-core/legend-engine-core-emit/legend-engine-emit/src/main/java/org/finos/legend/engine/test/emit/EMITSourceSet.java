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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.test.emit.catalog.EMITModelDescriptor;

import java.util.List;
import java.util.function.Consumer;

public class EMITSourceSet
{
    private final EMITModelDescriptor descriptor;
    private final ImmutableList<EMITSourceFile> modelFiles;
    private final ImmutableList<EMITSourceFile> dependencyFiles;

    public EMITSourceSet(EMITModelDescriptor descriptor, List<EMITSourceFile> modelFiles, List<EMITSourceFile> dependencyFiles)
    {
        this.descriptor = descriptor;
        this.modelFiles = (modelFiles == null) ? Lists.immutable.empty() : Lists.immutable.withAll(modelFiles);
        this.dependencyFiles = (dependencyFiles == null) ? Lists.immutable.empty() : Lists.immutable.withAll(dependencyFiles);
    }

    public EMITModelDescriptor getDescriptor()
    {
        return this.descriptor;
    }

    public List<EMITSourceFile> getModelFiles()
    {
        return this.modelFiles.castToList();
    }

    public List<EMITSourceFile> getDependencyFiles()
    {
        return this.dependencyFiles.castToList();
    }

    public int getTotalFileCount()
    {
        return this.modelFiles.size() + this.dependencyFiles.size();
    }

    public void forEachFile(Consumer<? super EMITSourceFile> consumer)
    {
        this.modelFiles.forEach(consumer);
        this.dependencyFiles.forEach(consumer);
    }

    public static EMITSourceSet newSourceSet(EMITModelDescriptor descriptor, List<EMITSourceFile> modelFiles, List<EMITSourceFile> dependencyFiles)
    {
        return new EMITSourceSet(descriptor, modelFiles, dependencyFiles);
    }
}
