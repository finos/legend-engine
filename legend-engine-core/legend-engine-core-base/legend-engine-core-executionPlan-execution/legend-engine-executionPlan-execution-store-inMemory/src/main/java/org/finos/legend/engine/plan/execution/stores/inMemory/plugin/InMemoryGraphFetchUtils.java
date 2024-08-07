// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.inMemory.plugin;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch.IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleHashingStrategy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class InMemoryGraphFetchUtils
{
    private static final Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, Function<Object, Integer>> PARENT_OBJECT_KEYS_HASHING_FUNCTION =
            (nodeSpecifics) -> (obj) -> hashWithParent(obj, nodeSpecifics);

    private static final Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, BiFunction<Object, Object, Boolean>> PARENT_OBJECT_KEYS_EQUALITY_FUNCTION =
            (nodeSpecifics) -> (obj1, obj2) -> equalsWithParent(obj1, obj2, nodeSpecifics);

    private static final Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, Function<Object, Integer>> CHILD_OBJECT_KEYS_HASHING_FUNCTION =
            (nodeSpecifics) -> (obj) -> hashWithChild(obj, nodeSpecifics);

    private static final Function<IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics, BiFunction<Object, Object, Boolean>> PARENT_CHILD_OBJECT_HETEROGENEOUS_EQUALS_FUNCTION =
            (nodeSpecifics) -> (obj1, obj2) -> heterogeneousEqualsParentAndChildObject(obj1, obj2, nodeSpecifics);

    public static DoubleHashingStrategy<Object, Object> parentChildDoubleHashStrategy(IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics nodeSpecifics)
    {
        return new DoubleHashingStrategy<>(
                InMemoryGraphFetchUtils.PARENT_OBJECT_KEYS_HASHING_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.PARENT_OBJECT_KEYS_EQUALITY_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.CHILD_OBJECT_KEYS_HASHING_FUNCTION.apply(nodeSpecifics),
                InMemoryGraphFetchUtils.PARENT_CHILD_OBJECT_HETEROGENEOUS_EQUALS_FUNCTION.apply(nodeSpecifics)
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

    // note: the below is modeled off of RelationalCrossObjectGraphFetchCacheKey - in future, can consider if refactoring is possible
    static class InMemoryCrossObjectGraphFetchCacheKey extends GraphFetchCacheKey
    {
        private static final long serialVersionUID = -2846528436743134709L;
        Object inMemoryObject;
        List<Method> keyGetters;
        List<Object> values;

        InMemoryCrossObjectGraphFetchCacheKey(Object inMemoryObject, List<Method> keyGetters)
        {
            this.inMemoryObject = inMemoryObject;
            this.keyGetters = keyGetters;
        }

        @Override
        public String getStringIdentifier()
        {
            return this.getValues().stream().map(v -> v == null ? "NULL" : v.toString()).collect(Collectors.joining("|", "InMemoryCrossObjectGraphFetchCacheKey{", "}"));
        }

        @Override
        protected int hash()
        {
            return this.values != null ? hashWithValues(this.values) : hashWithKeys(this.inMemoryObject, this.keyGetters);
        }

        @Override
        protected boolean equivalent(Object other)
        {
            if (other instanceof InMemoryCrossObjectGraphFetchCacheKey)
            {
                InMemoryCrossObjectGraphFetchCacheKey that = (InMemoryCrossObjectGraphFetchCacheKey) other;
                return this.values != null ?
                        (that.values != null ? equalsWithValues(this.values, that.values) : equalsWithKeysAndValues(that.inMemoryObject, that.keyGetters, this.values)) :
                        (that.values != null ? equalsWithKeysAndValues(this.inMemoryObject, this.keyGetters, that.values) : equalsWithDifferentKeys(this.inMemoryObject, that.inMemoryObject, this.keyGetters, that.keyGetters));
            }
            return false;
        }

        private List<Object> getValues()
        {
            if (this.values == null)
            {
                this.values = new ArrayList<>();
                try
                {
                    for (Method getter : this.keyGetters)
                    {
                        this.values.add(getter.invoke(resolveValueIfIChecked(this.inMemoryObject)));
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
            return this.values;
        }
    }

    private static int hashWithKeys(Object obj, List<Method> getters)
    {
        try
        {
            int hash = 0;
            int mul = 1;
            for (Method getter : getters)
            {
                Object val = getter.invoke(resolveValueIfIChecked(obj));
                hash = hash + mul * (val == null ? -1 : val.hashCode());
                mul = mul * 29;
            }
            return hash;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static int hashWithValues(List<Object> values)
    {
        int hash = 0;
        int mul = 1;
        for (Object val : values)
        {
            hash = hash + mul * (val == null ? -1 : val.hashCode());
            mul = mul * 29;
        }
        return hash;
    }

    private static boolean equalsWithDifferentKeys(Object obj1, Object obj2, List<Method> getters1, List<Method> getters2)
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

            int i = 0;
            for (Method getter1 : getters1)
            {
                Object obj1Val = getter1.invoke(obj1);
                Object obj2Val = getters2.get(i).invoke(obj2);
                if (!Objects.equals(obj1Val, obj2Val))
                {
                    return false;
                }
                i++;
            }
            return true;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static boolean equalsWithValues(List<Object> values1, List<Object> values2)
    {
        int i = 0;
        for (Object val1 : values1)
        {
            Object val2 = values2.get(i);
            if (!Objects.equals(val1, val2))
            {
                return false;
            }
            i++;
        }
        return true;
    }

    private static boolean equalsWithKeysAndValues(Object obj, List<Method> getters, List<Object> values)
    {
        try
        {
            int i = 0;
            for (Method getter : getters)
            {
                Object obj1Val = getter.invoke(resolveValueIfIChecked(obj));
                Object obj2Val = values.get(i);
                if (!Objects.equals(obj1Val, obj2Val))
                {
                    return false;
                }
                i++;
            }
            return true;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected static Object resolveValueIfIChecked(Object obj)
    {
        return obj instanceof IChecked ? ((IChecked<?>) obj).getValue() : obj;
    }
}
