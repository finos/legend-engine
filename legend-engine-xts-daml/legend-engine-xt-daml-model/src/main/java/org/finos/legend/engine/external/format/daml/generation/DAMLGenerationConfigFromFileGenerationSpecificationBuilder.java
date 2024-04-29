// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.daml.generation;

import org.finos.legend.engine.language.pure.dsl.generation.config.ConfigBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;

@Deprecated
public class DAMLGenerationConfigFromFileGenerationSpecificationBuilder
{
    public static DAMLGenerationConfig build(FileGenerationSpecification fileGeneration)
    {
        Assert.assertTrue(fileGeneration.type.equals("daml"), () -> "File generation of type of daml expected, got '" + fileGeneration.type + "'");
        DAMLGenerationConfig damlConfig = new DAMLGenerationConfig();
        ConfigBuilder.duplicateCheck(fileGeneration.configurationProperties);
        ConfigBuilder.setScopeElements(fileGeneration, damlConfig);
        fileGeneration.configurationProperties.forEach(e -> ConfigBuilder.setConfigurationProperty(fileGeneration, e, damlConfig));
        return damlConfig;
    }
}
