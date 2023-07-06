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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.DatasetType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.Delta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.actionindicator.NoActionIndicator;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.UnitemporalDelta;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Unitemporal;
import org.finos.legend.engine.testable.persistence.mapper.v1.MappingVisitors;

import static org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class UnitemporalDeltaMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta from(UnitemporalDelta unitemporalDelta)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .mergeStrategy(unitemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY))
                .transactionMilestoning(unitemporalDelta.transactionMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_TRANSACTION_MILESTONING))
                .build();
    }

    public static org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta from(Unitemporal temporality, DatasetType datasetType)
    {
        if (((Delta) datasetType).actionIndicator == null)
        {
            ((Delta) datasetType).actionIndicator = new NoActionIndicator();
        }
        return org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .mergeStrategy(((Delta)datasetType).actionIndicator.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_DELETE_STRATEGY))
                .transactionMilestoning(temporality.processingDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_PROCESSING_DIMENSION))
                .build();
    }
}