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

package org.finos.legend.engine.plan.execution.result;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorResult extends Result
{
    private int code;
    private String message;
    private String trace;

    private Result payload;
    private SourceInformation sourceInformation;

    public ErrorResult(int code, String message)
    {
        super("error");
        this.code = code;
        this.message = message;
    }

    public ErrorResult(int code, Throwable t)
    {
        super("error");
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        t.printStackTrace(writer);
        this.code = code;
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

    public ErrorResult(int code, String message, Result payload)
    {
        super("error");
        this.code = code;
        this.message = message;
        this.payload = payload;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return resultVisitor.visit(this);
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

    public Result getPayload()
    {
        return this.payload;
    }

    public SourceInformation getSourceInformation()
    {
        return this.sourceInformation;
    }
}
