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

package org.finos.legend.engine.language.pure.modelManager.sdlc.workspace;

import org.apache.http.client.methods.HttpGet;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.MetadataServerPrivateAccessTokenConfiguration;
import org.finos.legend.engine.language.pure.modelManager.sdlc.configuration.ServerConnectionConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * JUnit 4, against the repo convention: a Jupiter engine here would switch this module's Surefire
 * provider and change how its existing tests are discovered.
 */
public class TestWorkspaceSDLCLoaderClientName
{
    private static final String URL = "http://sdlc.example.com:7000/api/projects/proj/workspaces/ws/pureModelContextData";

    @Test
    public void theClientNameParameterNameIsPartOfTheSdlcContract()
    {
        WorkspaceSDLCLoader loader = new WorkspaceSDLCLoader(privateAccessTokenConfiguration());

        // legend-shared's LegendClientFinder reads client_name and ignores force_client.
        assertEquals(URL + "?client_name=" + StubAuthenticationSourceProvider.CLIENT_NAME,
                loader.prepareHttpRequest(new Identity("alice"), URL).getURI().toString());
    }

    @Test
    public void unresolvableClientNameFailsRatherThanSendingTheLiteralNull()
    {
        WorkspaceSDLCLoader loader = new WorkspaceSDLCLoader(privateAccessTokenConfiguration());
        Identity identity = new Identity(StubAuthenticationSourceProvider.UNRESOLVABLE);

        EngineException e = assertThrows(EngineException.class, () -> loader.prepareHttpRequest(identity, URL));

        assertTrue(e.getMessage(), e.getMessage().contains(StubAuthenticationSourceProvider.UNRESOLVABLE));
        assertTrue(e.getMessage(), e.getMessage().contains("no authentication profile"));
    }

    @Test
    public void withoutPrivateAccessTokenConfigurationTheUrlIsUnchanged()
    {
        WorkspaceSDLCLoader loader = new WorkspaceSDLCLoader(new ServerConnectionConfiguration("sdlc.example.com", 7000));

        HttpGet request = loader.prepareHttpRequest(new Identity("alice"), URL);

        assertEquals(URL, request.getURI().toString());
    }

    private static ServerConnectionConfiguration privateAccessTokenConfiguration()
    {
        ServerConnectionConfiguration configuration = new ServerConnectionConfiguration("sdlc.example.com", 7000);
        MetadataServerPrivateAccessTokenConfiguration pac4j = new MetadataServerPrivateAccessTokenConfiguration();
        pac4j.accessTokenHeaderName = "X-Access-Token";
        configuration.pac4j = pac4j;
        return configuration;
    }
}
