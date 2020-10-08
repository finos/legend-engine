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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;

import java.util.Collections;
import java.util.List;

public class OperationClassMapping extends ClassMapping
{
    public List<String> parameters = Collections.emptyList();
    public MappingOperation operation;
    public static ImmutableMap<MappingOperation, String> opsToFunc = Maps.immutable.of(
            MappingOperation.ROUTER_UNION, "meta::pure::router::operations::special_union_OperationSetImplementation_1__SetImplementation_MANY_",
            MappingOperation.STORE_UNION, "meta::pure::router::operations::union_OperationSetImplementation_1__SetImplementation_MANY_",
            MappingOperation.INHERITANCE, "meta::pure::router::operations::inheritance_OperationSetImplementation_1__SetImplementation_MANY_",
            MappingOperation.MERGE, "meta::pure::router::operations::merge_OperationSetImplementation_1__SetImplementation_MANY_");

    public <T> T accept(ClassMappingVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
