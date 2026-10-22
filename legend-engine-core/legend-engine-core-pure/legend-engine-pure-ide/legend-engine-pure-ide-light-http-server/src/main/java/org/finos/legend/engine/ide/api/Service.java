// Copyright 2023 Goldman Sachs
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
import io.swagger.v3.oas.annotations.Operation;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.ide.api.execution.function.manager.ContentType;
import org.finos.legend.engine.ide.api.execution.function.manager.ExecutionManager;
import org.finos.legend.engine.ide.api.execution.function.manager.ExecutionRequest;
import org.finos.legend.engine.ide.api.execution.function.manager.HttpServletResponseWriter;
import org.finos.legend.engine.ide.session.PureSession;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import java.io.IOException;
import java.util.Map;

@Tag(name = "Service")
@Path("")
public class Service
{
    private final PureSession pureSession;

    public Service(PureSession pureSession)
    {
        this.pureSession = pureSession;
    }

    @GET
    @Path("{path:.+}")
    @Operation(summary = "")
    public void exec(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("path") String path) throws IOException
    {
        Pair<CoreInstance, Map<String, String[]>> result = this.pureSession.getPureRuntime().getURLPatternLibrary().tryExecution("/" + path, this.pureSession.getPureRuntime().getProcessorSupport(), request.getParameterMap());
        if (result == null)
        {
            response.sendError(404, "The service '" + path + "' can't be found!");
            return;
        }

        ExecutionManager executionManager = new ExecutionManager(pureSession.getFunctionExecution());
        executionManager.execute(new ExecutionRequest(result.getTwo()), new HttpServletResponseWriter(response), ContentType.text);
    }
}
