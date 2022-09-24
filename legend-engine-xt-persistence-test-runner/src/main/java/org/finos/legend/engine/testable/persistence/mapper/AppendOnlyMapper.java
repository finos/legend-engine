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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;

import java.util.Arrays;

import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class AppendOnlyMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.AppendOnly from(AppendOnly appendOnly, String[] pkFields)
    {
        DeduplicationStrategy deduplicationStrategy = appendOnly.filterDuplicates ?
                FilterDuplicates.builder().build() : AllowDuplicates.builder().build();

        return org.finos.legend.engine.persistence.components.ingestmode.AppendOnly.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .addAllKeyFields(Arrays.asList(pkFields))
                .deduplicationStrategy(deduplicationStrategy)
                .auditing(appendOnly.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING))
                .build();
    }
}
