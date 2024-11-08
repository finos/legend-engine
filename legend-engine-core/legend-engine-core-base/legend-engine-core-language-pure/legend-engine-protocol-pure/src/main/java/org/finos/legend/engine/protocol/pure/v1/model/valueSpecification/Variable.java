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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.relationType.RelationType;

public class Variable extends ValueSpecification
{
    public String name;
    public Multiplicity multiplicity;

    // Type can be either a Class or a RelationType
    @JsonProperty(value = "class")
    @JsonSerialize(converter = PackageableElementPointer.ToPathSerializerConverter.class)
    public PackageableElementPointer _class;
    public RelationType relationType;

    public Boolean supportsStream;

    public Variable()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public Variable(String name, String _class, Multiplicity multiplicity)
    {
        this.name = name;
        this._class = new PackageableElementPointer(PackageableElementType.CLASS, _class);
        this.multiplicity = multiplicity;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
