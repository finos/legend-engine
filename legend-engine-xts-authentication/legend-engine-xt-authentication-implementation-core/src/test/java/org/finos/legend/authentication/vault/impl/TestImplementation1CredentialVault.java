/*
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

import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.authentication.vault.CredentialVault;
import org.finos.legend.authentication.vault.CredentialVaultType;

public class TestImplementation1CredentialVault extends CredentialVault
{
    private ImmutableMap<String, String> map;

    public TestImplementation1CredentialVault(ImmutableMap<String, String> map)
    {
        this.map = map;
    }

    @Override
    public String getSecret(String reference, Object context) throws Exception
    {
        return this.map.get(reference);
    }

    @Override
    public CredentialVaultType getType()
    {
        return CredentialVaultType.TEST_IMPLEMENTATION1;
    }
}
*/
