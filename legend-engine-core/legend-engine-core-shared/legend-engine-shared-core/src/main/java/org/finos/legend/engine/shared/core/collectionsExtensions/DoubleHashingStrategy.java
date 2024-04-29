// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.shared.core.collectionsExtensions;

import org.eclipse.collections.api.block.HashingStrategy;

import java.util.function.BiFunction;
import java.util.function.Function;

public class DoubleHashingStrategy<K1, K2> implements HashingStrategy<K1>
{
    private Function<K1, Integer> firstKeyHashCodeFunction;
    private BiFunction<K1, K1, Boolean> firstKeyEqualityFunction;
    private Function<K2, Integer> secondKeyHashCodeFunction;
    private BiFunction<K1, K2, Boolean> heterogeneousEqualityFunction;

    public DoubleHashingStrategy(
            Function<K1, Integer> firstKeyHashCodeFunction,
            BiFunction<K1, K1, Boolean> firstKeyEqualityFunction,
            Function<K2, Integer> secondKeyHashCodeFunction,
            BiFunction<K1, K2, Boolean> heterogeneousEqualityFunction
    )
    {
        this.firstKeyHashCodeFunction = firstKeyHashCodeFunction;
        this.firstKeyEqualityFunction = firstKeyEqualityFunction;
        this.secondKeyHashCodeFunction = secondKeyHashCodeFunction;
        this.heterogeneousEqualityFunction = heterogeneousEqualityFunction;
    }

    @Override
    public int computeHashCode(K1 object)
    {
        return this.firstKeyHashCodeFunction.apply(object);
    }

    @Override
    public boolean equals(K1 o1, K1 o2)
    {
        return this.firstKeyEqualityFunction.apply(o1, o2);
    }

    public int computeSecondKeyHashCode(K2 object)
    {
        return this.secondKeyHashCodeFunction.apply(object);
    }

    public boolean heterogeneousEquals(K1 o1, K2 o2)
    {
        return this.heterogeneousEqualityFunction.apply(o1, o2);
    }

    public void switchSecondKeyHashingStrategy(Function<K2, Integer> secondKeyHashCodeFunction, BiFunction<K1, K2, Boolean> heterogeneousEqualityFunction)
    {
        this.secondKeyHashCodeFunction = secondKeyHashCodeFunction;
        this.heterogeneousEqualityFunction = heterogeneousEqualityFunction;
    }
}
