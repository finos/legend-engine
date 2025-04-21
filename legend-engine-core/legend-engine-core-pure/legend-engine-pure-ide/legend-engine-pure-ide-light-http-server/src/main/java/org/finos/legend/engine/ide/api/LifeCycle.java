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

package org.finos.legend.engine.ide.api;

import io.swagger.annotations.Api;
import org.finos.legend.engine.ide.session.PureSessionManager;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;

@Api(tags = "LifeCycle")
@Path("/")
public class LifeCycle
{
    private final PureSessionManager sessionManager;

    public LifeCycle(PureSessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    @GET
    @Path("initialize")
    public Response initialize()
    {
        PureRuntime pureRuntime = sessionManager.getSession().getPureRuntime();

        return Response.ok((StreamingOutput) outStream ->
        {
            if (pureRuntime.isFullyInitialized())
            {
                outStream.write(("{\"cached\":false, \"datamarts\": [" + pureRuntime.getCodeStorage().getAllRepositories().collect(s -> "\"" + s.getName() + "\"").makeString(",") + "]}").getBytes());
                outStream.close();
                return;
            }

            try
            {
                pureRuntime.reset();
                pureRuntime.initialize(sessionManager.getSession().message);
                outStream.write("{\"text\":\"Full recompile completed successfully\", \"cached\":".getBytes());
                outStream.write((pureRuntime.getCache().getCacheState().isCached() + "}").getBytes());
                outStream.close();
            }
            catch (IOException | RuntimeException | Error e)
            {
                pureRuntime.getCache().deleteCache();
                throw e;
            }
        }).build();
    }

    @POST
    @Path("executeSaveAndReset")
    public Response executeSaveAndReset(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            PureRuntime pureRuntime = sessionManager.getSession().getPureRuntime();
            pureRuntime.reset();
            pureRuntime.getCache().deleteCache();
            sessionManager.getSession().saveFiles(request, response);
            outputStream.write(("{\"text\":\"Reset Done\", \"cached\":" + false + "}").getBytes());
            outputStream.close();
        }).build();
    }
}
