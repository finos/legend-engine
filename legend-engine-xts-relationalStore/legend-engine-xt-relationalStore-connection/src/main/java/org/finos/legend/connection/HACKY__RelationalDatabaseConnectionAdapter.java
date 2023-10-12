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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;

/**
 * NOTE: this is hacky way of us to realize the relational database connection that we can support
 * in the new connection framework (i.e. using {@link ConnectionFactory}), when we have a more
 * solid strategy in place for migration to this new framework, we should then remove this mechanism
 */
public interface HACKY__RelationalDatabaseConnectionAdapter
{
    ConnectionFactoryMaterial adapt(RelationalDatabaseConnection relationalDatabaseConnection, Identity identity, LegendEnvironment environment);

    class ConnectionFactoryMaterial
    {
        public final StoreInstance storeInstance;
        public final AuthenticationConfiguration authenticationConfiguration;

        public ConnectionFactoryMaterial(StoreInstance storeInstance, AuthenticationConfiguration authenticationConfiguration)
        {
            this.storeInstance = storeInstance;
            this.authenticationConfiguration = authenticationConfiguration;
        }
    }
}
