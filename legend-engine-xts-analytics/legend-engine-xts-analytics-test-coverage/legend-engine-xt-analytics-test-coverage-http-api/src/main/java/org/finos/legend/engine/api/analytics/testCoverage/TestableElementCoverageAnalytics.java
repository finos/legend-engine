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

package org.finos.legend.engine.api.analytics.testCoverage;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementCoverageInput;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementCoverageResult;
import org.finos.legend.engine.api.analytics.testCoverage.model.TestableElementInfo;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.http.InflateInterceptor;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.testable.api.TestableElementCoverageAnalyzer;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "Analytics - Test Coverage")
@Path("pure/v1/analytics/testCoverage")
public class TestableElementCoverageAnalytics
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    private final ModelManager modelManager;

    public TestableElementCoverageAnalytics(ModelManager modelManager)
    {
        this.modelManager = modelManager;
    }

    @POST
    @Path("testableElementCoverage")
    @ApiOperation(value = "Analyze the testable element coverage for services and mappings in a project")
    @Consumes({MediaType.APPLICATION_JSON, InflateInterceptor.APPLICATION_ZLIB})
    @Produces(MediaType.APPLICATION_JSON)
    public Response analyzeTestableElementCoverage(TestableElementCoverageInput input,
                                                   @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> pm)
    {
        Identity identity = Identity.makeIdentity(ProfileManagerHelper.extractProfiles(pm));
        PureModelContextData pureModelContextData = this.modelManager.loadData(input.model, input.clientVersion, identity);

        try (Scope scope = GlobalTracer.get().buildSpan("Analytics: testable element coverage").startActive(true))
        {
            try
            {
                org.finos.legend.engine.testable.api.model.TestableElementCoverageResult analyzed = TestableElementCoverageAnalyzer.analyze(pureModelContextData);
                TestableElementCoverageResult result = toLocalResult(analyzed);
                return ManageConstantResult.manageResult(identity.getName(), result, objectMapper);
            }
            catch (Exception e)
            {
                return ExceptionTool.exceptionManager(e, LoggingEventType.ANALYTICS_ERROR, Response.Status.BAD_REQUEST, identity.getName());
            }
        }
    }

    static TestableElementCoverageResult toLocalResult(org.finos.legend.engine.testable.api.model.TestableElementCoverageResult analyzed)
    {
        Map<String, List<TestableElementInfo>> elementsByType = analyzed.elementsByType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(info -> new TestableElementInfo(info.path, info.type, info.hasTestSuites, info.hasLegacyTests, info.testSuiteCount))
                        .collect(Collectors.toList())));
        return new TestableElementCoverageResult(elementsByType);
    }
}

