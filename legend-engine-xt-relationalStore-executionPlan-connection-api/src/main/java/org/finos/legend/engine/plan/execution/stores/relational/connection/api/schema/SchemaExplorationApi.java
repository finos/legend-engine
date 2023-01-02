// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema;

import io.swagger.annotations.Api;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema.model.DatabaseBuilderInput;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Utilities - Database")
@Path("pure/v1/utilities/database")
@Produces(MediaType.APPLICATION_JSON)

public class SchemaExplorationApi
{

    private final ModelManager modelManager;
    private final ConnectionManagerSelector connectionManager;


    public SchemaExplorationApi(ModelManager modelManager, RelationalStoreExecutor relationalStoreExecutor)
    {
        this.modelManager = modelManager;
        this.connectionManager = relationalStoreExecutor.getStoreState().getRelationalExecutor().getConnectionManager();
    }


    @Path("schemaExploration")
    @POST
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response buildDatabase(DatabaseBuilderInput databaseBuilderInput, @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(pm));

        try
        {

            SchemaExportation databaseBuilder = SchemaExportation.newBuilder(databaseBuilderInput);
            Database database = databaseBuilder.build(this.connectionManager, profiles);
            PureModelContextData graph = PureModelContextData.newBuilder().withElement(database).build();
            return Response.ok(graph, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.COMPILE_ERROR, user);
        }
    }

}
