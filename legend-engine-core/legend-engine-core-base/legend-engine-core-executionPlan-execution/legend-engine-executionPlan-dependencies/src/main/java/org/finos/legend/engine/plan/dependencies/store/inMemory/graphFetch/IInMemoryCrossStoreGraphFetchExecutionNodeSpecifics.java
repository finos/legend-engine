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

package org.finos.legend.engine.plan.dependencies.store.inMemory.graphFetch;

import org.finos.legend.engine.plan.dependencies.domain.graphFetch.IGraphInstance;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public interface IInMemoryCrossStoreGraphFetchExecutionNodeSpecifics
{
    /* Resolves cross store relationship and returns cross-store key values of valid children for given parent */
    Map<String, Object> getCrossStoreKeysValueForChildren(Object parent);

    /* Resolves cross store relationship and returns cross-store key values of child for given child */
    Map<String, Object> getCrossStoreKeysValueFromChild(Object child);

    /* Wrap child in graph instance */
    IGraphInstance<?> wrapChildInGraphInstance(Object child);

    /* Validate cross store relationship and attempt adding child to parent - returns true if child was added else returns false
    there is a possibility that returned child may not be associated with parent. This method evaluates the cross store property mapping expression to perform validation. */
    boolean attemptAddingChildToParent(Object parent, Object child);

    /* Cross caching - Cross key getters of parent ordered according to target properties */
    default List<Method> parentCrossKeyGettersOrderedByTargetProperties()
    {
        return null;
    }
}
