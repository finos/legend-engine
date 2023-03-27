// Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.postgres.auth;

import org.finos.legend.engine.postgres.ConnectionProperties;
import org.finos.legend.engine.shared.core.identity.Identity;

public interface AuthenticationMethod
{

    /**
     * @param userName the userName sent with the startup message
     * @param passwd   the password in clear-text or null
     * @return the user or null; null should be handled as if it's a "guest" user
     * @throws RuntimeException if the authentication failed
     */

    Identity authenticate(String userName, SecureString passwd, ConnectionProperties connProperties);

    /**
     * @return unique name of the authentication method
     */
    AuthenticationMethodType name();
}
