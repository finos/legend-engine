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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.testable.persistence.mapper.v1.MappingVisitors;

import static org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper.BATCH_ID_FIELD_DEFAULT;

public class NontemporalSnapshotMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot from(NontemporalSnapshot nontemporalSnapshot)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot.builder()
                .auditing(nontemporalSnapshot.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING))
                .batchIdField(BATCH_ID_FIELD_DEFAULT)
                .build();
    }

    public static org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot from(Nontemporal temporality, DatasetType datasetType)
    {
        if (temporality.auditing != null)
        {
            return org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot.builder()
                    .auditing(temporality.auditing.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_NONTEMPORAL_AUDITING))
                    .batchIdField(BATCH_ID_FIELD_DEFAULT)
                    .build();
        }
        return org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot.builder()
                .auditing(org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing.builder().build())
                .batchIdField(BATCH_ID_FIELD_DEFAULT)
                .build();
    }
}