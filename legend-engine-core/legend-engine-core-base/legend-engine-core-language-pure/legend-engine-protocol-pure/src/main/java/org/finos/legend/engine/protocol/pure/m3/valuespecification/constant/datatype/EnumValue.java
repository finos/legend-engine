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

package org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.datatype;

import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecificationVisitor;

import java.util.Objects;

public class EnumValue extends DataTypeValueSpecification
{
    public String fullPath;
    public String value;

    public EnumValue()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public EnumValue(String fullPath, String value)
    {
        this.fullPath = fullPath;
        this.value = value;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof EnumValue))
        {
            return false;
        }
        EnumValue enumValue = (EnumValue) o;
        return Objects.equals(fullPath, enumValue.fullPath) && Objects.equals(value, enumValue.value);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(fullPath, value);
    }
}