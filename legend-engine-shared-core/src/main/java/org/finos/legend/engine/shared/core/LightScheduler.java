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

package org.finos.legend.engine.shared.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LightScheduler
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private MutableMap<String, Pair<TimerTask, Info>> infoBuffer = Maps.mutable.empty();
    private List<Timer> timers = Lists.mutable.empty();

    public void schedule(String id, int frequency, Function0<String> f)
    {
        LOGGER.info(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_REGISTER, "Registering light schedule " + id + " with frequency " + frequency).toString());
        TimerTask task = getTask(id, f);
        infoBuffer.put(id, Tuples.pair(task, new Info(id, frequency)));
        Timer timer = new Timer();
        timers.add(timer);
        timer.schedule(task, (long) (new SecureRandom().nextDouble() * 10000), frequency);
    }

    public void schedule(String id, Date start, int frequency, Function0<String> f)
    {
        LOGGER.info(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_REGISTER, "Registering light schedule " + id + " with frequency " + frequency).toString());
        TimerTask task = getTask(id, f);
        infoBuffer.put(id, Tuples.pair(task, new Info(id, start, frequency)));
        Timer timer = new Timer();
        timers.add(timer);
        timer.schedule(task, new Date(start.getTime() + Math.round(new SecureRandom().nextDouble() * 1000 * 60 * 10)), frequency);
    }

    public void shutDown()
    {
        timers.forEach(t ->
        {
            t.cancel();
            t.purge();
        });
    }

    public void forceTrigger(String id)
    {
        LOGGER.info(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_FORCE_START, "Force execution for " + id).toString());
        this.infoBuffer.get(id).getOne().run();
    }

    private TimerTask getTask(String id, Function0<String> f)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    LOGGER.info(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_EXECUTE_START, "Start executing " + id).toString());
                    long t = System.currentTimeMillis();
                    String info = f.value();
                    Info feedback = infoBuffer.get(id).getTwo();
                    feedback.message = info;
                    feedback.lastExecuted = new Date();
                    long execTime = System.currentTimeMillis() - t;
                    feedback.lastExecutionDuration = execTime;
                    LOGGER.info(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_EXECUTE_STOP, "Finished executing " + id + " (info:" + info + ")", execTime).toString());
                }
                catch (Exception e)
                {
                    Info feedback = infoBuffer.get(id).getTwo();
                    feedback.message = "ERROR: " + e.getMessage();
                    LOGGER.error(new LogInfo((String)null, LoggingEventType.LIGHT_SCHEDULER_EXECUTE_ERROR, "Error executing " + id).toString(), e);
                }
            }
        };
    }

    public static class Info
    {
        public String id;
        public Date initialDate;
        public long frequency;
        public Date lastExecuted;
        public long lastExecutionDuration;
        public String message;

        public Info()
        {
            // DO NOT DELETE: this resets the default constructor for Jackson
        }

        public Info(String id, long frequency)
        {
            this.id = id;
            this.frequency = frequency;
        }

        public Info(String id, Date initialDate, long frequency)
        {
            this.id = id;
            this.initialDate = initialDate;
            this.frequency = frequency;
        }
    }

    public String printStats()
    {
        try
        {
            return new ObjectMapper().writeValueAsString(this.infoBuffer.valuesView().collect(Pair::getTwo).toList());
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
