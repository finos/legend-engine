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

package org.finos.legend.engine.external.format.protobuf.generation.descriptors;

import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.ProtobufGenerationService;
import org.finos.legend.engine.external.format.protobuf.deprecated.generation.configuration.ProtobufGenerationInput;
import org.finos.legend.engine.external.format.protobuf.generation.descriptors.service.FileService;
import org.finos.legend.engine.external.format.protobuf.generation.descriptors.service.ProtobufCompilerService;
import org.finos.legend.engine.external.format.protobuf.generation.descriptors.service.ProtobufDescriptorGenerationService;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Z - Generation - Schema")
@Path("pure/v1/schemaGeneration")
@Produces(MediaType.APPLICATION_OCTET_STREAM)
public class ProtobufDescriptorGenerationController
{
    private final ProtobufDescriptorGenerationService protobufDescriptorGenerationService;

    public ProtobufDescriptorGenerationController(ModelManager modelManager)
    {
        protobufDescriptorGenerationService =
            new ProtobufDescriptorGenerationService(new ProtobufGenerationService(modelManager),
                new FileService(),
                new ProtobufCompilerService());
    }

    @POST
    @Path("protobuf-descriptors")
    @ApiOperation(value = "Generates Protobuf descriptors for a given class and transitive dependencies")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateProtobuf(ProtobufGenerationInput generateProtobufInput,
                                     @ApiParam(hidden = true) @Pac4JProfileManager
                                         ProfileManager<CommonProfile> pm)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(pm);
        try (Scope scope = GlobalTracer.get().buildSpan("Service: Generate Protobuf Descriptor").startActive(true))
        {
            byte[] descriptor = protobufDescriptorGenerationService.generateDescriptor(generateProtobufInput, pm);
            return Response.ok(descriptor).type(MediaType.APPLICATION_OCTET_STREAM).build();
        } catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_PROTOBUF_DESCRIPTOR_ERROR, profiles);
        }
    }
}
