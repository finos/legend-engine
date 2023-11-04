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

package org.finos.legend.connection.impl;

import org.finos.legend.connection.protocol.AuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

public class EncryptedPrivateKeyPairAuthenticationConfiguration extends AuthenticationConfiguration
{
    public String userName;
    public CredentialVaultSecret privateKey;
    public CredentialVaultSecret passphrase;

    public EncryptedPrivateKeyPairAuthenticationConfiguration()
    {
    }

    public EncryptedPrivateKeyPairAuthenticationConfiguration(String userName, CredentialVaultSecret privateKey, CredentialVaultSecret passphrase)
    {
        this.userName = userName;
        this.privateKey = privateKey;
        this.passphrase = passphrase;
    }

    @Override
    public String shortId()
    {
        return "EncryptedPrivateKeyPair" +
                "--userName=" + userName +
                "--privateKey=" + privateKey.shortId() +
                "--passphrase=" + passphrase.shortId();
    }
}
