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

package org.finos.legend.engine.test.emit.junit.extensions;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

/**
 * Test-only {@link ArtifactGenerationExtension} used by the EMIT JUnit
 * integration self-tests to exercise the per-element artifact generation
 * task emitted by {@link org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder}.
 * Restricted to elements under {@code demo::artifactgen::} so it does not
 * accidentally fire on classes from other test fixtures.
 */
public class EmitDemoArtifactGenerationExtension implements ArtifactGenerationExtension
{
    public static final String KEY = "emit-demo-artifact";
    public static final String PACKAGE_PREFIX = "demo::artifactgen::";

    @Override
    public String getKey()
    {
        return KEY;
    }

    @Override
    public boolean canGenerate(PackageableElement element)
    {
        String path = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(element);
        return path != null && path.startsWith(PACKAGE_PREFIX);
    }

    @Override
    public List<Artifact> generate(PackageableElement element, PureModel pureModel, PureModelContextData data, String clientVersion)
    {
        String path = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(element);
        Artifact artifact = new Artifact("artifact-for:" + path, path.replace("::", "/") + ".artifact.txt", "text/plain");
        return Lists.fixedSize.with(artifact);
    }
}