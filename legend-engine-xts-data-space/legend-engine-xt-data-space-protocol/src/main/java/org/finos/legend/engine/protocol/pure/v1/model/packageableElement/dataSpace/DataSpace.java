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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.domain.TaggedValue;

import java.util.Collections;
import java.util.List;

// NOTE: since we deprecated and removed fields such as artifactId, groupId, versionId,
// we need this flag to be backward compatible once we can be sure that those fields are eradicated,
// we will be able to take this out
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataSpace extends PackageableElement
{
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();

    public String title;
    public String description;

    public List<DataSpaceExecutionContext> executionContexts;
    public String defaultExecutionContext;

    // For now, these can only include classes, enumerations, associations
    public List<DataSpaceElementPointer> elements;
    public List<DataSpaceExecutable> executables;
    public List<DataSpaceDiagram> diagrams;

    public DataSpaceSupportInfo supportInfo;

    @Deprecated
    // NOTE: this field will be deprecated, we should consider adding this to the list of diagrams during deserialization phase
    public List<PackageableElementPointer> featuredDiagrams;

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
