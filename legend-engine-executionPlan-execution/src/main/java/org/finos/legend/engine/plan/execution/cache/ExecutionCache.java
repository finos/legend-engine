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

package org.finos.legend.engine.plan.execution.cache;

import java.util.Map;
import java.util.concurrent.Callable;

public interface ExecutionCache<K, V>
{
    V get(K key, Callable<? extends V> valueLoader);

    V getIfPresent(K key);

    Map<? extends K, ? extends V> getAllPresent(Iterable<? extends K> keys);

    void put(K key, V value);

    void putAll(Map<? extends K, ? extends V> keyValues);

    void invalidate(K key);

    void invalidateAll(Iterable<? extends K> keys);

    void invalidateAll();

    long estimatedSize();

    ExecutionCacheStats stats();
}
