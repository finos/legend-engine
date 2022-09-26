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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicatesAbstract;

import java.util.Optional;

import static org.immutables.value.Value.Check;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public interface AppendOnlyAbstract extends IngestMode
{
    Optional<String> digestField();

    Optional<String> dataSplitField();

    Auditing auditing();

    DeduplicationStrategy deduplicationStrategy();

    @Check
    default void validate()
    {
        deduplicationStrategy().accept(new DeduplicationStrategyVisitor<Void>()
        {
            @Override
            public Void visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
            {
                return null;
            }

            @Override
            public Void visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
            {
                if (!digestField().isPresent())
                {
                    throw new IllegalStateException("Cannot build AppendOnly, [digestField] must be specified since [deduplicationStrategy] is set to filter duplicates");
                }
                return null;
            }

            @Override
            public Void visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
            {
                return null;
            }
        });
    }

    @Override
    default <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visitAppendOnly(this);
    }
}
