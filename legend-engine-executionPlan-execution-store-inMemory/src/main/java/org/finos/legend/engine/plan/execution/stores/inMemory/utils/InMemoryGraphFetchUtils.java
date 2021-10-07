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

package org.finos.legend.engine.plan.execution.stores.inMemory.utils;

import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleHashingStrategy;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class InMemoryGraphFetchUtils
{
    private final static Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, Function<Object, Integer>> PARENT_OBJECT_KEYS_HASHING_FUNCTION =
            (nodeSpecifics) -> (obj) -> hashWithParent(obj, nodeSpecifics);

    private final static Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, BiFunction<Object, Object, Boolean>> PARENT_OBJECT_KEYS_EQUALITY_FUNCTION =
            (nodeSpecifics) -> (obj1, obj2) -> equalsWithParent(obj1, obj2, nodeSpecifics);

    private final static Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, Function<Object, Integer>> CHILD_OBJECT_KEYS_HASHING_FUNCTION =
            (nodeSpecifics) -> (obj) -> hashWithChild(obj, nodeSpecifics);

    private final static Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, BiFunction<Object, Object, Boolean>> OBJECT_SQL_RESULT_HETEROGENEOUS_EQUALS_FUNCTION =
            (nodeSpecifics) -> (obj1, obj2) -> heterogeneousEqualsParentAndChildObject(obj1, obj2, nodeSpecifics);

    public static DoubleHashingStrategy<Object, Object> parentChildDoubleHashStrategy(IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        return new DoubleHashingStrategy<>(
                InMemoryGraphFetchUtils.PARENT_OBJECT_KEYS_HASHING_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.PARENT_OBJECT_KEYS_EQUALITY_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.CHILD_OBJECT_KEYS_HASHING_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.OBJECT_SQL_RESULT_HETEROGENEOUS_EQUALS_FUNCTION.apply(nodeSpecifics)
        );
    }

    private static int hashWithParent(Object object, IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        try
        {
            int hash = 0;
            int mul = 1;
            for (Map.Entry<String, Object> entry : nodeSpecifics.getCrossStoreKeysValueForChildren(object).entrySet())
            {
                int hashCode = entry.getKey().hashCode() + (entry.getValue() == null ? -1 : entry.getValue().hashCode());
                hash = hash + mul * hashCode;
                mul = mul * 29;
            }
            return hash;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean equalsWithParent(Object obj1, Object obj2, IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        try
        {
            if (obj1 == obj2)
            {
                return true;
            }
            if (obj1 == null || obj2 == null)
            {
                return false;
            }

            Map<String, Object> m1 = nodeSpecifics.getCrossStoreKeysValueForChildren(obj1);
            Map<String, Object> m2 = nodeSpecifics.getCrossStoreKeysValueForChildren(obj2);

            return Objects.equals(m1, m2);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static int hashWithChild(Object object, IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        try
        {
            int hash = 0;
            int mul = 1;
            for (Map.Entry<String, Object> entry : nodeSpecifics.getCrossStoreKeysValueFromChild(object).entrySet())
            {
                int hashCode = entry.getKey().hashCode() + (entry.getValue() == null ? -1 : entry.getValue().hashCode());
                hash = hash + mul * hashCode;
                mul = mul * 29;
            }
            return hash;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean heterogeneousEqualsParentAndChildObject(Object obj1, Object obj2, IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        try
        {
            if (obj1 == obj2)
            {
                return true;
            }
            if (obj1 == null || obj2 == null)
            {
                return false;
            }

            Map<String, Object> m1 = nodeSpecifics.getCrossStoreKeysValueForChildren(obj1);
            Map<String, Object> m2 = nodeSpecifics.getCrossStoreKeysValueFromChild(obj2);

            return Objects.equals(m1, m2);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
