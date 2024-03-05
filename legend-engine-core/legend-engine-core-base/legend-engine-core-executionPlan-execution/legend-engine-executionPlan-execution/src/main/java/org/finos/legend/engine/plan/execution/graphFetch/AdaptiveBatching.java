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

import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;

public class AdaptiveBatching
{
    public static long getAdaptiveBatchSize(ExecutionState executionState)
    {
        if (executionState.adaptiveGraphBatchStats == null)
        {
            executionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(0, 64);
            return executionState.adaptiveGraphBatchStats.previousBatchSize;
        }
        long softLimit = executionState.getGraphFetchExecutionConfiguration().getGraphFetchSoftMemoryLimitPercentage() * executionState.getGraphFetchExecutionConfiguration().getGraphFetchBatchMemoryHardLimit() / 100;
        long previousBatchMemoryUtilization = executionState.adaptiveGraphBatchStats.previousBatchMemoryUtilization;

        executionState.adaptiveGraphBatchStats.addPreviousAverageToStats(executionState.adaptiveGraphBatchStats.previousBatchMemoryUtilization, executionState.adaptiveGraphBatchStats.previousBatchSize);
        long avgMemoryUtilizationInBytesPerObject = (long) executionState.adaptiveGraphBatchStats.getWeightedAverage();
        long changeInBatchSize = softLimit / (avgMemoryUtilizationInBytesPerObject + 1) - executionState.adaptiveGraphBatchStats.previousBatchSize;

        return getNewBatchSize(executionState, changeInBatchSize, softLimit, previousBatchMemoryUtilization);
    }

    public static long getNewBatchSize(ExecutionState executionState, long changeInBatchSize, long softLimit, long previousBatchMemoryUtilization)
    {
        long newBatchSize;

        if (changeInBatchSize > 0)
        {
            double reductionFactor = (softLimit - previousBatchMemoryUtilization) / (1.0 * softLimit);
            long increment = (long) Math.pow(10, executionState.adaptiveGraphBatchStats.incrementRate);
            newBatchSize = getNewBatchSizeWithIncrement(executionState, changeInBatchSize, reductionFactor, increment);
        }
        else
        {
            long reductionFactor = (long) Math.pow(2, executionState.adaptiveGraphBatchStats.decrementRate);
            long previousBatchSize = executionState.adaptiveGraphBatchStats.previousBatchSize;

            if (previousBatchSize + changeInBatchSize > previousBatchSize / reductionFactor)
            {
                newBatchSize = previousBatchSize + changeInBatchSize;
                executionState.adaptiveGraphBatchStats.decrementRate = 1;
            }
            else
            {
                newBatchSize = previousBatchSize / reductionFactor;
                executionState.adaptiveGraphBatchStats.decrementRate += 1;
            }
            executionState.adaptiveGraphBatchStats.incrementRate = 1;
        }
        newBatchSize = newBatchSize <= 0 ? 1 : newBatchSize;
        executionState.adaptiveGraphBatchStats.previousBatchSize = newBatchSize;
        return newBatchSize;
    }

    private static long getNewBatchSizeWithIncrement(ExecutionState executionState, long changeInBatchSize, double reductionFactor, long increment)
    {
        long newBatchSize;
        if ((long) (changeInBatchSize * reductionFactor) < increment)
        {
            changeInBatchSize = (long) (changeInBatchSize * reductionFactor);
            executionState.adaptiveGraphBatchStats.incrementRate = 1;
        }
        else
        {
            changeInBatchSize = increment;
            executionState.adaptiveGraphBatchStats.incrementRate += 1;
        }
        executionState.adaptiveGraphBatchStats.decrementRate = 1;
        newBatchSize = executionState.adaptiveGraphBatchStats.previousBatchSize + changeInBatchSize;
        return newBatchSize;
    }
}
