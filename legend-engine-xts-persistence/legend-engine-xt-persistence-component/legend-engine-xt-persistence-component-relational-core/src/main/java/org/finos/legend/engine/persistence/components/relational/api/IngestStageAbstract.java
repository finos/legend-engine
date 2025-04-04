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

package org.finos.legend.engine.persistence.components.relational.api;

import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.immutables.value.Value;

import java.util.Optional;
import java.util.UUID;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public abstract class IngestStageAbstract
{
    public abstract Dataset stagingDataset();

    public abstract DatasetReference mainDataset();

    public abstract Optional<Dataset> deletePartitionDataset();

    public abstract IngestMode ingestMode();

    public abstract Optional<String> stagingDatasetBatchIdField();

    @Value.Derived
    public String getRunId()
    {
        return UUID.randomUUID().toString();
    }

    @Value.Check
    public void validate()
    {
        if (ingestMode().versioningStrategy() instanceof AllVersionsStrategy)
        {
            throw new IllegalArgumentException("AllVersionsStrategy not allowed for multi-dataset ingestion");
        }
    }
}
