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

package org.finos.legend.engine.plan.execution.graphFetch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdaptiveGraphBatchStats
{
    public long previousBatchMemoryUtilization;
    protected long previousBatchSize;
    protected long incrementRate;   // counter for incrementing batch size
    protected long decrementRate;   // counter for decrementing batch size
    private final List<BatchStats> lastTenBatchesStats;

    public AdaptiveGraphBatchStats(long previousBatchMemoryUtilization, long previousBatchSize)
    {
        this.previousBatchMemoryUtilization = previousBatchMemoryUtilization;
        this.previousBatchSize = previousBatchSize;
        this.incrementRate = 1;
        this.decrementRate = 1;
        if (previousBatchMemoryUtilization != 0 && previousBatchSize != 0)
        {
            this.lastTenBatchesStats = new ArrayList<>(Arrays.asList(new BatchStats(previousBatchMemoryUtilization, previousBatchSize)));
        }
        else
        {
            this.lastTenBatchesStats = new ArrayList<>();
        }
    }

    public void addPreviousAverageToStats(long previousBatchMemoryUtilization, long previousBatchSize)
    {
        this.lastTenBatchesStats.add(new BatchStats(previousBatchMemoryUtilization, previousBatchSize));
        if (this.lastTenBatchesStats.size() > 10)
        {
            this.lastTenBatchesStats.remove(0);
        }
    }

    public double getWeightedAverage()
    {
        double weight = 1.0;
        double weightedBatchSize = 0.0;
        double weightedMemoryUtilization = 0.0;
        for (int i = this.lastTenBatchesStats.size() - 1; i >= 0; i--)
        {
            BatchStats batchStats = this.lastTenBatchesStats.get(i);
            weightedBatchSize += weight * batchStats.batchSize;
            weightedMemoryUtilization += weight * batchStats.batchMemoryUtilization;
            weight -= 0.1;
        }
        return weightedMemoryUtilization / weightedBatchSize;
    }

    static class BatchStats
    {
        public final long batchSize;
        public final long batchMemoryUtilization;

        BatchStats(long batchMemoryUtilization, long batchSize)
        {
            this.batchSize = batchSize;
            this.batchMemoryUtilization = batchMemoryUtilization;
        }
    }
}
