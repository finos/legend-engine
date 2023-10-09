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

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningVisitors;
import org.immutables.value.Value;

import java.util.Optional;

public interface IngestMode
{
    @Value.Derived
    default Optional<String> dataSplitField()
    {
        return this.versioningStrategy().accept(VersioningVisitors.EXTRACT_DATA_SPLIT_FIELD);
    }

    @Value.Default
    default DeduplicationStrategy deduplicationStrategy()
    {
        return AllowDuplicates.builder().build();
    }

    @Value.Default
    default VersioningStrategy versioningStrategy()
    {
        return NoVersioningStrategy.builder().build();
    }

    <T> T accept(IngestModeVisitor<T> visitor);
}
