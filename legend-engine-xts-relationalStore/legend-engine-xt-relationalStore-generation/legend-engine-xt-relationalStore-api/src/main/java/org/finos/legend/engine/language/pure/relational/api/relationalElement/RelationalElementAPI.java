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

package org.finos.legend.engine.language.pure.relational.api.relationalElement;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.relational.api.relationalElement.input.DatabaseToModelGenerationInput;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.core_relational_relational_autogeneration_relationalToPure;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Database;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "Pure - Relational")
@Path("pure/v1/relational")
public class RelationalElementAPI
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RelationalElementAPI.class);
    private static final RelationalConnectionDbAuthenticationFlows dbDatasourceAuth = new RelationalConnectionDbAuthenticationFlows();
    private final DeploymentMode deploymentMode;
    private final ConnectionManagerSelector connectionManager;

    public RelationalElementAPI(DeploymentMode deploymentMode, RelationalStoreExecutor relationalStoreExecutor)
    {
        this.deploymentMode = deploymentMode;
        if (relationalStoreExecutor == null)
        {
            this.connectionManager = null;
        }
        else
        {
            this.connectionManager = relationalStoreExecutor.getStoreState().getRelationalExecutor().getConnectionManager();
        }
    }

    @POST
    @Path("generateModelsFromDatabaseSpecification")
    @ApiOperation(value = "Autogenerate models JSON from database specification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateModelsFromDatabaseSpecification(DatabaseToModelGenerationInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        try
        {
            PureModelContextData modelData = input.getModelData();
            String databasePath = input.getDatabasePath();
            PureModel model = new PureModel(modelData, identity.getName(), this.deploymentMode);
            String inputTargetPackage = input.getTargetPackage();
            Database database = (Database) model.getStore(databasePath);
            String targetPackage = (inputTargetPackage == null || inputTargetPackage.isEmpty()) ? getTargetPackageFromDatabasePath(databasePath) : inputTargetPackage;
            ExecutionSupport executionSupport = model.getExecutionSupport();
            String result = core_relational_relational_autogeneration_relationalToPure.Root_meta_relational_transform_autogen_classesAssociationsAndMappingFromDatabase_Database_1__String_1__String_1_(database, targetPackage, executionSupport);
            return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to generate models from database specifications", ex);
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, identity.getName());
        }
    }

    @GET
    @Path("connection/supportedDbAuthenticationFlows")
    @ApiOperation(value = "Get all available Database Authentication Flows")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDbDataSourceAuthComb(@Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentity(profiles);
        try
        {
            List<DbTypeDataSourceAuth> dbTypeDataSourceAuthCombinations = new ArrayList<>();
            if (this.connectionManager.getFlowProviderHolder().isPresent())
            {
                dbTypeDataSourceAuthCombinations = dbDatasourceAuth.getDbTypeDataSourceAndAuthCombos(this.connectionManager.getFlowProviderHolder().get().getFlows());
            }

            return Response.status(200)
                    .entity(dbTypeDataSourceAuthCombinations).build();
        }
        catch (Exception ex)
        {
            LOGGER.error("Failed to fetch Database Authentication Flows", ex);
            return ExceptionTool.exceptionManager(ex, LoggingEventType.CATCH_ALL, identity.getName());
        }
    }

    private String getTargetPackageFromDatabasePath(String databasePath)
    {
        int lastColonIndex = databasePath.lastIndexOf("::");
        return databasePath.substring(0, lastColonIndex);
    }
}
