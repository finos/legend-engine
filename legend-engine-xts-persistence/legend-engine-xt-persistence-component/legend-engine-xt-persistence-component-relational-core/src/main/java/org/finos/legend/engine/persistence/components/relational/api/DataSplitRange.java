// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api;

public class DataSplitRange
{
    private final long lowerBound;
    private final long upperBound;

    public static DataSplitRange of(long lowerBound, long upperBound)
    {
        return new DataSplitRange(lowerBound, upperBound);
    }

    private DataSplitRange(long lowerBound, long upperBound)
    {
        if (lowerBound > upperBound)
        {
            throw new IllegalArgumentException("Lower bound must be less than or equal to upper bound.");
        }

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public long lowerBound()
    {
        return lowerBound;
    }

    public long upperBound()
    {
        return upperBound;
    }
}
