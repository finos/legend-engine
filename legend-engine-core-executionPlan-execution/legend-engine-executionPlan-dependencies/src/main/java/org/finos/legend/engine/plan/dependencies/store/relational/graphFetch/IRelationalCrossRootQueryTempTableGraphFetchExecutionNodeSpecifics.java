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

package org.finos.legend.engine.plan.dependencies.store.relational.graphFetch;

import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;

public interface IRelationalCrossRootQueryTempTableGraphFetchExecutionNodeSpecifics
{
    /* Mapping id */
    String mappingId();

    /* Cross store mapping source instance set id */
    String sourceInstanceSetId();

    /* Cross store mapping target instance set id */
    String targetInstanceSetId();

    /* Cross key getters of parent */
    List<Method> parentCrossKeyGetters();

    /* PK methods for returned objects */
    List<Method> primaryKeyGetters();

    /* Parent PK columns in query */
    List<String> parentCrossKeyColumns(List<String> queryResultColumns);

    /* Prepare for reading */
    void prepare(ResultSet resultSet, String databaseTimeZone, String databaseConnection);

    /* Read next graphFetch instance */
    IGraphInstance<? extends IReferencedObject> nextGraphInstance();

    /* Tie result object back to parent - should be called only after prepare is called */
    void addChildToParent(Object parent, Object child, IExecutionNodeContext context);

    /* If cross caching specific methods can be called */
    default boolean supportsCrossCaching()
    {
        return false;
    }

    /* Cross caching - Target properties ordered */
    default List<String> targetPropertiesOrdered()
    {
        return null;
    }

    /* Cross caching - Cross key getters of parent ordered according to target properties */
    default List<Method> parentCrossKeyGettersOrderedByTargetProperties()
    {
        return null;
    }

    /* Deep copy - required for caching */
    default Object deepCopy(Object object)
    {
        return null;
    }

}
