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

package org.finos.legend.engine.protocol.pure.m3.type;

import java.util.Collections;
import java.util.List;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.constraint.Constraint;
import org.finos.legend.engine.protocol.pure.m3.function.property.Property;
import org.finos.legend.engine.protocol.pure.m3.function.property.QualifiedProperty;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;

public class Class extends PackageableElement
{
    public List<PackageableElementPointer> superTypes = Collections.emptyList();
    public List<Property> originalMilestonedProperties = Collections.emptyList();
    public List<Property> properties = Collections.emptyList();
    public List<QualifiedProperty> qualifiedProperties = Collections.emptyList();
    public List<StereotypePtr> stereotypes = Collections.emptyList();
    public List<TaggedValue> taggedValues = Collections.emptyList();
    public List<Constraint> constraints = Collections.emptyList();

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}