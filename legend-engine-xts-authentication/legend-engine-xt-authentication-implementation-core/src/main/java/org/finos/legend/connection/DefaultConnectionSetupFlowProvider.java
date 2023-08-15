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

package org.finos.legend.connection;

import org.finos.legend.engine.shared.core.identity.Credential;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultConnectionSetupFlowProvider implements ConnectionSetupFlowProvider
{
    private final Map<ConnectionSetupFlowKey, ConnectionFactoryFlow<?, ?, ?>> flows = new ConcurrentHashMap<>();

    @Override
    public Optional<ConnectionFactoryFlow> lookupFlow(ConnectionSetupSpecification connectionSetupSpecification, Credential credential)
    {
        return Optional.ofNullable(this.flows.get(new ConnectionSetupFlowKey(connectionSetupSpecification.getClass(), credential.getClass())));
    }

    @Override
    public void configure()
    {
        // TODO?: @akphi should we use service loader or have a collector/preset like LegendDefaultDatabaseAuthenticationFlowProvider
        for (ConnectionFactoryFlow<?, ?, ?> flow : ServiceLoader.load(ConnectionFactoryFlow.class))
        {
            // TODO?: take care of clash
            this.flows.put(new ConnectionSetupFlowKey(flow.getConnectionSetupSpecificationClass(), flow.getCredentialClass()), flow);
        }
    }
}
