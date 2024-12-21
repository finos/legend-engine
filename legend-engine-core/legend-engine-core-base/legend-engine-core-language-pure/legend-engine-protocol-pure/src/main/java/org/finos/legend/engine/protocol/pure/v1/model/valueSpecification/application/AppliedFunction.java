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

package org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecificationVisitor;

public class AppliedFunction extends AbstractAppliedFunction
{
    public String function;
    public String fControl;
    public List<ValueSpecification> parameters = Collections.emptyList();

    @Override
    public <T> T accept(ValueSpecificationVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof AppliedFunction))
        {
            return false;
        }
        AppliedFunction that = (AppliedFunction) o;
        return Objects.equals(function, that.function) && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(function, parameters);
    }
}
