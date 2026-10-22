// Copyright 2025 Goldman Sachs
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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.prometheus.MetricsHandler;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_m3_function_LambdaFunction;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
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

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;
import static org.finos.legend.pure.generated.core_pure_tds_relation_tdsToRelation.Root_meta_pure_tds_toRelation_transform_LambdaFunction_1__Extension_MANY__LambdaFunction_1_;

@Tag(name = "Pure - Autofix")
@Path("pure/v1/compilation/autofix")
@Produces(MediaType.APPLICATION_JSON)
public class Autofix
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Compile.class);
    private final ModelManager modelManager;

    public Autofix(ModelManager modelManager)
    {
        this.modelManager = modelManager;
        MetricsHandler.createMetrics(this.getClass());
    }

    @POST
    @Path("transformTdsToRelation/lambda")
    @Operation(summary = "Transform lambda from TDS protocol to relation protocol")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response transformTdsToRelationLambda(LambdaTdsToRelationInput lambdaTdsToRelationInput, @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        Identity identity = Identity.makeIdentity(profiles);
        PureModelContext model = lambdaTdsToRelationInput.model;
        PureModel pureModel = modelManager.loadModel(model, model instanceof PureModelContextPointer ? ((PureModelContextPointer) model).serializer.version : null, identity, null);
        org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction lambda = lambdaTdsToRelationInput.lambda;
        LambdaFunction<?> lambdaFunction = HelperValueSpecificationBuilder.buildLambda(lambda.body, lambda.parameters, pureModel.getContext());
        RichIterable<? extends
                Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
        Root_meta_protocols_pure_vX_X_X_metamodel_m3_function_LambdaFunction transformedRawLambda =
                Root_meta_pure_tds_toRelation_transform_LambdaFunction_1__Extension_MANY__LambdaFunction_1_(
                        lambdaFunction,
                        extensions,
                        pureModel.getExecutionSupport()
                );
        String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(transformedRawLambda, pureModel.getExecutionSupport());
        try
        {
            org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction transformedLambda = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(json, org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction.class);
            return Response.ok(transformedLambda, MediaType.APPLICATION_JSON_TYPE).build();
        }
        catch (JsonProcessingException e)
        {
            return Response.status(500).entity(e).build();
        }
    }
}
