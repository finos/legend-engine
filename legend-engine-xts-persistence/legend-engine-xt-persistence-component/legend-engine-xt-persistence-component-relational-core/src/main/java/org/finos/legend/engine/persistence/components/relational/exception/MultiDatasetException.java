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

package org.finos.legend.engine.persistence.components.relational.exception;

import org.finos.legend.engine.persistence.components.relational.api.IngestStageMetadata;

import java.time.Instant;

public class MultiDatasetException extends RuntimeException
{
    private Exception exception;

    private String dataset;

    private IngestStageMetadata ingestStageMetadata;

    private Instant stageStartInstant;

    private Instant stageExceptionInstant;

    public Exception getException()
    {
        return exception;
    }

    public String getDataset()
    {
        return dataset;
    }

    public IngestStageMetadata getIngestStageMetadata()
    {
        return ingestStageMetadata;
    }

    public Instant getStageStartInstant()
    {
        return stageStartInstant;
    }

    public Instant getStageExceptionInstant()
    {
        return stageExceptionInstant;
    }

    public MultiDatasetException(Exception exception, Instant stageStartInstant, Instant stageExceptionInstant, String dataset, IngestStageMetadata ingestStageMetadata, String message)
    {
        super(message);
        this.exception = exception;
        this.stageStartInstant = stageStartInstant;
        this.stageExceptionInstant = stageExceptionInstant;
        this.dataset = dataset;
        this.ingestStageMetadata = ingestStageMetadata;
    }
}
