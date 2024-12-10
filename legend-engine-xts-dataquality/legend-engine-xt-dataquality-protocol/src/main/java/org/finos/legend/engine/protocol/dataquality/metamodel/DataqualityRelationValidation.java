// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.protocol.dataquality.metamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;

import java.util.Collections;
import java.util.List;


//------------------------------------------------------------
// Should be generated out of the Pure protocol specification
//------------------------------------------------------------
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataqualityRelationValidation extends PackageableElement
{
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();

    public Lambda query;
    public List<RelationValidation> validations = Collections.emptyList();
    public PackageableElementPointer runtime;
}
