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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.MutableMultimap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
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
    private MutableMultimap<String, StoreExecutable> sessionExecutableMap = new FastListMultimap<>();
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
            sessionExecutableMap.put(sessionID, execution);
        }
    }

    public void removeExecutable(String sessionID, StoreExecutable executable)
    {
    if (isRegistered)
        {
        try
            {
            sessionExecutableMap.remove(sessionID, executable);
            }
        catch (Exception ignore)
            {
            LOGGER.info(new LogInfo(null, LoggingEventType.EXECUTABLE_REMOVE_ERROR, "Unable to remove executable for session " + sessionID).toString());
            }
        }
    }

    public MutableList<StoreExecutable> getExecutables(String sessionID)
    {
        return sessionExecutableMap.get(sessionID).toList();
    }

    public void registerManager()
    {
        this.isRegistered = true;
    }

     public void cancelExecutablesOnSession(String sessionID)
    {
        sessionExecutableMap.get(sessionID).toList().stream().forEach(
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
                    this.removeExecutable(sessionID, executable);
                }
        );
    }
}

