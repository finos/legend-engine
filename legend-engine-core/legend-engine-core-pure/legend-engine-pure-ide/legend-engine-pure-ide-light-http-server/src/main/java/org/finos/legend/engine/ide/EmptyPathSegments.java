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

package org.finos.legend.engine.ide;

import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.server.ServerFactory;
import io.dropwizard.core.server.SimpleServerFactory;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import org.eclipse.jetty.http.UriCompliance;

import java.util.Collections;
import java.util.List;

/**
 * Pure IDE file paths start with {@code /}, so clients request {@code /fileAsJson//welcome.pure}. Jetty 12
 * rejects that empty segment with a 400 unless both the connector and the servlet layer allow it. The
 * connector mode is set here rather than in the configuration file because a custom mode such as
 * {@code "RFC3986,AMBIGUOUS_EMPTY_SEGMENT"} deserializes to {@code null}; only named modes survive.
 */
final class EmptyPathSegments
{
    private EmptyPathSegments()
    {
    }

    static void allow(ServerFactory serverFactory, Environment environment)
    {
        for (ConnectorFactory connector : applicationConnectors(serverFactory))
        {
            if (connector instanceof HttpConnectorFactory)
            {
                HttpConnectorFactory httpConnector = (HttpConnectorFactory) connector;
                httpConnector.setUriCompliance(httpConnector.getUriCompliance().with("PURE_IDE", UriCompliance.Violation.AMBIGUOUS_EMPTY_SEGMENT));
            }
        }
        environment.getApplicationContext().getServletHandler().setDecodeAmbiguousURIs(true);
    }

    private static List<ConnectorFactory> applicationConnectors(ServerFactory serverFactory)
    {
        if (serverFactory instanceof SimpleServerFactory)
        {
            return Collections.singletonList(((SimpleServerFactory) serverFactory).getConnector());
        }
        if (serverFactory instanceof DefaultServerFactory)
        {
            return ((DefaultServerFactory) serverFactory).getApplicationConnectors();
        }
        return Collections.emptyList();
    }
}
