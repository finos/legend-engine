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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.VersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.NoDeletesMergeStrategy;

import java.util.Optional;

import static org.immutables.value.Value.Default;
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
public interface NontemporalDeltaAbstract extends IngestMode
{
    String digestField();

    Auditing auditing();

    Optional<String> dataSplitField();

    @Default
    default VersioningStrategy versioningStrategy()
    {
        return NoVersioningStrategy.builder().build();
    }

    @Default
    default MergeStrategy mergeStrategy()
    {
        return NoDeletesMergeStrategy.builder().build();
    }

    @Override
    default <T> T accept(IngestModeVisitor<T> visitor)
    {
        return visitor.visitNontemporalDelta(this);
    }
}
