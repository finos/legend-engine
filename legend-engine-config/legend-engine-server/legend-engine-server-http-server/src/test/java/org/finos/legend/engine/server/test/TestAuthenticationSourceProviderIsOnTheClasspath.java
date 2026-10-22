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

package org.finos.legend.engine.server.test;

import org.finos.legend.engine.shared.core.identity.factory.AuthenticationSourceProvider;
import org.junit.Test;

import java.util.ServiceLoader;

import static org.junit.Assert.assertTrue;

public class TestAuthenticationSourceProviderIsOnTheClasspath
{
    @Test
    public void theServerCanSupplyTheGraphFetchAuthenticationArgument()
    {
        assertTrue("ExecutionNodeExecutor throws on the first platform-bound graph fetch without "
                        + "one; add legend-engine-xt-identity-pac4j to this assembly's runtime classpath",
                ServiceLoader.load(AuthenticationSourceProvider.class).iterator().hasNext());
    }
}
