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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generation;

import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;

public class SchemaGeneration extends ArtifactGenerationSpecification
{

    public String format;
    public ModelUnit modelUnit;
    public ModelToSchemaConfiguration modelToSchemaConfiguration;

    public SourceInformation sourceInformation;


    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return null;
    }
}
