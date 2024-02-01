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

import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.dataset.DatasetType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.appendonly.AppendOnly;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.Nontemporal;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.auditing.NoAuditing;
import org.finos.legend.engine.testable.persistence.mapper.v1.MappingVisitors;

import static org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper.BATCH_ID_FIELD_DEFAULT;
import static org.finos.legend.engine.testable.persistence.mapper.v1.IngestModeMapper.DIGEST_FIELD_DEFAULT;

public class AppendOnlyMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.AppendOnly from(AppendOnly appendOnly)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(DIGEST_FIELD_DEFAULT).build())
                .filterExistingRecords(appendOnly.filterDuplicates)
                .auditing(appendOnly.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING))
                .batchIdField(BATCH_ID_FIELD_DEFAULT)
                .build();
    }

    public static org.finos.legend.engine.persistence.components.ingestmode.AppendOnly from(Nontemporal temporality, DatasetType datasetType)
    {
        DeduplicationStrategy deduplicationStrategy;
        if (temporality.auditing == null)
        {
            temporality.auditing = new NoAuditing();
        }
        boolean filterExistingRecords = false;
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.AppendOnly appendOnlyHandling = (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.AppendOnly) temporality.updatesHandling;
        if (appendOnlyHandling.appendStrategy instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.relational.temporality.updatesHandling.appendStrategy.FilterDuplicates)
        {
            filterExistingRecords = true;
        }

        return org.finos.legend.engine.persistence.components.ingestmode.AppendOnly.builder()
                .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(DIGEST_FIELD_DEFAULT).build())
                .filterExistingRecords(filterExistingRecords)
                .auditing(temporality.auditing.accept(org.finos.legend.engine.testable.persistence.mapper.v2.MappingVisitors.MAP_TO_COMPONENT_NONTEMPORAL_AUDITING))
                .batchIdField(BATCH_ID_FIELD_DEFAULT)
                .build();
    }
}