// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.context;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Objects;

public class PureModelContextPointerCombination extends PureModelContext
{
    public List<PureModelContextPointer> pointers = Collections.emptyList();

    @JsonIgnore
    private Set<PureModelContextPointer> pointerSet;

    private Set<PureModelContextPointer> getPointerSet()
    {
        if (this.pointerSet == null)
        {
            this.pointerSet = Collections.unmodifiableSet(
                    new LinkedHashSet<>(this.pointers)
            );
        }
        return this.pointerSet;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        PureModelContextPointerCombination that =
                (PureModelContextPointerCombination) o;

        return Objects.equals(this.getPointerSet(), that.getPointerSet());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.getPointerSet());
    }
}
