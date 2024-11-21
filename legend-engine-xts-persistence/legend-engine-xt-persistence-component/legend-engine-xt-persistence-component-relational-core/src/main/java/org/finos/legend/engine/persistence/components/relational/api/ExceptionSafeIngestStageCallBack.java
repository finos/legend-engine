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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

public class ExceptionSafeIngestStageCallBack implements IngestStageCallBack
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionSafeIngestStageCallBack.class);

    private final IngestStageCallBack delegate;

    public ExceptionSafeIngestStageCallBack(IngestStageCallBack delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public void onStageStart(String datasetName, long batchId, IngestMode ingestMode, Instant stageStartInstant)
    {
        safeCall(() -> delegate.onStageStart(datasetName, batchId, ingestMode, stageStartInstant));
    }

    @Override
    public void onStageSuccess(String datasetName, long batchId, IngestMode ingestMode, List<IngestStageResult> ingestStageResults)
    {
        safeCall(() -> delegate.onStageSuccess(datasetName, batchId, ingestMode, ingestStageResults));
    }

    private void safeCall(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Exception e)
        {
            LOGGER.warn("Encountered exception while executing callback", e);
        }
    }
}
