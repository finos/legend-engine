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

import io.swagger.annotations.Api;
import org.finos.legend.engine.ide.session.PureSession;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

@Api(tags = "Execute")
@Path("/")
public class ExecuteGo
{
    private PureSession pureSession;

    public ExecuteGo(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @POST
    @Path("executeGo")
    public Response executeGo(@Context HttpServletRequest request, @Context HttpServletResponse response)
    {
        return Response.ok((StreamingOutput) outputStream ->
        {
            this.pureSession.saveFilesAndExecute(request, response, outputStream, new GoRun());
        }).build();
    }
}
