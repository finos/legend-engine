// Copyright 2022 Goldman Sachs
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.result.ManageConstantResult;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.engine.testable.TestableRunner;
import org.finos.legend.engine.testable.model.RunTestsInput;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.finos.legend.engine.testable.model.RunTestsResult;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.jax.rs.annotations.Pac4JProfileManager;
import org.slf4j.Logger;

import static org.finos.legend.engine.shared.core.operational.http.InflateInterceptor.APPLICATION_ZLIB;

@Api(tags = "Testing")
@Path("pure/v1/testable")
@Produces(MediaType.APPLICATION_JSON)
public class Testable
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private final TestableRunner testableRunner;

    private final ObjectMapper objectMapper;


    public Testable(ModelManager modelManager)
    {
        this.testableRunner = new TestableRunner(modelManager);
        this.objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    }

    @POST
    @Path("runTests")
    @ApiOperation(value = "Run tests on testables")
    @Consumes({MediaType.APPLICATION_JSON, APPLICATION_ZLIB})
    public Response doTests(RunTestsInput input, @ApiParam(hidden = true) @Pac4JProfileManager ProfileManager<CommonProfile> profileManager)
    {
        MutableList<CommonProfile> profiles = ProfileManagerHelper.extractProfiles(profileManager);
        String user = SubjectTools.getPrincipal(ProfileManagerHelper.extractSubject(profiles));
        try
        {
            LOGGER.info(new LogInfo(user, LoggingEventType.TESTABLE_DO_TESTS_START, "").toString());
            RunTestsResult runTestsResult = testableRunner.doTests(input, profiles);
            LOGGER.info(new LogInfo(user, LoggingEventType.TESTABLE_DO_TESTS_STOP, "").toString());
            return ManageConstantResult.manageResult(profiles, runTestsResult, objectMapper);
        }
        catch (Exception e)
        {
            return ExceptionTool.exceptionManager(e, LoggingEventType.TESTABLE_DO_TESTS_ERROR, user);
        }
    }
}
