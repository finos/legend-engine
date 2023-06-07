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

package org.finos.legend.engine.generation.relational.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.generation.relational.model.RelationalModelGeneration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
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
public class RelationalStoreModelGenerationApi
{

    private final ModelManager modelManager;


    public RelationalStoreModelGenerationApi(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("generateMapping")
    @ApiOperation(value = "Use JDBC connection to survey database metadata (schemas, tables, columns, etc.) and build database Pure model")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildDatabase(RelationalModelGenerationInput relationalModelGenerationInput, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try
        {
            PureModelContext model = relationalModelGenerationInput.model;;
            Pair<PureModelContextData, PureModel> res = modelManager.loadModelAndData(model, model instanceof PureModelContextPointer ? ((PureModelContextPointer) model).serializer.version : null, profiles, null);
            PureModelContextData generated = new RelationalModelGeneration(res, relationalModelGenerationInput.databasePath, relationalModelGenerationInput.schema,relationalModelGenerationInput.modelPackage, relationalModelGenerationInput.mappingPackage).build();
            return Response.ok(generated, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.COMPILE_ERROR, profiles);
        }
    }
}
