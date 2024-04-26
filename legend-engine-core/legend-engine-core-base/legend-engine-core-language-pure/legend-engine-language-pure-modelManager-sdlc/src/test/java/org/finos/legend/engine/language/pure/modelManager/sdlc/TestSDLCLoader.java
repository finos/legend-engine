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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetaDataServerConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.AlloySDLC;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.WorkspaceSDLC;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
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

    private static final String CLIENT_VERSION = "v1_33_0";

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

        PureModelContextData pmcdLoaded = sdlcLoader.load(IdentityFactoryProvider.getInstance().getAnonymousIdentity(), pointer, CLIENT_VERSION, tracer.activeSpan());
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
            sdlcLoader.load(IdentityFactoryProvider.getInstance().getAnonymousIdentity(), pointer, CLIENT_VERSION, tracer.activeSpan());
            Assert.fail("Should throw");
        }
        catch (EngineException e)
        {
            Assert.assertTrue(e.getMessage().contains("Engine was unable to load information from the Pure SDLC using"));
        }
    }

    @Test
    public void testSdlcLoaderForWorkspacesWithoutDependency() throws Exception
    {
        WorkspaceSDLC sdlcInfo = new WorkspaceSDLC();
        sdlcInfo.project = "proj-1234";
        sdlcInfo.isGroupWorkspace = true;
        sdlcInfo.version = "workspaceAbc";

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;

        configureWireMockForRetries();
        SDLCLoader sdlcLoader = createSDLCLoader();
        PureModelContextData pmcdLoaded = sdlcLoader.load(IdentityFactoryProvider.getInstance().getAnonymousIdentity(), pointer, CLIENT_VERSION, tracer.activeSpan());
        Assert.assertNotNull(pmcdLoaded);
        Assert.assertEquals(1, pmcdLoaded.getElements().size());
        Assert.assertEquals("pkg::pkg::myClass", pmcdLoaded.getElements().get(0).getPath());
    }

    @Test
    public void testSdlcLoaderForWorkspacesWithDependency() throws Exception
    {
        WorkspaceSDLC sdlcInfo = new WorkspaceSDLC();
        sdlcInfo.project = "proj-1235";
        sdlcInfo.isGroupWorkspace = false;
        sdlcInfo.version = "workspaceAbc";

        PureModelContextPointer pointer = new PureModelContextPointer();
        pointer.sdlcInfo = sdlcInfo;

        configureWireMockForRetries();
        SDLCLoader sdlcLoader = createSDLCLoader();

        ModelManager modelManager = new ModelManager(DeploymentMode.TEST, tracer, sdlcLoader);

        PureModelContextData pmcdLoaded = modelManager.loadData(pointer, CLIENT_VERSION, IdentityFactoryProvider.getInstance().getAnonymousIdentity());

        Assert.assertNotNull(pmcdLoaded);
        Assert.assertEquals(2, pmcdLoaded.getElements().size());

        List<String> paths = pmcdLoaded.getElements().stream().map(PackageableElement::getPath).sorted().collect(Collectors.toList());
        Assert.assertEquals("pkg::pkg::myAnotherClass", paths.get(0));
        Assert.assertEquals("pkg::pkg::myClass", paths.get(1));
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
        serverConfiguration.sdlc = new ServerConnectionConfiguration();

        serverConfiguration.alloy.host = "localhost";
        serverConfiguration.alloy.port = rule.port();
        serverConfiguration.alloy.prefix = "/alloy";

        serverConfiguration.pure.host = "localhost";
        serverConfiguration.pure.port = rule.port();
        serverConfiguration.pure.prefix = "/pure";

        serverConfiguration.sdlc.host = "localhost";
        serverConfiguration.sdlc.port = rule.port();
        serverConfiguration.sdlc.prefix = "/sdlc";
        serverConfiguration.sdlc.scheme = "http";

        return new SDLCLoader(serverConfiguration, Subject::new);
    }

    private static void configureWireMockForRetries() throws JsonProcessingException
    {
        ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

        PureModelContextData data = PureModelContextData.newPureModelContextData(new Protocol(), new PureModelContextPointer(), Lists.fixedSize.empty());
        String pmcdJson = objectMapper.writeValueAsString(data);

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=" + CLIENT_VERSION)
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(WireMock.aResponse().withStatus(503).withBody("a failure"))
                .willSetStateTo("FAILED_1"));

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=" + CLIENT_VERSION)
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs("FAILED_1")
                .willReturn(WireMock.aResponse().withStatus(503).withBody("a failure"))
                .willSetStateTo("FAILED_2"));

        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=" + CLIENT_VERSION)
                .inScenario("RETRY_FAILURES")
                .whenScenarioStateIs("FAILED_2")
                .willReturn(WireMock.okJson(pmcdJson))
                .willSetStateTo("FAILED_3"));


        Class t = new Class();
        t.name = "myClass";
        t._package = "pkg::pkg";
        PureModelContextData data2 = PureModelContextData.newPureModelContextData(new Protocol(), new PureModelContextPointer(), Lists.fixedSize.with(t));
        String pmcdJson2 = objectMapper.writeValueAsString(data2);

        Class t2 = new Class();
        t2.name = "myAnotherClass";
        t2._package = "pkg::pkg";
        PureModelContextData dataDep = PureModelContextData.newPureModelContextData(new Protocol(), new PureModelContextPointer(), Lists.fixedSize.with(t2));
        String pmcdJsonDep = objectMapper.writeValueAsString(dataDep);

        WireMock.stubFor(WireMock.get("/sdlc/api/projects/proj-1234/groupWorkspaces/workspaceAbc/pureModelContextData")
                .willReturn(WireMock.okJson(pmcdJson2)));

        WireMock.stubFor(WireMock.get("/sdlc/api/projects/proj-1234/groupWorkspaces/workspaceAbc/revisions/HEAD/upstreamProjects")
                .willReturn(WireMock.okJson("[]")));

        WireMock.stubFor(WireMock.get("/sdlc/api/projects/proj-1235/workspaces/workspaceAbc/pureModelContextData")
                .willReturn(WireMock.okJson(pmcdJson2)));

        WireMock.stubFor(WireMock.get("/sdlc/api/projects/proj-1235/workspaces/workspaceAbc/revisions/HEAD/upstreamProjects")
                .willReturn(WireMock.okJson("[{\"projectId\": \"org.finos.legend.dependency:models\",\"versionId\": \"2.0.1\"}]")));

        WireMock.stubFor(WireMock.get("/alloy/projects/org.finos.legend.dependency/models/versions/2.0.1/pureModelContextData?clientVersion=" + CLIENT_VERSION)
                .willReturn(WireMock.okJson(pmcdJsonDep)));
    }

    private static void configureWireMockForNoRetries() throws JsonProcessingException
    {
        WireMock.stubFor(WireMock.get("/alloy/projects/groupId/artifactId/versions/1.0.0/pureModelContextData?clientVersion=" + CLIENT_VERSION)
                .willReturn(WireMock.aResponse().withStatus(400).withBody("a failure")));
    }
}
