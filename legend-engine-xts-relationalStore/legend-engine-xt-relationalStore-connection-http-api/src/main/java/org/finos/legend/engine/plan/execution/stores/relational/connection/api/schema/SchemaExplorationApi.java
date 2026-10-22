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

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema.model.RawSQLExecuteInput;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.exploration.SchemaExportation;
import org.finos.legend.engine.plan.execution.stores.relational.exploration.model.DatabaseBuilderInput;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Tag(name = "Utilities - Database")
@Path("pure/v1/utilities/database")
public class SchemaExplorationApi
{
    private final ConnectionManagerSelector connectionManager;


    @Deprecated
    public SchemaExplorationApi(ModelManager modelManager, RelationalStoreExecutor relationalStoreExecutor)
    {
        this(relationalStoreExecutor);
    }

    public SchemaExplorationApi(RelationalStoreExecutor relationalStoreExecutor)
    {
        this.connectionManager = relationalStoreExecutor.getStoreState().getRelationalExecutor().getConnectionManager();
    }

    @POST
    @Path("schemaExploration")
    @Operation(summary = "Use JDBC connection to survey database metadata (schemas, tables, columns, etc.) and build database Pure model")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildDatabase(DatabaseBuilderInput databaseBuilderInput, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            SchemaExportation databaseBuilder = SchemaExportation.newBuilder(this.connectionManager);
            Database database = databaseBuilder.build(databaseBuilderInput, identity);
            PureModelContextData graph = PureModelContextData.newBuilder().withElement(database).build();
            return Response.ok(graph, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.COMPILE_ERROR, identity.getName());
        }
    }

    @POST
    @Path("executeRawSQL")
    @Operation(summary = "Use JDBC connection to execute SQL (this API is meant for non-production use case such exploring/previewing data, its result set is capped)")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.TEXT_PLAIN)
    public Response executeRawSQL(RawSQLExecuteInput input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        try
        {
            String result = new AdhocSQLExecutor().executeRawSQL(this.connectionManager, input.connection, input.sql, identity);
            return Response.ok(result).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.USER_EXECUTION_ERROR, identity.getName());
        }
    }
}
