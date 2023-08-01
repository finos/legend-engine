/**
 * Copyright (c) 2020-present, Goldman Sachs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.finos.legend.engine.api.testData.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.api.testData.generation.model.TestEmbeddedDataGenerationResult;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.testData.model.TestEmbeddedDataGenerationInput;
import org.finos.legend.testData.service.TestEmbeddedDataGenerationObjectMapperFactory;
import org.finos.legend.testData.service.TestEmbeddedDataGenerationServiceExtension;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import io.swagger.annotations.Api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "TestData - Generation")
@Path("pure/v1/testData/generation")
@Produces(MediaType.APPLICATION_JSON)
public class TestEmbeddedDataGeneration
{
    private static final ObjectMapper objectMapper = TestEmbeddedDataGenerationObjectMapperFactory.getNewObjectMapper();
    private final ModelManager modelManager;
    private List<TestEmbeddedDataGenerationServiceExtension> testEmbeddedDataGenerationServiceExtensions;

    public TestEmbeddedDataGeneration(ModelManager modelManager, List<TestEmbeddedDataGenerationServiceExtension> testEmbeddedDataGenerationServiceExtensions)
    {
        this.modelManager = modelManager;
        this.testEmbeddedDataGenerationServiceExtensions = testEmbeddedDataGenerationServiceExtensions;
    }

    @POST
    @Path("DONOTUSE_generateData")
    @ApiOperation(value = "Studio WIP: will not be backward compatible until we remove the DONOTUSE flag")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response generateEmbeddedData(TestEmbeddedDataGenerationInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        PureModel pureModel = modelManager.loadModel(input.model, input.clientVersion == null ? PureClientVersions.production : input.clientVersion, profiles, null);
        try (Scope scope = GlobalTracer.get().buildSpan("generate test embedded data").startActive(true))
        {
            List<EmbeddedData> embeddedData = LazyIterate.flatCollect(this.testEmbeddedDataGenerationServiceExtensions, extension -> extension.generateTestEmbeddedData(input, pureModel)).toList();
            return ManageConstantResult.manageResult(profiles, new TestEmbeddedDataGenerationResult(embeddedData), objectMapper);
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.TEST_DATA_GENERATION_ERROR, Response.Status.BAD_REQUEST, profiles);
        }
    }
}
