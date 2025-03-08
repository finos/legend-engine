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

package org.finos.legend.engine.protocol.pure.m3.valuespecification;

import org.finos.legend.engine.protocol.pure.m3.multiplicity.Multiplicity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Collection extends Many
{
    public List<ValueSpecification> values = Collections.emptyList();

    public Collection()
    {
        this.multiplicity = new Multiplicity(0, 0);
    }

    public Collection(List<ValueSpecification> values)
    {
        this.values = values;
        this.multiplicity = new Multiplicity(values.size(), values.size());
    }

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Collection))
        {
            return false;
        }
        Collection that = (Collection) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(values);
    }
}
