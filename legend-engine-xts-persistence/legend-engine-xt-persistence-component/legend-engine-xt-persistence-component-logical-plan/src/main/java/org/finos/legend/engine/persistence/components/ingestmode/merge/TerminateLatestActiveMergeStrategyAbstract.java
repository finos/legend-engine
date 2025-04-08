// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.merge;

import java.util.List;

import static org.immutables.value.Value.Check;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Parameter;
import static org.immutables.value.Value.Style;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface TerminateLatestActiveMergeStrategyAbstract extends MergeStrategy
{
    @Parameter(order = 0)
    String terminateField();

    @Parameter(order = 1)
    List<Object> terminateValues();

    @Override
    default <T> T accept(MergeStrategyVisitor<T> visitor)
    {
        return visitor.visitTerminateLatestActiveMergeStrategy(this);
    }

    @Check
    default void validate()
    {
        if (terminateValues().isEmpty())
        {
            throw new IllegalStateException("Cannot build TerminateLatestActiveMergeStrategy, [terminateValues] must contain at least one element");
        }
    }
}
