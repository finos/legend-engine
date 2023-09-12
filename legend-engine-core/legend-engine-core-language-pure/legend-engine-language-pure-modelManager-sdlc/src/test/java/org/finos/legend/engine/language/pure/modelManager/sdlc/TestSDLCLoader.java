// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.pure.modelManager.sdlc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import javax.security.auth.Subject;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class TestSDLCLoader
{
    @ClassRule
    public static WireMockClassRule wireMockServer = new WireMockClassRule();

    @Rule
    public WireMockClassRule rule = wireMockServer;

    private static final MockTracer tracer = new MockTracer();

    @BeforeClass
    public static void setUpClass()
    {
        Assert.assertTrue(GlobalTracer.registerIfAbsent(tracer));
    }

    @Before
    public void setUp()
    {
        MockSpan span = tracer.buildSpan("testSpan").start();
        tracer.scopeManager().activate(span);
    }

    @After
    public void tearDown()
    {
        tracer.reset();
    }

    @Test
    public void testSdlcLoaderRetriesOnSomeHttpResponses() throws Exception
    {
        PureModelContextPointer pointer = getPureModelContextPointer();

        configureWireMockForRetries();
        SDLCLoader sdlcLoader = createSDLCLoader();

        PureModelContextData pmcdLoaded = sdlcLoader.load(Lists.fixedSize.empty(), pointer, "v1_32_0", tracer.activeSpan());
        Assert.assertNotNull(pmcdLoaded);

        Object tries = tracer.finishedSpans()
                .stream()
                .filter(x -> x.operationName().equals("METADATA_REQUEST_ALLOY_PROJECT_START"))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Missing expected span"))
                .tags()
                .get("httpRequestTries");

        Assert.assertEquals(3, tries);
    }

    @Test
    public void testSdlcLoaderDoesNotRetryOnHardFailures() throws Exception
    {
        PureModelContextPointer pointer = getPureModelContextPointer();

        configureWireMockForNoRetries();
        SDLCLoader sdlcLoader = createSDLCLoader();

        try
        {
            sdlcLoader.load(Lists.fixedSize.empty(), pointer, "v1_32_0", tracer.activeSpan());
            Assert.fail("Should throw");
        }
        catch (EngineException e)
        {
            Assert.assertTrue(e.getMessage().contains("Engine was unable to load information from the Pure SDLC using"));
        }
    }

    private static PureModelContextPointer getPureModelContextPointer()
    {
        AlloySDLC sdlcInfo = new AlloySDLC();
        sdlcInfo.groupId = "groupId";
        sdlcInfo.artifactId = "artifactId";
        sdlcInfo.version = "1.0.0";

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;
        return pointer;
    }

    private SDLCLoader createSDLCLoader()
    {
        MetaDataServerConfiguration serverConfiguration = new MetaDataServerConfiguration();
        serverConfiguration.alloy = new ServerConnectionConfiguration();
        serverConfiguration.pure = new ServerConnectionConfiguration();

        serverConfiguration.alloy.host = "localhost";
        serverConfiguration.alloy.port = rule.port();
        serverConfiguration.alloy.prefix = "/alloy";

        serverConfiguration.pure.host = "localhost";
        serverConfiguration.pure.port = rule.port();
        serverConfiguration.pure.prefix = "/pure";

        return new SDLCLoader(serverConfiguration, Subject::new);
    }

    private static void configureWireMockForRetries() throws JsonProcessingException
    {
        PureModelContextData data = PureModelContextData.newPureModelContextData(new Protocol(), new PureModelContextPointer(), Lists.fixedSize.empty());
        String pmcdJson = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(data);

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=v1_32_0")
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.aResponse().withStatus(503).withBody("a failure"))
                .willSetStateTo("FAILED_1"));

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=v1_32_0")
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs("FAILED_1")
                .willReturn(WireMock.aResponse().withStatus(503).withBody("a failure"))
                .willSetStateTo("FAILED_2"));

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=v1_32_0")
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs("FAILED_2")
                .willReturn(WireMock.okJson(pmcdJson))
                .willSetStateTo("FAILED_3"));
    }

    private static void configureWireMockForNoRetries() throws JsonProcessingException
    {
        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=v1_32_0")
                .willReturn(WireMock.aResponse().withStatus(400).withBody("a failure")));
    }
}
