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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;

@Tag(name = "LifeCycle")
@Path("/")
public class LifeCycle
{
    private PureSession pureSession;

    public LifeCycle(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("initialize")
    public Response initialize()
    {
        PureRuntime pureRuntime = pureSession.getPureRuntime();

        return Response.ok((StreamingOutput) outStream ->
        {
            if (pureRuntime.isFullyInitialized())
            {
                outStream.write(("{\"cached\":false, \"datamarts\": [" + pureSession.getPureRuntime().getCodeStorage().getAllRepositories().collect(s -> "\"" + s.getName() + "\"").makeString(",") + "]}").getBytes());
                outStream.close();
                return;
            }

            try
            {
                pureRuntime.reset();
                pureRuntime.initialize(pureSession.message);
                outStream.write("{\"text\":\"Full recompile completed successfully\", \"cached\":".getBytes());
                outStream.write((pureRuntime.getCache().getCacheState().isCached() + "}").getBytes());
                outStream.close();
            }
            catch (IOException | RuntimeException | Error e)
            {
                //e.printStackTrace();
                pureSession.getPureRuntime().getCache().deleteCache();
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
            PureRuntime pureRuntime = pureSession.getPureRuntime();
            pureRuntime.reset();
            pureRuntime.getCache().deleteCache();
            pureSession.saveFiles(request, response);
            outputStream.write(("{\"text\":\"Reset Done\", \"cached\":" + false + "}").getBytes());
            outputStream.close();
        }).build();
    }
}
