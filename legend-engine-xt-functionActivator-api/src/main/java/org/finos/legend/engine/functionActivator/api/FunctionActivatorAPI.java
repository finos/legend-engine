//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.functionActivator.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.functionActivator.api.input.FunctionActivatorInput;
import org.finos.legend.engine.functionActivator.api.output.FunctionActivatorInfo;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorLoader;
import org.finos.legend.engine.functionActivator.service.FunctionActivatorService;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_external_functionActivator_FunctionActivator;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Function Activator")
@Path("functionActivator/")
public class FunctionActivatorAPI
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final ModelManager modelManager;
    private final PureModel emptyModel;
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions;

    public FunctionActivatorAPI(ModelManager modelManager, Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions)
    {
        this.modelManager = modelManager;
        this.routerExtensions = routerExtensions;
        this.emptyModel = Compiler.compile(PureModelContextData.newPureModelContextData(), DeploymentMode.PROD, null);
    }

    @GET
    @Path("list")
    @ApiOperation(value = "List all available function activators")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        MutableList<FunctionActivatorInfo> values = FunctionActivatorLoader.extensions().collect(x -> x.info(emptyModel, "vX_X_X"));
        return ManageConstantResult.manageResult(profiles, values, objectMapper);
    }

    @POST
    @Path("validate")
    @ApiOperation(value = "Validate that the functions can be properly activated by the activator.")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response validate(FunctionActivatorInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            String clientVersion = input.clientVersion == null ? PureClientVersions.production : input.clientVersion;
            PureModel pureModel = modelManager.loadModel(input.model, clientVersion, profiles, null);
            Root_meta_external_functionActivator_FunctionActivator activator = (Root_meta_external_functionActivator_FunctionActivator) pureModel.getPackageableElement(input.functionActivator);
            FunctionActivatorService<Root_meta_external_functionActivator_FunctionActivator> service = getActivatorService(activator, pureModel);
            return Response.ok(objectMapper.writeValueAsString(service.validate(pureModel, activator, input.model, routerExtensions))).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, profiles);
        }
    }

    @POST
    @Path("publishToSandbox")
    @ApiOperation(value = "Public the activator to a sandbox environment. Production deployment will occur using the SDLC pipeline.")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishToSandbox(FunctionActivatorInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            String clientVersion = input.clientVersion == null ? PureClientVersions.production : input.clientVersion;
            PureModel pureModel = modelManager.loadModel(input.model, clientVersion, profiles, null);
            Root_meta_external_functionActivator_FunctionActivator activator = (Root_meta_external_functionActivator_FunctionActivator) pureModel.getPackageableElement(input.functionActivator);
            FunctionActivatorService<Root_meta_external_functionActivator_FunctionActivator> service = getActivatorService(activator,pureModel);
            return Response.ok(objectMapper.writeValueAsString(service.publishToSandbox(pureModel, activator, input.model, routerExtensions))).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, profiles);
        }
    }

    @POST
    @Path("renderArtifact")
    @ApiOperation(value = "Display generated artifact as text where applicable")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response renderArtifact(FunctionActivatorInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            String clientVersion = input.clientVersion == null ? PureClientVersions.production : input.clientVersion;
            PureModel pureModel = modelManager.loadModel(input.model, clientVersion, profiles, null);
            Root_meta_external_functionActivator_FunctionActivator activator = (Root_meta_external_functionActivator_FunctionActivator) pureModel.getPackageableElement(input.functionActivator);
            FunctionActivatorService<Root_meta_external_functionActivator_FunctionActivator> service = getActivatorService(activator, pureModel);
            return Response.ok(objectMapper.writeValueAsString(service.renderArtifact(pureModel, activator, input.model, routerExtensions))).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, profiles);
        }
    }

    public FunctionActivatorService<Root_meta_external_functionActivator_FunctionActivator> getActivatorService(Root_meta_external_functionActivator_FunctionActivator activator, PureModel pureModel)
    {
        FunctionActivatorService<Root_meta_external_functionActivator_FunctionActivator> service = FunctionActivatorLoader.extensions().select(c -> c.supports(activator)).getFirst();
        if (service == null)
        {
            throw new RuntimeException(activator.getClass().getSimpleName() + "is not supported!");
        }
        return service;
    }
}
