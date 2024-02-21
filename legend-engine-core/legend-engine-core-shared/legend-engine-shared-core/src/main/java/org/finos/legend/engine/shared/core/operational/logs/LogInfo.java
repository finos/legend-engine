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

package org.finos.legend.engine.shared.core.operational.logs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class LogInfo
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapper();

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    public Date timeStamp;
    public DeploymentMode mode = DeploymentStateAndVersions.DEPLOYMENT_MODE;
    public String user;
    public String eventType;
    public String message;
    public Object info;
    public double duration;
    public Throwable t;
    public String trace;
    public SourceInformation sourceInformation;

    public LogInfo(ILoggingEventType eventType, String user, String message)
    {
        this.timeStamp = new Date();
        this.user = user;
        this.eventType = eventType.name();
        this.message = message;
    }

    public LogInfo(ILoggingEventType eventType, String user, String message, long durationMs)
    {
        this.timeStamp = new Date();
        this.user = user;
        this.eventType = eventType.name();
        this.message = message;
        this.duration = durationMs;
    }

    public LogInfo(String user, ILoggingEventType eventType)
    {
        this.timeStamp = new Date();
        this.user =  user;
        this.eventType = eventType.toString();
    }

    public LogInfo(String user, String eventType)
    {
        this.timeStamp = new Date();
        this.user = user;
        this.eventType = eventType;
    }

    public LogInfo(String user, ILoggingEventType eventType, double duration)
    {
        this(user, eventType);
        this.duration = duration;
    }

    public LogInfo(String user, String eventType, double duration)
    {
        this(user, eventType);
        this.duration = duration;
    }

    public LogInfo(String user, ILoggingEventType eventType, String message)
    {
        this(user, eventType);
        this.message = message;
    }

    public LogInfo(String user, String eventType, String message)
    {
        this(user, eventType);
        this.message = message;
    }

    public LogInfo(String user, ILoggingEventType eventType, Object info)
    {
        this(user, eventType);
        this.info = info;
    }

    public LogInfo(String user, String eventType, Object info)
    {
        this(user, eventType);
        this.info = info;
    }

    public LogInfo(String user, ILoggingEventType eventType, Object info, double duration)
    {
        this(user, eventType, info);
        this.duration = duration;
    }

    public LogInfo(String user, String eventType, Object info, double duration)
    {
        this(user, eventType, info);
        this.duration = duration;
    }

    public LogInfo(String user, ILoggingEventType eventType, Throwable t)
    {
        this(user, eventType.toString(), t);
    }

    public LogInfo(String user, String eventType, Throwable t)
    {
        this(user, eventType);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        t.printStackTrace(writer);
        int index = ExceptionUtils.indexOfThrowable(t, EngineException.class);
        this.message = index == -1 ? ExceptionUtils.getRootCauseMessage(t) : ExceptionUtils.getThrowables(t)[index].getMessage();
        this.trace = out.toString();
        if (t instanceof EngineException && ((EngineException) t).getSourceInformation() != SourceInformation.getUnknownSourceInformation())
        {
            this.sourceInformation = ((EngineException) t).getSourceInformation();
        }
        if (index != -1 && ExceptionUtils.getThrowables(t)[index] instanceof EngineException && ((EngineException) ExceptionUtils.getThrowables(t)[index]).getSourceInformation() != SourceInformation.getUnknownSourceInformation())
        {
            this.sourceInformation = ((EngineException) ExceptionUtils.getThrowables(t)[index]).getSourceInformation();
        }
    }

    public LogInfo(String user, String eventType, Throwable t, double duration)
    {
        this(user, eventType, t);
        this.duration = duration;
    }

    public String toString()
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(this);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
