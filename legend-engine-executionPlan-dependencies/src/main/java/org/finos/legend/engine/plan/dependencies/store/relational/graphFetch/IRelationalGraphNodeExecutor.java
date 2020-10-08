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

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.List;

public interface IRelationalGraphNodeExecutor
{
    IGraphInstance<?> getObjectFromResultSet(ResultSet resultSet, String databaseTimeZone, String databaseConnection);

    List<Method> primaryKeyGetters();

    // ----------------------------------------------------------------------------------------------------------------
    // Default methods to be implemented if caching required

    default boolean supportsCaching()
    {
        return false;
    }

    default String getMappingId(ResultSet resultSet, String databaseTimeZone, String databaseConnection)
    {
        return null;
    }

    default String getInstanceSetId(ResultSet resultSet, String databaseTimeZone, String databaseConnection)
    {
        return null;
    }

    default List<String> primaryKeyColumns()
    {
        return null;
    }

    default Object deepCopy(Object object)
    {
        return null;
    }

    // -------------------------------------------------------------------------------------------------------------------
}
