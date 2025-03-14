// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.deephaven.connection;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.BarrageSessionFactoryConfig;
import io.deephaven.client.impl.BarrageSessionFactoryConfig.Factory;
import io.deephaven.client.impl.ClientConfig;
import io.deephaven.client.impl.Session;
import io.deephaven.client.impl.SessionConfig;
import io.deephaven.uri.DeephavenTarget;
import io.grpc.ManagedChannel;
import org.apache.arrow.memory.RootAllocator;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DeephavenSession
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DeephavenSession.class);
    private final Session clientSession;
    private final BarrageSession barrageSession;
    private final RootAllocator bufferAllocator;
    private final ScheduledExecutorService scheduler;
    private final Factory factory;
    private final ManagedChannel managedChannel;

    public DeephavenSession(DeephavenTarget target, String authTypeAndValue)
    {
        // TODO: there should be a constructor(s) that accept params "mtls" and "explicit" - however, would need to incorporate these concepts into AuthenticationSpecification first
        this.bufferAllocator = new RootAllocator();

        // Create a scheduler thread pool. This is used by the Flight session.
        this.scheduler = Executors.newScheduledThreadPool(4);

        // ClientConfig describes the configuration to connect to the host
        ClientConfig clientConfig = ClientConfig.builder()
                .target(target)
                .build();

        // SessionConfig describes the configuration needed to create a session
        SessionConfig sessionConfig = SessionConfig.builder()
                .authenticationTypeAndValue(authTypeAndValue)
                .build();

        // Create a FlightSessionFactory. This stitches together the above components to create the real, live API session with the server.
        this.factory = BarrageSessionFactoryConfig.builder()
                .clientConfig(clientConfig)
                .allocator(bufferAllocator)
                .scheduler(scheduler)
                .build()
                .factory();

        this.barrageSession = factory.newBarrageSession(sessionConfig);
        this.clientSession = this.barrageSession.session();
        this.managedChannel = factory.managedChannel();
    }

    public Session getClientSession()
    {
        return clientSession;
    }

    public BarrageSession getBarrageSession()
    {
        return barrageSession;
    }

    public void close() throws Exception
    {
        try
        {
            barrageSession.close();
            clientSession.close();
            try
            {
                clientSession.closeFuture().get(5, TimeUnit.SECONDS);
            }
            catch (Exception e)
            {
                LOGGER.warn("Session close future failed");
            }
            if (managedChannel != null)
            {
                managedChannel.shutdown();
                if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS))
                {
                    managedChannel.shutdownNow();
                    if (!managedChannel.awaitTermination(5, TimeUnit.SECONDS))
                    {
                        LOGGER.warn("ManagedChannel failed to terminate even after shutdownNow");
                    }
                }
            }
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS))
            {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                {
                    LOGGER.warn("Scheduler failed to terminate even after shutdownNow");
                }
            }
            bufferAllocator.close();
        }
        catch (Exception e)
        {
            throw new EngineException("Error closing deephaven session", e, ExceptionCategory.SERVER_EXECUTION_ERROR);
        }
    }
}