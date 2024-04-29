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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration;

import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.AbstractGenerationSpecification;

import java.util.Collections;
import java.util.List;

public class FileGenerationSpecification extends AbstractGenerationSpecification
{
    public String type;
    public List<String> scopeElements = Collections.emptyList();
    public List<ConfigurationProperty> configurationProperties = Collections.emptyList();
    public SourceInformation typeSourceInformation;
    public String generationOutputPath;

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
