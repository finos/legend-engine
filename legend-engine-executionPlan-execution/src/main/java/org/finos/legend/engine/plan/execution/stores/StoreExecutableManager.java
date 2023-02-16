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
import java.util.List;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;


/**
 * This is a singleton that maintains the relationship between  http sessions and store executions.
 * It can be used to operate on executions (for example to cancel running executions)
 * The manager must be initialized as well as added as a HttpSessionListener
 */
public enum StoreExecutableManager
{
    INSTANCE;
    private final ConcurrentHashMap<String, List<StoreExecutable>> sessionExecutableMap = new ConcurrentHashMap<>();
    private boolean isRegistered = false;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StoreExecutableManager.class);

    /**
     * Resets the state of the tracker -intending to be used for testing
     */
    public void reset()
    {
        this.isRegistered = false;
        this.sessionExecutableMap.clear();
    }

    public void addExecutable(String sessionID, StoreExecutable execution)
    {
        if (isRegistered)
        {
            sessionExecutableMap.computeIfAbsent(sessionID, x -> Collections.synchronizedList(new ArrayList<>())).add(execution);
        }
    }

    public void removeExecutable(String sessionID, StoreExecutable executable)
    {
        if (isRegistered)
        {
            try
            {
                sessionExecutableMap.computeIfPresent(sessionID, (a, executableList) ->
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
                });

            }
            catch (Exception ignore)
            {
                LOGGER.info(new LogInfo(null, LoggingEventType.EXECUTABLE_REMOVE_ERROR, "Unable to remove executable for session " + sessionID).toString());
            }
        }
    }

    public List<StoreExecutable> getExecutables(String sessionID)
    {
        return sessionExecutableMap.getOrDefault(sessionID, Collections.EMPTY_LIST);
    }

    public void registerManager()
    {
        this.isRegistered = true;
    }

    public void cancelExecutablesOnSession(String sessionID)
    {
        sessionExecutableMap.remove(sessionID).forEach(
                executable ->
                {
                    try
                    {
                        executable.cancel();
                    }
                    catch (Exception ignore)
                    {
                        LOGGER.info(new LogInfo(null, LoggingEventType.EXECUTABLE_CANCELLATION_ERROR, "Unable to cancel executable for session " + sessionID).toString());
                    }
                }
        );
    }
}

