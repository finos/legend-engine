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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.codecs.pojo.annotations.BsonIgnore;

public class Multiplicity
{
    public static final Multiplicity PURE_ONE = new Multiplicity(1, 1);
    public static final Multiplicity PURE_MANY = new Multiplicity(1, null);

    public int lowerBound;
    private Integer upperBound = Integer.MAX_VALUE;
    private boolean infinite = true;

    public Multiplicity()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public Multiplicity(int lowerBound, Integer upperBound)
    {
        this.lowerBound = lowerBound;
        this.setUpperBound(upperBound);
    }

    public void setUpperBound(Integer upperBound)
    {
        this.upperBound = upperBound;
        this.infinite = upperBound == null || upperBound == Integer.MAX_VALUE;
    }

    public Integer getUpperBound()
    {
        //For Object Mapper (jackson) to return null if upper bound is max value
        return this.infinite ? null : this.upperBound;
    }

    public boolean isUpperBoundEqualTo(Integer i)
    {
        return this.getUpperBound() != null && this.getUpperBound().equals(i);
    }

    public boolean isUpperBoundGreaterThan(Integer i)
    {
        return this.getUpperBound() == null || this.getUpperBound() > i;
    }

    @JsonIgnore
    public Integer getUpperBoundInt()
    {
        return this.getUpperBound() == null ? Integer.MAX_VALUE : this.getUpperBound();
    }

    @JsonIgnore
    @BsonIgnore
    public boolean isInfinite()
    {
        return infinite;
    }
}
