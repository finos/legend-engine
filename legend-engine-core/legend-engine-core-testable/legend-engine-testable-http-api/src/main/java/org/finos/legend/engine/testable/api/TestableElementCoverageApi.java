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

package org.finos.legend.engine.testable.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.finos.legend.engine.testable.api.model.TestableElementCoverageResult;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Testable")
@Path("pure/v1/testable")
public class TestableElementCoverageApi
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private final ModelManager modelManager;

    public TestableElementCoverageApi(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @GET
    @Path("coverage/{groupId}/{artifactId}/{versionId}")
    @Operation(summary = "Analyze the testable element coverage for services and mappings in a project")
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeTestableElementCoverage(@PathParam("groupId") String groupId,
                                                   @PathParam("artifactId") String artifactId,
                                                   @PathParam("versionId") String versionId,
                                                   @QueryParam("clientVersion") String clientVersion,
                                                   @Parameter(hidden = true) @Pac4JProfileManager ProfileManager pm)
    {
        Identity identity = Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm));

        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = groupId;
        sdlcInfo.artifactId = artifactId;
        sdlcInfo.version = versionId;

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;

        PureModelContextData pureModelContextData = this.modelManager.loadData(pointer, clientVersion, identity);

        try (Scope scope = GlobalTracer.get().buildSpan("Testable: element coverage analysis").startActive(true))
        {
            try
            {
                TestableElementCoverageResult result = TestableElementCoverageAnalyzer.analyze(pureModelContextData);
                return ManageConstantResult.manageResult(identity.getName(), result, objectMapper);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }
}
