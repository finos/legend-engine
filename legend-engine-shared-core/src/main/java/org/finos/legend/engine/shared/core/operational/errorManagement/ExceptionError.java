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

package org.finos.legend.engine.shared.core.operational.errorManagement;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;

public class ExceptionError
{
    public String status = null;
    public Object generationInfo = null;
    public List<Object> activities;
    private int code;
    private String message;
    private String trace;

    private Object payload;
    private SourceInformation sourceInformation;
    private EngineErrorType errorType;

    ExceptionError(int code, String message)
    {
        this.status = "error";
        this.code = code;
        this.message = message;
    }

    ExceptionError(int code, Throwable t)
    {
        this.status = "error";
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        t.printStackTrace(writer);
        this.code = code;
        int index = ExceptionUtils.indexOfThrowable(t, EngineException.class);
        Throwable[] throwables = ExceptionUtils.getThrowables(t);
        this.message = index == -1 ? ExceptionUtils.getRootCauseMessage(t) : throwables[index].getMessage();
        this.trace = out.toString();
        if (t instanceof EngineException)
        {
            this.errorType = ((EngineException) t).getErrorType();
            if (((EngineException) t).getSourceInformation() != SourceInformation.getUnknownSourceInformation())
            {
                this.sourceInformation = ((EngineException) t).getSourceInformation();
            }
        }
        if (index != -1 && throwables[index] instanceof EngineException)
        {
            EngineException engineException = (EngineException) throwables[index];
            this.errorType = engineException.getErrorType();
            if (engineException.getSourceInformation() != SourceInformation.getUnknownSourceInformation())
            {
                this.sourceInformation = engineException.getSourceInformation();
            }
        }
    }

    public int getCode()
    {
        return code;
    }

    public String getTrace()
    {
        return this.trace;
    }

    public String getMessage()
    {
        return message;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }

    public EngineErrorType getErrorType()
    {
        return errorType;
    }
}
