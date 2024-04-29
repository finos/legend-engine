// Copyright 2021 Goldman Sachs
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
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.generation.artifact.ArtifactGenerationFactory;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.shared.core.identity.Identity;

public class ArtifactGenerationExtensionRunner
{

    private final ModelManager modelManager;

    public ArtifactGenerationExtensionRunner(ModelManager modelManager)
    {

        this.modelManager = modelManager;
    }

    public ArtifactGenerationExtensionOutput run(ArtifactGenerationExtensionInput artifactGenerationExtensionInput, Identity identity)
    {
        String clientVersion = artifactGenerationExtensionInput.clientVersion == null ? PureClientVersions.production : artifactGenerationExtensionInput.clientVersion;
        PureModelContextData mainModel = this.modelManager.loadData(artifactGenerationExtensionInput.model, clientVersion, identity);

        List<PackageableElement> elementList = Lists.mutable.empty();
        if (artifactGenerationExtensionInput.includeElementPaths != null)
        {
            elementList = mainModel.getElements().stream().filter(element -> artifactGenerationExtensionInput.includeElementPaths.contains(element.getPath())).collect(Collectors.toList());
        }
        PureModel pureModel = this.modelManager.loadModel(mainModel, artifactGenerationExtensionInput.clientVersion, identity, null);
        ArtifactGenerationFactory factory = ArtifactGenerationFactory.newFactory(pureModel, mainModel, elementList, artifactGenerationExtensionInput.excludedExtensionKeys);
        return ArtifactGenerationExtensionOutput.fromFactoryResults(factory.generate(), pureModel);
    }

}
