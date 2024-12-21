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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.datatype;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

import java.util.Objects;

public class UnitInstance extends DataTypeValueSpecification
{
    public String unitType;
    public Number unitValue;

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof UnitInstance))
        {
            return false;
        }
        UnitInstance that = (UnitInstance) o;
        return Objects.equals(unitType, that.unitType) && Objects.equals(unitValue, that.unitValue);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(unitType, unitValue);
    }
}
