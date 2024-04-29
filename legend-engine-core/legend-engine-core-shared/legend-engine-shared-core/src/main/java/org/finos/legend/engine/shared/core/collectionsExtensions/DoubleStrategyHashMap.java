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

import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;

import java.util.function.BiFunction;
import java.util.function.Function;

public class DoubleStrategyHashMap<K1, V, K2> extends UnifiedMapWithHashingStrategy<K1, V>
{
    protected DoubleHashingStrategy<K1, K2> doubleHashingStrategy;

    @Deprecated
    public DoubleStrategyHashMap()
    {
        // As it extends externalizable
    }

    public DoubleStrategyHashMap(DoubleHashingStrategy<K1, K2> doubleHashingStrategy)
    {
        super(doubleHashingStrategy);
        this.doubleHashingStrategy = doubleHashingStrategy;
    }

    public V getWithFirstKey(K1 key)
    {
        return this.get(key);
    }

    public V getWithSecondKey(K2 key)
    {
        return this.getWithSecondKeyInternal(key);
    }

    public void switchSecondKeyHashingStrategy(Function<K2, Integer> secondKeyHashCodeFunction, BiFunction<K1, K2, Boolean> heterogeneousEqualityFunction)
    {
        this.doubleHashingStrategy.switchSecondKeyHashingStrategy(secondKeyHashCodeFunction, heterogeneousEqualityFunction);
    }

    private int secondKeyIndex(K2 key)
    {
        int h = this.doubleHashingStrategy.computeSecondKeyHashCode(key);
        h ^= h >>> 20 ^ h >>> 12;
        h ^= h >>> 7 ^ h >>> 4;
        return (h & (this.table.length >> 1) - 1) << 1;
    }

    private V getWithSecondKeyInternal(K2 key)
    {
        int index = this.secondKeyIndex(key);
        Object cur = this.table[index];
        if (cur != null)
        {
            Object val = this.table[index + 1];
            if (cur == CHAINED_KEY)
            {
                return this.getWithSecondKeyFromChain((Object[]) val, key);
            }
            if (this.nonNullTableObjectHeterogeneousEquals(cur, key))
            {
                return (V) val;
            }
        }
        return null;
    }

    private V getWithSecondKeyFromChain(Object[] chain, K2 key)
    {
        for (int i = 0; i < chain.length; i += 2)
        {
            Object k = chain[i];
            if (k == null)
            {
                return null;
            }
            if (this.nonNullTableObjectHeterogeneousEquals(k, key))
            {
                return (V) chain[i + 1];
            }
        }
        return null;
    }

    private boolean nonNullTableObjectHeterogeneousEquals(Object cur, K2 key)
    {
        return cur == key || (cur == NULL_KEY ? key == null : this.doubleHashingStrategy.heterogeneousEquals(this.nonSentinelKey(cur), key));
    }

    private K1 nonSentinelKey(Object key)
    {
        return key == NULL_KEY ? null : (K1) key;
    }
}
