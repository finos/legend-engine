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

package org.finos.legend.engine.plan.execution.stores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

/**
 * This is a singleton that maintains the relationship between  http requests and store executions.
 * It can be used to operate on executions (for example to cancel running executions)
 * The manager must be initialized as well as added as a HttpSessionListener
 */
public enum StoreExecutableManager
{
    INSTANCE;
    private final ConcurrentHashMap<String, List<StoreExecutable>> requestExecutableMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Set<String>> sessionIDToProvidedID = new ConcurrentHashMap<>();
    private boolean isRegistered = false;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StoreExecutableManager.class);

    /**
     * Resets the state of the tracker -intending to be used for testing
     */
    public void reset()
    {
        this.isRegistered = false;
        this.requestExecutableMap.clear();
        this.sessionIDToProvidedID.clear();
    }

    public void addExecutable(String requestID, StoreExecutable execution)
    {
        if (isRegistered && requestID != null)
        {
            requestExecutableMap.computeIfAbsent(requestID, x -> Collections.synchronizedList(new ArrayList<>())).add(execution);
        }
    }

    public void addExecutable(RequestContext context, StoreExecutable execution)
    {
        String requestID = RequestContext.getRequestToken(context);  //header if its there else it's the session

        if (isRegistered && requestID != null)
        {
            Set<String> sessionIDs = sessionIDToProvidedID.computeIfAbsent(RequestContext.getSessionID(context), x -> Collections.synchronizedSet(new HashSet<>()));
            sessionIDs.add(requestID);

        }

        addExecutable(requestID, execution);
    }


    public void removeExecutable(String id, StoreExecutable executable)
    {
        if (isRegistered && id != null)
        {
            try
            {

                Set<String> providedIds = new HashSet<>(sessionIDToProvidedID.getOrDefault(id, Collections.singleton(id)));
                providedIds.forEach(providedId -> requestExecutableMap.computeIfPresent(providedId, (key, executableList) ->
                {
                    executableList.remove(executable);
                    if (executableList.isEmpty())
                    {
                        return null;
                    }
                    else
                    {
                        return executableList;
                    }
                }));
                sessionIDToProvidedID.remove(id);

            }
            catch (Exception e)
            {
                LOGGER.info(new LogInfo(IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_REMOVE_ERROR, "Unable to remove executable for id " + id).toString());
            }
        }
    }


    public void removeExecutable(RequestContext context, StoreExecutable executable)
    {
        String requestID = RequestContext.getRequestToken(context);
        removeExecutable(requestID, executable);

    }

    public List<StoreExecutable> getExecutables(String id)
    {
        Set<String> requestIDs = new HashSet<>(sessionIDToProvidedID.getOrDefault(id, Collections.singleton(id)));
        return requestIDs.stream()
                .flatMap(reqID -> requestExecutableMap.getOrDefault(reqID, Collections.emptyList()).stream())
                .collect(Collectors.toList());
    }

    public List<StoreExecutable> getExecutables(RequestContext context)
    {
        String requestID = RequestContext.getRequestToken(context);
        return getExecutables(requestID);
    }

    public void registerManager()
    {
        this.isRegistered = true;
    }

    public int cancelExecutablesOnSession(String sessionID)
    {
        AtomicInteger numberOfCancelled = new AtomicInteger(0);
        List<StoreExecutable> executables = requestExecutableMap.remove(sessionID);
        if (executables != null)
        {
            executables.forEach(executable ->
            {
                try
                {
                    executable.cancel();
                    numberOfCancelled.incrementAndGet();
                }
                catch (Exception e)
                {
                    LOGGER.error(new LogInfo(IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName(), LoggingEventType.EXECUTABLE_CANCELLATION_ERROR, "Unable to cancel executable for ID " + sessionID + ": " + e.getMessage()).toString());
                }
            });
        }
        return numberOfCancelled.get();
    }

    public int cancelExecutablesByID(String requestID)
    {
        return cancelExecutablesOnSession(requestID);

    }

    public Integer getActiveSessionCount()
    {
        return requestExecutableMap.keySet().size();
    }

}

