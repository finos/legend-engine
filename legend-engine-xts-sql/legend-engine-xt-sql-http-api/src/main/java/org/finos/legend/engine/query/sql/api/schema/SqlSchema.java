// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.query.sql.api.schema;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.pure.generated.core_external_query_sql_server_schema;
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

@Api(tags = "SQL - Schema")
@Path("sql/v1/schema")
public class SqlSchema
{
    private ModelManager modelManager;

    public SqlSchema(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("getSchema")
    @ApiOperation(value = "Provide available tables queryable by SQL expressions for a project.")
    @Consumes({MediaType.TEXT_PLAIN, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSchema(PureModelContext model,
                              @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        PureModel pureModel = this.modelManager.loadModel(model, PureClientVersions.production, identity, null);
        return Response.ok(core_external_query_sql_server_schema.Root_meta_external_query_sql_server_schema_transform_PackageableElement_MANY__AddressableRelationCollection_1_(pureModel.getPackageableElements(), pureModel.getExecutionSupport())).build();
    }

}
