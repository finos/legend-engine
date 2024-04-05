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

package org.finos.legend.engine.ide.api.concept;

import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.engine.ide.session.SimpleFunction;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

public class GetConcept implements SimpleFunction
{
    @Override
    public void run(PureSession pureSession, JSONObject extraParams, JSONArray modifiedFiles, HttpServletResponse response, OutputStream outputStream) throws Exception
    {
        PureException compilationError = null;
        String compilationErrorText = "";
        PureRuntime pureRuntime = pureSession.getPureRuntime();

        ProcessorSupport processorSupport = pureRuntime.getProcessorSupport();

        Source src = pureRuntime.getSourceById((String) extraParams.get("file"));
        if (!src.isCompiled())
        {
            try
            {
                pureRuntime.compile();
            }
            catch (PureException e)
            {
                compilationError = e;
                compilationErrorText = JSONValue.escape(ExceptionTranslation.pureExceptionToJson(pureSession, compilationError, null).getText());
                // check if target file was compiled even if the whole compilation failed.
                if (!src.isCompiled())
                {
                    throw e;
                }
            }
        }
        Long line = (Long) extraParams.get("line");
        Long column = (Long) extraParams.get("column");
        CoreInstance found = src.navigate(line.intValue(), column.intValue(), processorSupport);

        if (null != found)
        {
            SourceInformation sourceInfo = found.getSourceInformation();
            if (null == sourceInfo)
            {
                outputStream.write(("{\"error\":true,\"text\":\"" + (null == compilationError ? "Navigation is not supported yet for this element!" : compilationErrorText) + "\"").getBytes());
                outputStream.write((",\"compiler\":\"\"").getBytes());
                outputStream.write((",\"cached\":" + pureRuntime.getCache().getCacheState().isCached()).getBytes());
                if (null != compilationError)
                {
                    outputStream.write(",\"exceptionType\":\"Parser error\"".getBytes());
                }
                outputStream.write("}".getBytes());
            }
            else
            {
                Source source = pureRuntime.getSourceById(sourceInfo.getSourceId());
                outputStream.write(("{\"text\":\"" + (null == compilationError ? "" : compilationErrorText) + "\"").getBytes());
                outputStream.write((",\"compiler\":\"\"").getBytes());
                outputStream.write((",\"jumpTo\":{\"source\":\"" + sourceInfo.getSourceId() + "\", \"line\":" + sourceInfo.getLine() + ", \"column\":" + sourceInfo.getColumn() + ", \"RO\":\"" + source.isImmutable() + "\"}").getBytes());
                outputStream.write((",\"cached\":" + pureRuntime.getCache().getCacheState().isCached()).getBytes());
                if (null != compilationError)
                {
                    outputStream.write(",\"exceptionType\":\"Parser error\"".getBytes());
                }
                outputStream.write("}".getBytes());
            }
        }
        else
        {
            outputStream.write(("{\"error\":true,\"text\":\"" + (null == compilationError ? "Navigation is not supported yet for this element!" : compilationErrorText) + "\"").getBytes());
            outputStream.write((",\"compiler\":\"\"").getBytes());
            if (null != compilationError)
            {
                outputStream.write(",\"exceptionType\":\"Parser error\"".getBytes());
            }
            outputStream.write("}".getBytes());
        }
    }
}
