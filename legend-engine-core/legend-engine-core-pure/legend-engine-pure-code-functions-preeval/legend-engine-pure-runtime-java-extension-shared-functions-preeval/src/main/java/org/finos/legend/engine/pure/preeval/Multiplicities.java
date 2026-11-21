// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.pure.preeval;

import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;

public final class Multiplicities
{
    private Multiplicities()
    {
    }

    public static boolean hasLowerBound(PrevalRuntime runtime, Multiplicity multiplicity)
    {
        Long lowerBound = runtime.lowerBound(multiplicity);
        // parity: Pure's hasLowerBound treats a lower bound of 0 as absent
        return lowerBound != null && lowerBound != 0L;
    }

    public static boolean hasUpperBound(PrevalRuntime runtime, Multiplicity multiplicity)
    {
        Long upperBound = runtime.upperBound(multiplicity);
        return upperBound != null && upperBound != -1L;
    }

    public static boolean isToOne(PrevalRuntime runtime, Multiplicity multiplicity)
    {
        return hasUpperBound(runtime, multiplicity) && runtime.upperBound(multiplicity) == 1L && Long.valueOf(1L).equals(runtime.lowerBound(multiplicity));
    }
}
