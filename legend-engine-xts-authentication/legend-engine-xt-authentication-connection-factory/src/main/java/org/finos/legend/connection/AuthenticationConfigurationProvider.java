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

import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.shared.core.identity.Identity;

/**
 * TODO: @akphi - not sure if this is the right construct, but sometimes, we need something
 * like this to provide the authentication configurations as we don't want users to have to provide
 * us one for simplicity sake or security sake (such as in the case of data push server).
 */
public abstract class AuthenticationConfigurationProvider
{
    protected final LegendEnvironment environment;
    protected final StoreInstanceProvider storeInstanceProvider;

    public AuthenticationConfigurationProvider(StoreInstanceProvider storeInstanceProvider, LegendEnvironment environment)
    {
        this.environment = environment;
        this.storeInstanceProvider = storeInstanceProvider;
    }

    public abstract AuthenticationConfiguration lookup(Identity identity, StoreInstance storeInstance);
}
