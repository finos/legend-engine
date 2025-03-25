// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.deephaven.connection;

import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.PSKAuthenticationSpecification;
import io.deephaven.uri.DeephavenTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class DeephavenSessionPSKProvider implements DeephavenSessionProvider
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DeephavenSessionPSKProvider.class);
    // TODO: Move target prefix to be a part of the SourceSpecification - and accommodate different types
    private final String targetPrefix = "dh+plain://";
    private final String authType = "io.deephaven.authentication.psk.PskAuthenticationHandler ";

    @Override
    public Optional<DeephavenSession> provide(PSKAuthenticationSpecification pskAuth, DeephavenSourceSpecification sourceSpec)
    {
        DeephavenSessionContext deephavenSessionContext = new DeephavenSessionContext(pskAuth, sourceSpec);
        return Optional.of(deephavenSessionContext)
                .filter(dsc -> dsc.pskAuth instanceof PSKAuthenticationSpecification)
                .filter(dsc -> dsc.sourceSpec instanceof DeephavenSourceSpecification)
                .map(this::createDeephavenSession);
    }

    private DeephavenSession createDeephavenSession(DeephavenSessionContext deephavenSessionContext)
    {
        DeephavenTarget target = DeephavenTarget.builder().host(deephavenSessionContext.sourceSpec.url.getHost()).port(deephavenSessionContext.sourceSpec.url.getPort()).isSecure(false).build();
        String authTypeAndPSK = this.authType + deephavenSessionContext.pskAuth.psk;
        return new DeephavenSession(target, authTypeAndPSK);
    }

    public class DeephavenSessionContext
    {
        private final PSKAuthenticationSpecification pskAuth;
        private final DeephavenSourceSpecification sourceSpec;

        private DeephavenSessionContext(PSKAuthenticationSpecification pskAuth, DeephavenSourceSpecification sourceSpec)
        {
            this.pskAuth = pskAuth;
            this.sourceSpec = sourceSpec;
        }
    }
}


