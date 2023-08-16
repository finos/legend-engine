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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.Optional;
import java.util.function.Function;

public class DatasetsEnricher
{
    DatasetCaseConverter datasetCaseConverter = new DatasetCaseConverter();
    private static final String LOCK_INFO_DATASET_SUFFIX = "_legend_persistence_lock";

    public Datasets enrichAndApplyCase(Datasets datasets, Function<String, String> strategy)
    {
        Dataset main = datasetCaseConverter.applyCaseOnDataset(datasets.mainDataset(), strategy);
        Dataset staging = datasetCaseConverter.applyCaseOnDataset(datasets.stagingDataset(), strategy);
        Optional<Dataset> temp = datasets.tempDataset().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        Optional<Dataset> tempWithDeleteIndicator = datasets.tempDatasetWithDeleteIndicator().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        Optional<Dataset> stagingWithoutDuplicates = datasets.stagingDatasetWithoutDuplicates().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        MetadataDataset metadataDataset = getMetadataDataset(datasets);
        LockInfoDataset lockInfoDataset = getLockInfoDataset(datasets, main);
        Optional<MetadataDataset> metadata = Optional.ofNullable(datasetCaseConverter.applyCaseOnMetadataDataset(metadataDataset, strategy));
        Optional<LockInfoDataset> lockInfo = Optional.ofNullable(datasetCaseConverter.applyCaseOnLockInfoDataset(lockInfoDataset, strategy));

        return Datasets.builder()
            .mainDataset(main)
            .stagingDataset(staging)
            .tempDataset(temp)
            .tempDatasetWithDeleteIndicator(tempWithDeleteIndicator)
            .stagingDatasetWithoutDuplicates(stagingWithoutDuplicates)
            .metadataDataset(metadata)
            .lockInfoDataset(lockInfo)
            .build();
    }

    private MetadataDataset getMetadataDataset(Datasets datasets)
    {
        MetadataDataset metadataset;
        if (datasets.metadataDataset().isPresent())
        {
            metadataset = datasets.metadataDataset().get();
        }
        else
        {
            metadataset = MetadataDataset.builder().build();
        }
        return metadataset;
    }

    private LockInfoDataset getLockInfoDataset(Datasets datasets, Dataset main)
    {
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
