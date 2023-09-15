// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.DeriveMainDatasetSchemaFromStaging;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetsCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataDataset;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.List;

public class ApiUtils
{
    private static final String LOCK_INFO_DATASET_SUFFIX = "_legend_persistence_lock";

    public static Dataset deriveMainDatasetFromStaging(Datasets datasets, IngestMode ingestMode)
    {
        Dataset mainDataset = datasets.mainDataset();
        List<Field> mainDatasetFields = mainDataset.schema().fields();
        if (mainDatasetFields == null || mainDatasetFields.isEmpty())
        {
            mainDataset = ingestMode.accept(new DeriveMainDatasetSchemaFromStaging(datasets.mainDataset(), datasets.stagingDataset()));
        }
        return mainDataset;
    }

    public static Datasets enrichAndApplyCase(Datasets datasets, CaseConversion caseConversion)
    {
        DatasetsCaseConverter converter = new DatasetsCaseConverter();
        MetadataDataset metadataDataset = datasets.metadataDataset().orElse(MetadataDataset.builder().build());
        BulkLoadMetadataDataset bulkLoadMetadataDataset = datasets.bulkLoadMetadataDataset().orElse(BulkLoadMetadataDataset.builder().build());
        LockInfoDataset lockInfoDataset = getLockInfoDataset(datasets);
        Datasets enrichedDatasets = datasets
                .withMetadataDataset(metadataDataset)
                .withLockInfoDataset(lockInfoDataset)
                .withBulkLoadMetadataDataset(bulkLoadMetadataDataset);
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return converter.applyCase(enrichedDatasets, String::toUpperCase);
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return converter.applyCase(enrichedDatasets, String::toLowerCase);
        }
        return enrichedDatasets;
    }

    public static IngestMode applyCase(IngestMode ingestMode, CaseConversion caseConversion)
    {
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toUpperCase));
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toLowerCase));
        }
        return ingestMode;
    }

    private static LockInfoDataset getLockInfoDataset(Datasets datasets)
    {
        Dataset main = datasets.mainDataset();
        LockInfoDataset lockInfoDataset;
        if (datasets.lockInfoDataset().isPresent())
        {
            lockInfoDataset = datasets.lockInfoDataset().get();
        }
        else
        {
            String datasetName = main.datasetReference().name().orElseThrow(IllegalStateException::new);
            String lockDatasetName = datasetName + LOCK_INFO_DATASET_SUFFIX;
            lockInfoDataset = LockInfoDataset.builder()
                    .database(main.datasetReference().database())
                    .group(main.datasetReference().group())
                    .name(lockDatasetName)
                    .build();
        }
        return lockInfoDataset;
    }
}
