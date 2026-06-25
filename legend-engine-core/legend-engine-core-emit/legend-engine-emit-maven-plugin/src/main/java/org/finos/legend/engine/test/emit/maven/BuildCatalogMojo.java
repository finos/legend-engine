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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Mojo(
        name = "build-catalog",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true
)
public class BuildCatalogMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${maven.multiModuleProjectDirectory}")
    private File repoRoot;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/emit-catalog")
    private File outputDirectory;

    @Parameter(defaultValue = "src/test/resources/emit-models")
    private String emitModelsSubPath;

    @Parameter(defaultValue = "index.txt")
    private String indexFileName;

    @Override
    public void execute() throws MojoExecutionException
    {
        Path repo = this.repoRoot.toPath();
        Path out = this.outputDirectory.toPath();
        try
        {
            List<Path> yamls = EmitCatalogBuilder.findYamls(repo, this.emitModelsSubPath);
            getLog().info("EMIT catalog: found " + yamls.size() + " yaml descriptor(s) under " + repo);
            List<String> entries = EmitCatalogBuilder.writeCatalog(yamls, repo, out, this.indexFileName);
            getLog().info("EMIT catalog: wrote " + entries.size() + " entries + manifest to " + out);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Failed to build EMIT catalog from " + repo, e);
        }
    }
}

