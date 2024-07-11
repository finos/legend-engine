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

import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public abstract class RelationalMultiDatasetIngestorAbstract
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalMultiDatasetIngestorAbstract.class);

    //-------------------- FIELDS --------------------

    public abstract RelationalSink relationalSink();

    abstract LockInfoDataset lockInfoDataset();

    //-------------------- FLAGS --------------------

    @Value.Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Value.Default
    public boolean collectStatistics()
    {
        return true;
    }

    @Value.Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Value.Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    public abstract Map<String, Object> additionalMetadata();

    public abstract Optional<String> ingestRequestId();

    @Value.Default
    public boolean enableIdempotencyCheck()
    {
        return false;
    }

    @Value.Default
    public SqlLogging sqlLogging()
    {
        return SqlLogging.DISABLED;
    }

    @Value.Default
    public int sampleDataErrorRowCount()
    {
        return 20;
    }

    //-------------------- APIs --------------------

    /**
     * Ingest Multi datasets in a Tx. Each dataset can have multi stages
     * @param Map of dataset Name, List of IngestStages
     * @return Map of dataset Name, List of IngestStageResults
     */
    public void init(List<DatasetIngestDetails> datasetIngestDetailsMap)
    {

    }

    public void create()
    {

    }

    public List<DatasetIngestResultsAbstract> ingest()
    {
        return null;
    }

}
