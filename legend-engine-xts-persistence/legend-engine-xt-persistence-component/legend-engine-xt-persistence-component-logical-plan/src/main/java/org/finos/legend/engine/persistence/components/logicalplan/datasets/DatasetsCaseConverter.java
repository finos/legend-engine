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

public class DatasetsCaseConverter
{
    DatasetCaseConverter datasetCaseConverter = new DatasetCaseConverter();

    public Datasets applyCase(Datasets datasets, Function<String, String> strategy)
    {
        Dataset main = datasetCaseConverter.applyCaseOnDataset(datasets.mainDataset(), strategy);
        Dataset staging = datasetCaseConverter.applyCaseOnDataset(datasets.stagingDataset(), strategy);
        Optional<Dataset> temp = datasets.tempDataset().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        Optional<Dataset> tempWithDeleteIndicator = datasets.tempDatasetWithDeleteIndicator().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        Optional<Dataset> stagingWithoutDuplicates = datasets.stagingDatasetWithoutDuplicates().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));
        Optional<MetadataDataset> metadata = Optional.ofNullable(datasetCaseConverter.applyCaseOnMetadataDataset(datasets.metadataDataset().orElseThrow(IllegalStateException::new), strategy));
        Optional<LockInfoDataset> lockInfo = datasets.lockInfoDataset().map(dataset -> datasetCaseConverter.applyCaseOnLockInfoDataset(dataset, strategy));
        Optional<Dataset> deletePartitionDataset = datasets.deletePartitionDataset().map(dataset -> datasetCaseConverter.applyCaseOnDataset(dataset, strategy));

        return Datasets.builder()
            .mainDataset(main)
            .stagingDataset(staging)
            .tempDataset(temp)
            .tempDatasetWithDeleteIndicator(tempWithDeleteIndicator)
            .stagingDatasetWithoutDuplicates(stagingWithoutDuplicates)
            .metadataDataset(metadata)
            .lockInfoDataset(lockInfo)
            .deletePartitionDataset(deletePartitionDataset)
            .build();
    }
}
