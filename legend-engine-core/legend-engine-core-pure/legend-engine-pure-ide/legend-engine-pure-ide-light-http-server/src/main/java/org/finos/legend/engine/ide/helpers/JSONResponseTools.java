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

package org.finos.legend.engine.ide.helpers;

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m4.exception.PureException;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class JSONResponseTools
{
    public static final String JSON_CONTENT_TYPE = "application/json";

    private JSONResponseTools()
    {
        // Utility class
    }

    private static void sendJSONResponse(HttpServletResponse response, int status, Object json) throws IOException
    {
        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        // We call getOutputStream instead of getWriter, as calling getWriter after calling getOutputStream can
        // cause problems and getOutputStream may have already been called
        try (PrintWriter writer = new PrintWriter(response.getOutputStream()))
        {
            JSONValue.writeJSONString(json, writer);
        }
    }

    public static void sendJSONErrorResponse(HttpServletResponse response, int status, Throwable t, boolean includeStackTrace) throws IOException
    {
        sendJSONErrorResponse(response, status, t, includeStackTrace, System.currentTimeMillis(), null);
    }

    private static void sendJSONErrorResponse(HttpServletResponse response, int status, Throwable t, boolean includeStackTrace, long timestamp, String requestId) throws IOException
    {
        String message = t.getMessage();
        String stackTrace = null;
        if (t instanceof PureException && ((PureException)t).hasPureStackTrace())
        {
            stackTrace = ((PureException)t).getPureStackTrace();
        }
        else if (includeStackTrace)
        {
            StringWriter writer = new StringWriter(512);
            t.printStackTrace(new PrintWriter(writer));
            stackTrace = writer.toString();
        }
        sendJSONErrorResponse(response, status, message, stackTrace, timestamp, requestId);
    }

    private static void sendJSONErrorResponse(HttpServletResponse response, int status, String message, String stackTrace, long timestamp, String requestId) throws IOException
    {
        MutableMap<String, Object> json = buildJSONErrorMessage(message, stackTrace, timestamp, requestId);
        sendJSONResponse(response, status, json);
    }

    private static MutableMap<String, Object> buildJSONErrorMessage(String message, String stackTrace, long timestamp, String requestId)
    {
        MutableMap<String, Object> json = UnifiedMap.newMap(5);
        json.put("error", true);
        if (message != null)
        {
            json.put("message", message);
        }
        if (stackTrace != null)
        {
            json.put("stackTrace", stackTrace);
        }
        json.put("host", getLocalHostName());
        json.put("timestamp", String.format("%tY-%<tm-%<td %<tH:%<tM:%<tS.%<tL %<tZ", timestamp));
        if (requestId != null)
        {
            json.put("requestId", requestId);
        }
        return json;
    }

    private static String getLocalHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            return "unknown host";
        }
    }
}