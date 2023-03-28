// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.util.Optional;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface FieldAbstract extends LogicalPlanNode
{
    String name();

    FieldType type();

    @Default
    default boolean nullable()
    {
        if (primaryKey())
        {
            return false;
        }
        return true;
    }

    @Default
    default boolean primaryKey()
    {
        return false;
    }

    @Default
    default boolean identity()
    {
        return false;
    }

    @Default
    default boolean unique()
    {
        return false;
    }

    Optional<String> fieldAlias();

    Optional<Object> defaultValue();
}
