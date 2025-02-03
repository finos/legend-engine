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

package org.finos.legend.engine.ide.api.execution.go;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.ide.helpers.response.ExceptionTranslation;
import org.finos.legend.engine.ide.helpers.response.IDEExceptionResponse;
import org.finos.legend.engine.ide.helpers.response.IDEResponse;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.engine.ide.session.SimpleFunction;
import org.finos.legend.engine.pure.ide.interpreted.debug.FunctionExecutionInterpretedWithDebugSupport;
import org.finos.legend.pure.m3.execution.Console;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class GoRun implements SimpleFunction
{
    @Override
    public void run(PureSession pureSession, JSONObject extraParams, JSONArray modifiedFiles, HttpServletResponse response, OutputStream outputStream) throws Exception
    {
        PureRuntime pureRuntime = pureSession.getPureRuntime();
        pureRuntime.compile();

        CoreInstance function = pureRuntime.getFunction("go():Any[*]");
        if (null == function)
        {
            throw new RuntimeException("Please write a go function. Example: function go():Any[*]{print('ok');}");
        }

        Console console = null;
        try
        {
            outputStream.write("{\"text\":\"".getBytes());
            FunctionExecution functionExecution = pureSession.getFunctionExecution();
            console = functionExecution.getConsole();
            console.setPrintStream(new JSONPrintStream(outputStream));
            console.setConsole(true);
            if (functionExecution instanceof FunctionExecutionInterpretedWithDebugSupport)
            {
                ((FunctionExecutionInterpretedWithDebugSupport) functionExecution).startDebug(function, FastList.newList());
            }
            else
            {
                functionExecution.start(function, FastList.newList());
            }
            outputStream.write("\"".getBytes());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            IDEResponse exceptionResponse = ExceptionTranslation.buildExceptionMessage(pureSession, ex, new ByteArrayOutputStream());
            outputStream.write(JSONValue.escape("\n" + exceptionResponse.getText()).getBytes());
            outputStream.write("\"".getBytes());

            if (exceptionResponse instanceof IDEExceptionResponse)
            {
                IDEExceptionResponse ideExceptionResponse = (IDEExceptionResponse)exceptionResponse;
                MutableMap<String, Object> additionalValues = Maps.mutable.of();
                ideExceptionResponse.addJsonKeyValues(additionalValues);
                for (String key : additionalValues.keysView())
                {
                    outputStream.write((",\"" + key + "\":" + JSONValue.toJSONString(additionalValues.get(key))).getBytes());
                }
            }
        }
        finally
        {
            if (null != console)
            {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(stream);
                console.setPrintStream(ps);
                console.setConsole(false);
            }
        }

        if (null != modifiedFiles)
        {
            outputStream.write((",\"modifiedFiles\":" + modifiedFiles.toJSONString()).getBytes());
        }
        outputStream.write((",\"compiler\":\"" + JSONValue.escape("pureSession.getCompilerLogs()") + "\", \"cached\":" + pureRuntime.getCache().getCacheState().isCached() + "}").getBytes());
    }

    private static class JSONPrintStream extends PrintStream
    {
        JSONPrintStream(OutputStream out)
        {
            super(out, true);
        }

        @Override
        public void print(String s)
        {
            super.print(JSONValue.escape(s));
        }
    }
}
