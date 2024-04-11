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

package org.finos.legend.engine.plan.execution.cache;

public interface ExecutionCacheStats
{
    long requestCount();

    long hitCount();

    long missCount();

    long loadCount();

    long loadSuccessCount();

    long loadFailureCount();

    long evictionCount();

    double hitRate();

    double missRate();

    double loadFailureRate();

    double averageLoadPenalty();

    long totalLoadTime();

    String toString();

    default String buildStatsString()
    {
        return "ExecutionCacheStats(\n" +
                "   requestCount = " + this.requestCount() + ",\n" +
                "   hitCount = " + this.hitCount() + ",\n" +
                "   missCount = " + this.missCount() + ",\n" +
                "   loadCount = " + this.loadCount() + ",\n" +
                "   loadSuccessCount = " + this.loadSuccessCount() + ",\n" +
                "   loadFailureCount = " + this.loadFailureCount() + ",\n" +
                "   evictionCount = " + this.evictionCount() + "\n" +
                "   hitRate = " + this.hitRate() + ",\n" +
                "   missRate = " + this.missRate() + ",\n" +
                "   loadFailureRate = " + this.loadFailureRate() + ",\n" +
                "   averageLoadPenalty = " + this.averageLoadPenalty() + ",\n" +
                "   totalLoadTime = " + this.totalLoadTime() + ",\n" +
                ")";
    }
}
