// Copyright 2020 Goldman Sachs
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


package org.finos.legend.engine.plan.execution.result.graphFetch;

public class AdaptiveGraphBatchStats
{
    public long batchObjectMemoryUtilization;
    public long previousBatchSize;
    public long incrementRate;   // counter for incrementing batch size
    public long decrementRate;   // counter for decrementing batch size

    public AdaptiveGraphBatchStats(long batchObjectMemoryUtilization, long previousBatchSize)
    {
        this.batchObjectMemoryUtilization = batchObjectMemoryUtilization;
        this.previousBatchSize = previousBatchSize;
        this.incrementRate = 0;
        this.decrementRate = 0;
    }
}
