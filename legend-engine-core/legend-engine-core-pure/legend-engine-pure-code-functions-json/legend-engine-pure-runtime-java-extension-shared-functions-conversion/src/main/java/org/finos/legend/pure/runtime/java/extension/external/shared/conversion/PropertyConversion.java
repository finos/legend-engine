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

package org.finos.legend.pure.runtime.java.extension.external.shared.conversion;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;

public abstract class PropertyConversion<F, T> implements Conversion<F, T>
{
    protected final AbstractProperty property;
    protected final String name;
    protected final boolean isFromAssociation;
    protected final Conversion conversion;
    protected final Type type;

    public PropertyConversion(AbstractProperty property, boolean isFromAssociation, Conversion conversion, Type type)
    {
        this.property = property;
        this.name = property.getName();
        this.isFromAssociation = isFromAssociation;
        this.conversion = conversion;
        this.type = type;
    }

    public AbstractProperty getProperty()
    {
        return this.property;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean isFromAssociation()
    {
        return this.isFromAssociation;
    }

    @Override
    public String pureTypeAsString()
    {
        return this.conversion != null ? this.conversion.pureTypeAsString() : null;
    }
}
