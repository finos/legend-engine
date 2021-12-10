// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceStatistics;
import org.junit.Assert;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class InstrumentedConnectionStateManager extends ConnectionStateManager
{
    private CountDownLatch countDownLatch;
    private final int expectedSize;

    InstrumentedConnectionStateManager(Clock clock, CountDownLatch countDownLatch, int expectedSize)
    {
        super(clock);
        this.countDownLatch = countDownLatch;
        this.expectedSize = expectedSize;
    }

    @Override
    protected Set<Pair<String, DataSourceStatistics>> findPoolsOlderThan(Duration duration)
    {
        Set<Pair<String, DataSourceStatistics>> set = super.findPoolsOlderThan(duration);
        Assert.assertEquals(expectedSize,set.size());
        try
        {
            this.countDownLatch.await();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        return set;
    }
}
