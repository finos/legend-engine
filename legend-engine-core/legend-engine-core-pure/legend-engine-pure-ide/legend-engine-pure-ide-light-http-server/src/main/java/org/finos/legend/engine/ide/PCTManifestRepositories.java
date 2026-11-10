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

package org.finos.legend.engine.ide;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositorySet;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;

final class PCTManifestRepositories
{
    private PCTManifestRepositories()
    {
    }

    static CodeRepositorySet selectRepositories(CodeRepositorySet available, RepositoryCodeStorage codeStorage)
    {
        MutableList<String> required = Lists.mutable.with("core", "pure_ide");
        available.getRepositories().select(repository -> hasManifestDirectory("/" + repository.getName(), codeStorage))
                .collect(CodeRepository::getName, required);
        return available.subset(required);
    }

    private static boolean hasManifestDirectory(String repositoryRoot, RepositoryCodeStorage codeStorage)
    {
        return codeStorage.isFolder(repositoryRoot) && (containsManifest(repositoryRoot, codeStorage)
                || codeStorage.getFiles(repositoryRoot).anySatisfy(node -> codeStorage.isFolder(node.getPath()) && containsManifest(node.getPath(), codeStorage)));
    }

    private static boolean containsManifest(String folder, RepositoryCodeStorage codeStorage)
    {
        return codeStorage.getFiles(folder).anySatisfy(node -> node.getPath().endsWith("_manifest.json"));
    }
}
