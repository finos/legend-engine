// Copyright 2023 Goldman Sachs
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

import java.util.Arrays;
import java.util.Objects;

public class CByteArray extends PrimitiveValueSpecification
{
    public byte[] value;

    public CByteArray()
    {
    }

    public CByteArray(byte[] byteArray)
    {
        this.value = byteArray;
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof CByteArray))
        {
            return false;
        }
        CByteArray that = (CByteArray) o;
        return Objects.deepEquals(value, that.value);
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(value);
    }
}