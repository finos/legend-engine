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

package org.finos.legend.engine.language.pure.grammar.api.elementGenerationFromSchema;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.api.elementGenerationFromSchema.input.ElementGenerationInput;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_relational_relational_autogeneration_relationalToPure;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Schema;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

@Api(tags = "Pure - Grammar")
@Path("pure/v1/grammar/schemaToElements")
public class ElementGenerationAPI
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");
    private final Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensionsFunc;
    private final DeploymentMode deploymentMode;

    public ElementGenerationAPI(Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensionsFunc, DeploymentMode deploymentMode)
    {
        this.routerExtensionsFunc = routerExtensionsFunc;
        this.deploymentMode = deploymentMode;
    }

    @POST
    @Path("generateElements")
    @ApiOperation(value = "Autogenerate Pure PureModelContextData JSON from schema")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateElements(ElementGenerationInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            PureModelContextData modelData = input.getModelData();
            String databasePath = input.getDatabasePath();
            PureModel model = new PureModel(modelData, profiles, this.deploymentMode);
            Database database = (Database) model.getStore(databasePath);
            RichIterable<? extends Schema> schemas = database._schemas();
            Schema schema = schemas.getFirst();
            String targetPackage = getTargetPackageFromDatabasePath(databasePath);
            RichIterable<? extends Root_meta_pure_extension_Extension> extensions = this.routerExtensionsFunc.apply(model);
            ExecutionSupport executionSupport = model.getExecutionSupport();
            String result = core_relational_relational_autogeneration_relationalToPure.Root_meta_relational_transform_autogen_classesAssociationsAndMappingFromSchema_Schema_1__String_1__Extension_MANY__String_1_(schema, targetPackage, extensions, executionSupport);
            return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to generate elements", ex);
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, profiles);
        }
    }

    private String getTargetPackageFromDatabasePath(String databasePath)
    {
        int lastColonIndex = databasePath.lastIndexOf(":");
        return databasePath.substring(0, lastColonIndex + 1);
    }
}