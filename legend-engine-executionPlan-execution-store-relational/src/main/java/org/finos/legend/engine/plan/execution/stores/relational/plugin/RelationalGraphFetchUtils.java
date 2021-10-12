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

package org.finos.legend.engine.plan.execution.stores.relational.plugin;

import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByEqualityKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByTargetCrossKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CBoolean;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDateTime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CDecimal;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CFloat;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CStrictDate;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CString;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Collection;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.EnumValue;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.GraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleHashingStrategy;
import org.finos.legend.engine.shared.core.collectionsExtensions.DoubleStrategyHashMap;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class RelationalGraphFetchUtils
{
    private final static Function<List<Method>, Function<Object, Integer>> OBJECT_KEYS_HASHING_FUNCTION =
            (getters) -> (obj) -> hashWithKeys(obj, getters);

    private final static Function<List<Method>, BiFunction<Object, Object, Boolean>> OBJECT_KEYS_EQUALITY_FUNCTION =
            (getters) -> (obj1, obj2) -> equalsWithKeys(obj1, obj2, getters);

    private final static Function<List<Integer>, Function<SQLExecutionResult, Integer>> SQL_RESULT_INDICES_HASHING_FUNCTION =
            (indices) -> (obj) -> hashSQLResultWithKeyIndices(obj, indices);

    private final static BiFunction<List<Method>, List<Integer>, BiFunction<Object, SQLExecutionResult, Boolean>> OBJECT_SQL_RESULT_HETEROGENEOUS_EQUALS_FUNCTION =
            (getters, indices) -> (object, sqlResult) -> heterogeneousEqualsObjectAndSQLResult(object, sqlResult, getters, indices);


    static class RelationalObjectGraphFetchCacheKey extends GraphFetchCacheKey
    {
        Object relationalObject;
        List<Method> keyGetters;

        RelationalObjectGraphFetchCacheKey(Object relationalObject, List<Method> keyGetters)
        {
            this.relationalObject = relationalObject;
            this.keyGetters = keyGetters;
        }

        @Override
        protected int hash()
        {
            return hashWithKeys(this.relationalObject, this.keyGetters);
        }

        @Override
        protected boolean equivalent(Object other)
        {
            if (other instanceof RelationalObjectGraphFetchCacheKey)
            {
                return equalsWithKeys(this.relationalObject, ((RelationalObjectGraphFetchCacheKey) other).relationalObject, this.keyGetters);
            }
            if (other instanceof RelationalSQLResultGraphFetchCacheKey)
            {
                RelationalSQLResultGraphFetchCacheKey that = (RelationalSQLResultGraphFetchCacheKey) other;
                return heterogeneousEqualsObjectAndSQLResult(this.relationalObject, that.sqlExecutionResult, this.keyGetters, that.pkIndices);
            }
            return false;
        }
    }

    static class RelationalSQLResultGraphFetchCacheKey extends GraphFetchCacheKey
    {
        SQLExecutionResult sqlExecutionResult;
        List<Integer> pkIndices;

        RelationalSQLResultGraphFetchCacheKey(SQLExecutionResult sqlExecutionResult, List<Integer> pkIndices)
        {
            this.sqlExecutionResult = sqlExecutionResult;
            this.pkIndices = pkIndices;
        }

        @Override
        protected int hash()
        {
            return hashSQLResultWithKeyIndices(this.sqlExecutionResult, this.pkIndices);
        }

        @Override
        public boolean equals(Object other) //NOSONAR hashCode is properly implemented
        {
            if (!(other instanceof GraphFetchCacheKey))
            {
                return false;
            }
            return this.equivalent(other);
        }

        @Override
        protected boolean equivalent(Object other)
        {
            if (other instanceof RelationalObjectGraphFetchCacheKey)
            {
                RelationalObjectGraphFetchCacheKey that = (RelationalObjectGraphFetchCacheKey) other;
                return heterogeneousEqualsObjectAndSQLResult(
                        that.relationalObject, this.sqlExecutionResult,
                        that.keyGetters, this.pkIndices
                );
            }
            return false;
        }
    }

    static class RelationalCrossObjectGraphFetchCacheKey extends GraphFetchCacheKey
    {
        Object relationalObject;
        List<Method> keyGetters;

        RelationalCrossObjectGraphFetchCacheKey(Object relationalObject, List<Method> keyGetters)
        {
            this.relationalObject = relationalObject;
            this.keyGetters = keyGetters;
        }

        @Override
        protected int hash()
        {
            return hashWithKeys(this.relationalObject, this.keyGetters);
        }

        @Override
        protected boolean equivalent(Object other)
        {
            if (other instanceof RelationalCrossObjectGraphFetchCacheKey)
            {
                RelationalCrossObjectGraphFetchCacheKey that = (RelationalCrossObjectGraphFetchCacheKey) other;
                return equalsWithDifferentKeys(this.relationalObject, that.relationalObject, this.keyGetters, that.keyGetters);
            }
            return false;
        }
    }

    static DoubleHashingStrategy<Object, SQLExecutionResult> objectSQLResultDoubleHashStrategy(List<Method> keyGetters, List<Integer> keyIndices)
    {
        return new DoubleHashingStrategy<>(
                RelationalGraphFetchUtils.OBJECT_KEYS_HASHING_FUNCTION.apply(keyGetters),
                RelationalGraphFetchUtils.OBJECT_KEYS_EQUALITY_FUNCTION.apply(keyGetters),
                RelationalGraphFetchUtils.SQL_RESULT_INDICES_HASHING_FUNCTION.apply(keyIndices),
                RelationalGraphFetchUtils.OBJECT_SQL_RESULT_HETEROGENEOUS_EQUALS_FUNCTION.apply(keyGetters, keyIndices)
        );
    }

    static DoubleHashingStrategy<Object, SQLExecutionResult> objectSQLResultDoubleHashStrategyWithEmptySecondStrategy(List<Method> keyGetters)
    {
        return new DoubleHashingStrategy<>(
                RelationalGraphFetchUtils.OBJECT_KEYS_HASHING_FUNCTION.apply(keyGetters),
                RelationalGraphFetchUtils.OBJECT_KEYS_EQUALITY_FUNCTION.apply(keyGetters),
                (SQLExecutionResult secondKey) -> -1,
                (Object firstKey, SQLExecutionResult sqlExecutionResult) -> false
        );
    }

    static void switchSecondKeyHashingStrategy(DoubleStrategyHashMap<Object, Object, SQLExecutionResult> hashMap, List<Method> keyGetters, List<Integer> keyIndices)
    {
        hashMap.switchSecondKeyHashingStrategy(
                SQL_RESULT_INDICES_HASHING_FUNCTION.apply(keyIndices),
                OBJECT_SQL_RESULT_HETEROGENEOUS_EQUALS_FUNCTION.apply(keyGetters, keyIndices)
        );
    }

    static GraphFetchCacheByEqualityKeys findCacheByEqualityKeys(GraphFetchTree graphFetchTree, String mappingId, String instanceSetId, List<GraphFetchCache> graphFetchCaches)
    {
        if (!subTreeValidForCaching(graphFetchTree))
        {
            return null;
        }

        String subTree = getSubTreeString(graphFetchTree);

        GraphFetchCacheByEqualityKeys matchingUtilizedCache = null;
        for (GraphFetchCache c : graphFetchCaches)
        {
            if (c instanceof GraphFetchCacheByEqualityKeys)
            {
                GraphFetchCacheByEqualityKeys ce = (GraphFetchCacheByEqualityKeys) c;
                if (ce.isCacheUtilized() && mappingId.equals(ce.getMappingId()) && instanceSetId.equals(ce.getInstanceSetId()) && subTree.equals(ce.getSubTree()))
                {
                    matchingUtilizedCache = ce;
                    break;
                }
            }
        }

        if (matchingUtilizedCache == null)
        {
            GraphFetchCacheByEqualityKeys unUtilizedCache = null;
            for (GraphFetchCache c : graphFetchCaches)
            {
                if (c instanceof GraphFetchCacheByEqualityKeys)
                {
                    GraphFetchCacheByEqualityKeys ce = (GraphFetchCacheByEqualityKeys) c;
                    if (!ce.isCacheUtilized() && mappingId.equals(ce.getMappingId()) && instanceSetId.equals(ce.getInstanceSetId()))
                    {
                        unUtilizedCache = ce;
                        break;
                    }
                }
            }

            if (unUtilizedCache != null)
            {
                unUtilizedCache.setSubTree(subTree);
                return unUtilizedCache;
            }

            return null;
        }

        return matchingUtilizedCache;
    }

    static GraphFetchCacheByTargetCrossKeys findCacheByCrossKeys(GraphFetchTree graphFetchTree, String mappingId, String sourceInstanceSetId, String targetInstanceSetId, List<String> targetPropertiesOrdered, List<GraphFetchCache> graphFetchCaches)
    {
        if (!subTreeValidForCaching(graphFetchTree))
        {
            return null;
        }

        String subTree = getSubTreeString(graphFetchTree);
        Pair<String, String> mappingSetIdPair = Tuples.pair(mappingId, sourceInstanceSetId);

        GraphFetchCacheByTargetCrossKeys matchingUtilizedCache = null;
        for (GraphFetchCache c : graphFetchCaches)
        {
            if (c instanceof GraphFetchCacheByTargetCrossKeys)
            {
                GraphFetchCacheByTargetCrossKeys ce = (GraphFetchCacheByTargetCrossKeys) c;
                if (ce.isCacheUtilized() && ce.getSourceSetIds().contains(mappingSetIdPair) && targetInstanceSetId.equals(ce.getTargetSetId()) && targetPropertiesOrdered.equals(ce.getTargetPropertiesOrdered()) && subTree.equals(ce.getSubTree()))
                {
                    matchingUtilizedCache = ce;
                    break;
                }
            }
        }

        if (matchingUtilizedCache == null)
        {
            GraphFetchCacheByTargetCrossKeys unUtilizedCache = null;
            for (GraphFetchCache c : graphFetchCaches)
            {
                if (c instanceof GraphFetchCacheByTargetCrossKeys)
                {
                    GraphFetchCacheByTargetCrossKeys ce = (GraphFetchCacheByTargetCrossKeys) c;
                    if (!ce.isCacheUtilized() && ce.getSourceSetIds().contains(mappingSetIdPair) && targetInstanceSetId.equals(ce.getTargetSetId()))
                    {
                        unUtilizedCache = ce;
                        break;
                    }
                }
            }

            if (unUtilizedCache != null)
            {
                unUtilizedCache.setSubTree(subTree);
                unUtilizedCache.setTargetPropertiesOrdered(targetPropertiesOrdered);
                return unUtilizedCache;
            }

            return null;
        }

        return matchingUtilizedCache;
    }

    private static int hashWithKeys(Object obj, List<Method> getters)
    {
        try
        {
            int hash = 0;
            int mul = 1;
            for (Method getter : getters)
            {
                Object val = getter.invoke(obj);
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

    private static boolean equalsWithKeys(Object obj1, Object obj2, List<Method> getters)
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

            for (Method getter : getters)
            {
                Object obj1Val = getter.invoke(obj1);
                Object obj2Val = getter.invoke(obj2);
                if (!Objects.equals(obj1Val, obj2Val))
                {
                    return false;
                }
            }
            return true;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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

    private static int hashSQLResultWithKeyIndices(SQLExecutionResult sqlExecutionResult, List<Integer> indices)
    {
        try
        {
            int hash = 0;
            int mul = 1;
            for (int index : indices)
            {
                Object val = sqlExecutionResult.getTransformedValue(index);
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

    private static boolean heterogeneousEqualsObjectAndSQLResult(Object object, SQLExecutionResult sqlResult, List<Method> getters, List<Integer> indices)
    {
        try
        {
            int i = 0;
            for (int index : indices)
            {
                Object thisVal = sqlResult.getTransformedValue(index);
                Object thatVal = getters.get(i).invoke(object);
                if (!Objects.equals(thisVal, thatVal))
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

    static boolean subTreeValidForCaching(GraphFetchTree graphFetchTree)
    {
        boolean currentValidity = true;
        if (graphFetchTree instanceof PropertyGraphFetchTree)
        {
            PropertyGraphFetchTree propertyGraphFetchTree = (PropertyGraphFetchTree) graphFetchTree;
            if (propertyGraphFetchTree.parameters != null && !propertyGraphFetchTree.parameters.isEmpty())
            {
                currentValidity = propertyGraphFetchTree.parameters.stream().allMatch(param ->
                        param instanceof CBoolean ||
                                param instanceof CInteger ||
                                param instanceof CFloat ||
                                param instanceof CDecimal ||
                                param instanceof CString ||
                                param instanceof CStrictDate ||
                                param instanceof CDateTime ||
                                param instanceof EnumValue ||
                                (param instanceof Collection && ((Collection) param).values.stream().allMatch(x -> x instanceof EnumValue))
                );
            }
        }

        boolean childrenValidity = graphFetchTree.subTrees == null || graphFetchTree.subTrees.isEmpty() ||
                graphFetchTree.subTrees.stream().allMatch(RelationalGraphFetchUtils::subTreeValidForCaching);

        return currentValidity && childrenValidity;
    }

    private static String getSubTreeString(GraphFetchTree graphFetchTree)
    {
        return graphFetchTree.subTrees != null && !graphFetchTree.subTrees.isEmpty() ?
                graphFetchTree.subTrees.stream()
                        .map(subTree -> ((PropertyGraphFetchTree) subTree).property + parametersString(((PropertyGraphFetchTree) subTree)) + getSubTreeString(subTree))
                        .collect(Collectors.joining(",", "{", "}")) :
                "";
    }

    private static String parametersString(PropertyGraphFetchTree propertyGraphFetchTree)
    {
        if (propertyGraphFetchTree.parameters != null && !propertyGraphFetchTree.parameters.isEmpty())
        {
            final Collector<CharSequence, ?, String> joinStrings = Collectors.joining(",", "[", "]");

            return propertyGraphFetchTree.parameters.stream().map(param ->
                    param instanceof CBoolean ?
                            ((CBoolean) param).values.stream().map(Object::toString).collect(joinStrings) :
                            param instanceof CInteger ?
                                    ((CInteger) param).values.stream().map(Object::toString).collect(joinStrings) :
                                    param instanceof CFloat ?
                                            ((CFloat) param).values.stream().map(Object::toString).collect(joinStrings) :
                                            param instanceof CDecimal ?
                                                    ((CDecimal) param).values.stream().map(Object::toString).collect(joinStrings) :
                                                    param instanceof CString ?
                                                            ((CString) param).values.stream().map(x -> "'" + x.replace("'", "\\'") + "'").collect(joinStrings) :
                                                            param instanceof CStrictDate ?
                                                                    ((CStrictDate) param).values.stream().map(x -> "'" + x + "'").collect(joinStrings) :
                                                                    param instanceof CDateTime ?
                                                                            ((CDateTime) param).values.stream().map(x -> "'" + x + "'").collect(joinStrings) :
                                                                            param instanceof EnumValue ?
                                                                                    ((EnumValue) param).fullPath + "." + ((EnumValue) param).value :
                                                                                    (param instanceof Collection && ((Collection) param).values.stream().allMatch(x -> x instanceof EnumValue)) ?
                                                                                            ((Collection) param).values.stream().map(x -> ((EnumValue) x).fullPath + "." + ((EnumValue) x).value).collect(joinStrings) :
                                                                                            "$$~UNKNOWN~$$"
            ).collect(Collectors.joining(",", "(", ")"));
        }
        return "";
    }
}
