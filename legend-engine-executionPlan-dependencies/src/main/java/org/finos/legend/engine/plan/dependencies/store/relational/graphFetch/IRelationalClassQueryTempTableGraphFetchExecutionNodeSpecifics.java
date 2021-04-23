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

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;
import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;
import org.finos.legend.engine.plan.dependencies.store.shared.IReferencedObject;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;

public interface IRelationalClassQueryTempTableGraphFetchExecutionNodeSpecifics
{
    /* Ordered list of all instance set ids along with respective mapping ids */
    List<Pair<String, String>> allInstanceSetImplementations();

    /* PK columns in result set */
    List<String> primaryKeyColumns(int setIndex);

    /* PK methods for returned objects */
    List<Method> primaryKeyGetters();

    /* Parent PK columns in query */
    List<String> parentPrimaryKeyColumns(List<String> queryResultColumns);

    /* Prepare for reading */
    void prepare(ResultSet resultSet, String databaseTimeZone, String databaseConnection);

    /* Read next graphFetch instance */
    IGraphInstance<? extends IReferencedObject> nextGraphInstance();

    /* Tie result object back to parent - should be called only after prepare is called */
    void addChildToParent(Object parent, Object child, IExecutionNodeContext context);

    /* If caching specific methods can be called */
    default boolean supportsCaching()
    {
        return false;
    }

    /* Deep copy - required for caching */
    default Object deepCopy(Object object)
    {
        return null;
    }
}
