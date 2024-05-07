// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.versioning;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface NoVersioningStrategyAbstract extends VersioningStrategy
{
    @Value.Default
    default boolean failOnDuplicatePrimaryKeys()
    {
        return false;
    }

    @Override
    default <T> T accept(VersioningStrategyVisitor<T> visitor)
    {
        return visitor.visitNoVersioningStrategy(this);
    }
}