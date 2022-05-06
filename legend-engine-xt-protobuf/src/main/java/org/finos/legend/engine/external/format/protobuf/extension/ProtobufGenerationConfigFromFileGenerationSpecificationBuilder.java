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

package org.finos.legend.engine.external.format.protobuf.extension;

import org.finos.legend.engine.external.format.protobuf.schema.generations.ProtobufGenerationConfig;
import org.finos.legend.engine.language.pure.dsl.generation.config.ConfigBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationType;
import org.finos.legend.engine.shared.core.operational.Assert;

public class ProtobufGenerationConfigFromFileGenerationSpecificationBuilder
{
    public static ProtobufGenerationConfig build(FileGenerationSpecification fileGeneration)
    {
        Assert.assertTrue(fileGeneration.type.equals(FileGenerationType.protobuf.name()), () -> "File generation of type of protobuf expected, got '" + fileGeneration.type + "'");
        ProtobufGenerationConfig protobufConfig = new ProtobufGenerationConfig();
        ConfigBuilder.noConfigurationPropertiesCheck(fileGeneration);
        ConfigBuilder.setScopeElements(fileGeneration, protobufConfig);
        return protobufConfig;
    }
}
