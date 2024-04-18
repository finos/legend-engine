// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.generation.artifact.api;

import java.util.List;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;

public class TestSimpleEnumArtifactGenerationExtension implements ArtifactGenerationExtension
{

    @Override
    public String getKey()
    {
        return "test-enumeration-generation";
    }

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("__Test__");

    }


    @Override
    public boolean canGenerate(PackageableElement packageableElement)
    {
        return packageableElement instanceof Enumeration;
    }

    @Override
    public List<Artifact> generate(PackageableElement packageableElement, PureModel pureModel, PureModelContextData pureModelContextData, String s)
    {
        String content = "Some output for enumeration '" + packageableElement._name() + "'";
        Artifact artifact = new Artifact(content, "SomeTestOutput.txt", "txt");
        return Lists.mutable.with(artifact);
    }
}
