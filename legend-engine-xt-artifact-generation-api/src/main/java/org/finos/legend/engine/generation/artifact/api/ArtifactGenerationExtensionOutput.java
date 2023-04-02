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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.generation.artifact.ArtifactGenerationResult;
import org.finos.legend.engine.generation.artifact.GenerationOutput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.ArtifactGenerationExtension;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class ArtifactGenerationExtensionOutput
{

    static class SerializedArtifactsByExtensionElement
    {

        public String element;
        public List<GenerationOutput> files;
        public String extension;


        public SerializedArtifactsByExtensionElement(String element, List<GenerationOutput> files, String extension)
        {
            this.element = element;
            this.files = files;
            this.extension = extension;

        }
    }

    static class SerializedArtifactExtensionResult
    {

        public String extension;
        public List<SerializedArtifactsByExtensionElement> artifactsByExtensionElement;

        public SerializedArtifactExtensionResult(String extension, List<SerializedArtifactsByExtensionElement> artifactsByExtensionElements)
        {
            this.extension = extension;
            this.artifactsByExtensionElement = artifactsByExtensionElements;
        }

    }


    public List<SerializedArtifactExtensionResult> values = new ArrayList<>();


    public static ArtifactGenerationExtensionOutput fromFactoryResults(MutableMap<ArtifactGenerationExtension, List<ArtifactGenerationResult>> factoryResults, PureModel pureModel)
    {
        ArtifactGenerationExtensionOutput result = new ArtifactGenerationExtensionOutput();
        for (Map.Entry<ArtifactGenerationExtension, List<ArtifactGenerationResult>> resultByExtension : factoryResults.entrySet())
        {
            ArtifactGenerationExtension extension = resultByExtension.getKey();
            String extensionKey = extension.getKey();
            List<SerializedArtifactsByExtensionElement> extensionResults = resultByExtension.getValue().stream()
                .map(outputs -> processFilePaths(extension, outputs, pureModel)).collect(Collectors.toList());
            result.values.add(new SerializedArtifactExtensionResult(extensionKey, extensionResults));
        }
        return result;
    }


    private static SerializedArtifactsByExtensionElement processFilePaths(ArtifactGenerationExtension extension, ArtifactGenerationResult result, PureModel pureModel)
    {
        Set<String> fileOutputPaths = new HashSet<>();
        PackageableElement generator = result.getElement();
        List<GenerationOutput> preProcessedOuputs = result.getResults();
        String fileSeparator = "/";
        String rootExtensionFolder = extension.getKey();
        List<GenerationOutput> processedOutputs = Lists.mutable.empty();

        String elementFolder = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(generator, fileSeparator);
        for (GenerationOutput output : preProcessedOuputs)
        {
            String fileName = elementFolder + fileSeparator + rootExtensionFolder + fileSeparator + output.getFileName();
            if (!fileOutputPaths.add(fileName))
            {
                throw new EngineException("Duplicate file path found when serializing artifact generation extension  '" + extension.getClass() + "' output: '" + fileName + "'");
            }
            processedOutputs.add(new GenerationOutput(output.getContent(), fileName, output.getFormat()));
        }

        return new SerializedArtifactsByExtensionElement(HelperModelBuilder.getElementFullPath(generator, pureModel.getExecutionSupport()), processedOutputs, extension.getKey());
    }

}
