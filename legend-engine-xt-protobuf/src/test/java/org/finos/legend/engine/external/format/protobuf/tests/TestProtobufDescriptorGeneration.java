// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.external.format.protobuf.tests;

import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationConfigFromFileGenerationSpecificationBuilder;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
import org.finos.legend.engine.external.format.protobuf.generation.descriptors.ProtobufDescriptorGenerationController;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;

import static org.finos.legend.engine.external.format.protobuf.Utils.getProtocol;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestProtobufDescriptorGeneration
{
    private final ProtobufDescriptorGenerationController protobufDescriptorGenerationController =
        new ProtobufDescriptorGenerationController(new ModelManager(DeploymentMode.TEST));

    @Test
    public void generateDescriptorTest() throws IOException, InterruptedException
    {
        ProtobufGenerationInput protobufGenerationInput = new ProtobufGenerationInput();
        PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
        protobufGenerationInput.model = pureModelContextData;

        FileGenerationSpecification fileGeneration =
            pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
        protobufGenerationInput.config =
            ProtobufGenerationConfigFromFileGenerationSpecificationBuilder.build(fileGeneration);

        byte[] descriptor = protobufDescriptorGenerationController.getProtobufDescriptorGenerationService()
            .generateDescriptor(protobufGenerationInput, FastList.newListWith(new CommonProfile()));

        byte[] etalonDescriptor = FileUtils.readFileToByteArray(FileUtils.getFile(
            "src\\test\\resources\\org\\finos\\legend\\engine\\external\\format\\protobuf\\tests\\descriptor-set.pb"));

        assertThat(descriptor, is(etalonDescriptor));
    }
}
