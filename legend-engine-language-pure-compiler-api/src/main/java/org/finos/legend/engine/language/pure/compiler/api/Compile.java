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

package org.finos.legend.engine.language.pure.compiler.api;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtensions;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.engine.shared.core.operational.prometheus.Prometheus;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Pure - Compiler")
@Path("pure/v1/compilation")
@Produces(MediaType.APPLICATION_JSON)
public class Compile
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final ModelManager modelManager;

    public Compile(ModelManager modelManager)
    {
        this.modelManager = modelManager;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @Path("compile")
    @ApiOperation(value = "Loads the model and then compiles. It performs no action. Mostly used for testing")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "compile model", doc = "Pure model compilation duration summary")
    public Response compile(PureModelContext model, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: compile").startActive(true))
        {
            Long start = System.currentTimeMillis();
            CompilerExtensions.logAvailableExtensions();
            modelManager.loadModelAndData(model, model instanceof PureModelContextPointer ? ((PureModelContextPointer) model).serializer.version : null, profiles, null);
            Long end = System.currentTimeMillis();
            MetricsHandler.observe("compile model", start, end);
            // NOTE: we could change this to return 204 (No Content), but Pure client test will break
            // on the another hand, returning 200 Ok with no content is not appropriate. So we have to put this dummy message "OK"
            return Response.ok("{\"message\":\"OK\"}", MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("compile model");
            Response errorResponse = ExceptionTool.exceptionManager(ex, LoggingEventType.COMPILE_ERROR, profiles);
            if (ex instanceof EngineException)
            {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex).build();
            }
            return errorResponse;
        }
    }

    @POST
    @Path("lambdaReturnType")
    @ApiOperation(value = "Loads a given model and lambda. Returns the lambda return type")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Prometheus(name = "lambda return type")
    public Response lambdaReturnType(LambdaReturnTypeInput lambdaReturnTypeInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            Long start = System.currentTimeMillis();
            PureModelContext model = lambdaReturnTypeInput.model;
            Lambda lambda = lambdaReturnTypeInput.lambda;
            String typeName = modelManager.getLambdaReturnType(lambda, model, model instanceof PureModelContextPointer ? ((PureModelContextPointer) model).serializer.version : null, profiles);
            Map<String, String> result = new HashMap<>();
            Long end = System.currentTimeMillis();
            MetricsHandler.observe("lambda return type", start, end);
            // This is an object in case we want to add more information on the lambda.
            result.put("returnType", typeName);
            return Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            MetricsHandler.observeError("lambda return type");
            Response errorResponse = ExceptionTool.exceptionManager(ex, LoggingEventType.COMPILE_ERROR, profiles);
            if (ex instanceof EngineException)
            {
                return Response.status(Response.Status.BAD_REQUEST).entity(ex).build();
            }
            return errorResponse;
        }
    }
}
