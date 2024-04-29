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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.Snapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.partitioning.FieldBasedForGraphFetch;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.partitioning.FieldBasedForTds;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.partitioning.Partitioning;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.BitemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Bitemporal;
import org.finos.legend.engine.testable.persistence.mapper.v1.MappingVisitors;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class BitemporalSnapshotMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot from(BitemporalSnapshot bitemporalSnapshot)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .transactionMilestoning(bitemporalSnapshot.transactionMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_TRANSACTION_MILESTONING))
                .validityMilestoning(bitemporalSnapshot.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING))
                .build();
    }

    public static org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot from(Bitemporal temporality, DatasetType datasetType)
    {
        Partitioning partition =  ((Snapshot) datasetType).partitioning;
        if (partition != null)
        {
            if (partition instanceof FieldBasedForTds)
            {
                FieldBasedForTds fieldBasedForTds = (FieldBasedForTds) partition;
                return org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot.builder()
                        .digestField(DIGEST_FIELD_DEFAULT)
                        .addAllPartitionFields(fieldBasedForTds.partitionFields)
                        .transactionMilestoning(temporality.processingDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_PROCESSING_DIMENSION))
                        .validityMilestoning(temporality.sourceDerivedDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_SOURCE_DERIVED_DIMENSION))
                        .build();
            }
            if (partition instanceof FieldBasedForGraphFetch)
            {
                List<String> partitionFields = new ArrayList<>();
                ((FieldBasedForGraphFetch) partition).partitionFieldPaths.forEach(pfp ->
                {
                    partitionFields.add(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.getPropertyPathElement(pfp));
                });
                return org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot.builder()
                        .digestField(DIGEST_FIELD_DEFAULT)
                        .addAllPartitionFields(partitionFields)
                        .transactionMilestoning(temporality.processingDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_PROCESSING_DIMENSION))
                        .build();
            }
        }
        return org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .transactionMilestoning(temporality.processingDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_PROCESSING_DIMENSION))
                .validityMilestoning(temporality.sourceDerivedDimension.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_SOURCE_DERIVED_DIMENSION))
                .build();
    }
}