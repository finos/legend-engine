// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.generation.stores.graphQLStore.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.GraphQLStoreModelGenerationInput;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.GraphQLStoreModelGenerationResult;
import org.finos.legend.engine.plan.generation.stores.graphQLStore.GraphQLStoreModelGenerator;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "GraphQL Store - Generation")
@Path("pure/v1/graphQL")
@Produces(MediaType.APPLICATION_JSON)
public class GraphQLGenerationApi
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(GraphQLGenerationApi.class);

    private final GraphQLStoreModelGenerator generator;

    public GraphQLGenerationApi(CredentialProviderProvider credentialProviderProvider)
    {
        this.generator = new GraphQLStoreModelGenerator(credentialProviderProvider);
    }

    @POST
    @Path("generateModelsFromIntrospection")
    @Operation(summary = "Generates Class(es), a Binding and a Mapping for a GraphQL store from live schema introspection")
    @Consumes({MediaType.APPLICATION_JSON})
    public Response generateModelsFromIntrospection(GraphQLStoreModelGenerationInput input, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        long start = System.currentTimeMillis();
        LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_GRAPHQL_STORE_MODEL_START).toString());
        try
        {
            GraphQLStoreModelGenerationResult result = generator.generate(input, identity);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_GRAPHQL_STORE_MODEL_STOP, (double) System.currentTimeMillis() - start).toString());
            return ManageConstantResult.manageResult(identity.getName(), result);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_GRAPHQL_STORE_MODEL_ERROR, identity.getName());
        }
    }
}
