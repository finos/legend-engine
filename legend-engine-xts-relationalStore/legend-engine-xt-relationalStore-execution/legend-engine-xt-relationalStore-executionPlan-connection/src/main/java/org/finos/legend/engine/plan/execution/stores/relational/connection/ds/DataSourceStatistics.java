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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DataSourceStatistics
{
    private final AtomicInteger builtConnections;
    private final AtomicInteger requestedConnections;
    private final AtomicInteger connectionErrors = new AtomicInteger();
    private final AtomicLong firstConnectionRequest;
    private AtomicLong lastConnectionRequest;

    public DataSourceStatistics()
    {
        this.firstConnectionRequest = new AtomicLong(System.currentTimeMillis());
        this.builtConnections = new AtomicInteger(0);
        this.lastConnectionRequest = new AtomicLong(getCurrentTimeInInMillis());
        this.requestedConnections = new AtomicInteger(0);
    }

    private DataSourceStatistics(int builtConnections, long firstConnectionRequest, long lastConnectionRequest, int requestedConnections)
    {
        this.builtConnections = new AtomicInteger(builtConnections);
        this.firstConnectionRequest = new AtomicLong(firstConnectionRequest);
        this.lastConnectionRequest = new AtomicLong(lastConnectionRequest);
        this.requestedConnections = new AtomicInteger(requestedConnections);
    }

    public static DataSourceStatistics clone(DataSourceStatistics statistics)
    {
        return new DataSourceStatistics(statistics.builtConnections.get(), statistics.firstConnectionRequest.get(), statistics.lastConnectionRequest.get(), statistics.requestedConnections.get());
    }

    public int getRequestedConnections()
    {
        return requestedConnections.get();
    }

    public int requestConnection()
    {
        lastConnectionRequest.set(getCurrentTimeInInMillis());
        return requestedConnections.incrementAndGet();
    }

    private long getCurrentTimeInInMillis()
    {
        return ConnectionStateManager.getInstance().getClock().millis();
    }

    public long getFirstConnectionRequest()
    {
        return firstConnectionRequest.get();
    }

    public long getLastConnectionRequest()
    {
        return lastConnectionRequest.get();
    }

    public long getDataSourceAge()
    {
        return getCurrentTimeInInMillis() - this.firstConnectionRequest.get();
    }

    public long getLastConnectionRequestAge()
    {
        return getCurrentTimeInInMillis() - this.lastConnectionRequest.get();
    }

    public int buildConnection()
    {
        return builtConnections.incrementAndGet();
    }

    public int getBuiltConnections()
    {
        return builtConnections.get();
    }

    public void logConnectionError()
    {
        this.connectionErrors.incrementAndGet();
    }

    public int getTotalConnectionErrors()
    {
        return this.connectionErrors.get();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DataSourceStatistics that = (DataSourceStatistics) o;
        return Objects.equals(getBuiltConnections(), that.getBuiltConnections())
                && Objects.equals(getRequestedConnections(), that.getRequestedConnections())
                && Objects.equals(getTotalConnectionErrors(), that.getTotalConnectionErrors())
                && Objects.equals(getFirstConnectionRequest(), that.getFirstConnectionRequest())
                && Objects.equals(getLastConnectionRequest(), that.getLastConnectionRequest());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getBuiltConnections(), getRequestedConnections(), getTotalConnectionErrors(), getFirstConnectionRequest(), getLastConnectionRequest());
    }
}
