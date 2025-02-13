// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api.utils;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.DeriveMainDatasetSchemaFromStaging;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class ApiUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiUtils.class);
    public static final String LOCK_INFO_DATASET_SUFFIX = "_legend_persistence_lock";

    public static Dataset deriveMainDatasetFromStaging(Dataset mainDataset, Dataset stagingDataset, IngestMode ingestMode)
    {
        Dataset enrichedMainDataset = mainDataset;
        if (mainDataset instanceof DatasetDefinition && (mainDataset.schema().fields() == null || mainDataset.schema().fields().isEmpty()))
        {
            enrichedMainDataset = ingestMode.accept(new DeriveMainDatasetSchemaFromStaging(mainDataset, stagingDataset));
        }
        return enrichedMainDataset;
    }

    public static Datasets enrichAndApplyCase(Datasets datasets, CaseConversion caseConversion, boolean enableConcurrentSafety)
    {
        DatasetsCaseConverter converter = new DatasetsCaseConverter();
        MetadataDataset metadataDataset = datasets.metadataDataset().orElse(MetadataDataset.builder().build());

        Datasets enrichedDatasets;
        if (enableConcurrentSafety)
        {
            LockInfoDataset lockInfoDataset = getLockInfoDataset(datasets);
            enrichedDatasets = datasets
                .withMetadataDataset(metadataDataset)
                .withLockInfoDataset(lockInfoDataset);
        }
        else
        {
            enrichedDatasets = datasets
                .withMetadataDataset(metadataDataset);
        }

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

    public static DatasetDefinition applyCase(DatasetDefinition datasetDefinition, CaseConversion caseConversion)
    {
        DatasetCaseConverter converter = new DatasetCaseConverter();
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return (DatasetDefinition) converter.applyCaseOnDataset(datasetDefinition, String::toUpperCase);
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return (DatasetDefinition) converter.applyCaseOnDataset(datasetDefinition, String::toLowerCase);
        }
        return datasetDefinition;
    }

    public static DatasetReference applyCase(DatasetReference datasetReference, CaseConversion caseConversion)
    {
        Function<String, String> strategy;
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            strategy = String::toUpperCase;
        }
        else if (caseConversion == CaseConversion.TO_LOWER)
        {
            strategy = String::toLowerCase;
        }
        else
        {
            return datasetReference;
        }

        datasetReference = datasetReference.withName(strategy.apply(datasetReference.name().orElseThrow(IllegalAccessError::new)));
        if (datasetReference.database().isPresent())
        {
            datasetReference = datasetReference.withDatabase(strategy.apply(datasetReference.database().get()));
        }
        if (datasetReference.group().isPresent())
        {
            datasetReference = datasetReference.withGroup(strategy.apply(datasetReference.group().get()));
        }

        return datasetReference;
    }

    public static LockInfoDataset applyCase(LockInfoDataset lockInfoDataset, CaseConversion caseConversion)
    {
        Function<String, String> strategy;
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            strategy = String::toUpperCase;
        }
        else if (caseConversion == CaseConversion.TO_LOWER)
        {
            strategy = String::toLowerCase;
        }
        else
        {
            return lockInfoDataset;
        }
        return new DatasetCaseConverter().applyCaseOnLockInfoDataset(lockInfoDataset, strategy);
    }

    public static SchemaDefinition applyCase(SchemaDefinition schema, CaseConversion caseConversion)
    {
        DatasetCaseConverter converter = new DatasetCaseConverter();
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return converter.applyCaseOnSchemaDefinition(schema, String::toUpperCase);
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return converter.applyCaseOnSchemaDefinition(schema, String::toLowerCase);
        }
        return schema;
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

    public static String convertCase(CaseConversion caseConversion, String value)
    {
        switch (caseConversion)
        {
            case TO_UPPER:
                return value.toUpperCase();
            case TO_LOWER:
                return value.toLowerCase();
            default:
                return value;
        }
    }
}
