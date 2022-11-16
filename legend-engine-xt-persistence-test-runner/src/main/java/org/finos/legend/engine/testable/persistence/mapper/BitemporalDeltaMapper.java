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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;

import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class BitemporalDeltaMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta from(BitemporalDelta bitemporalDelta)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .mergeStrategy(bitemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY))
                .transactionMilestoning(bitemporalDelta.transactionMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_TRANSACTION_MILESTONING))
                .validityMilestoning(bitemporalDelta.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING))
                .build();
    }
}
