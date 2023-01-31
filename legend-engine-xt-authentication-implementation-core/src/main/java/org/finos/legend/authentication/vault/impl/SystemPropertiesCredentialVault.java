// Copyright 2021 Goldman Sachs
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

package org.finos.legend.authentication.vault.impl;

import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.shared.core.identity.Identity;

public class SystemPropertiesCredentialVault extends CredentialVault<SystemPropertiesSecret>
{
    public SystemPropertiesCredentialVault()
    {
    }

    @Override
    public String lookupSecret(SystemPropertiesSecret vaultSecret, Identity identity) throws Exception
    {
        String systemPropertyName = vaultSecret.systemPropertyName;
        if (System.getProperty(systemPropertyName) == null)
        {
            throw new RuntimeException("Secret not found in system properties. System property name=" + systemPropertyName);
        }
        return System.getProperty(systemPropertyName);
    }
}
