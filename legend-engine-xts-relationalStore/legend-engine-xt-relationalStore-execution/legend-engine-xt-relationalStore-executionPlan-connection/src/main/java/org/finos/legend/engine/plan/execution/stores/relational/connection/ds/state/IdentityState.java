//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state;

import org.finos.legend.engine.authentication.credential.CredentialSupplier;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Objects;
import java.util.Optional;

public class IdentityState
{
    private final Optional<CredentialSupplier> credentialSupplier;
    private final Identity identity;

    public IdentityState(Identity identity, Optional<CredentialSupplier> credentialSupplier)
    {
        this.credentialSupplier = credentialSupplier;
        this.identity = identity;
    }

    public Optional<CredentialSupplier> getCredentialSupplier()
    {
        return credentialSupplier;
    }

    public Identity getIdentity()
    {
        return identity;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        IdentityState that = (IdentityState) o;
        return Objects.equals(credentialSupplier, that.credentialSupplier) && Objects.equals(identity, that.identity);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(credentialSupplier, identity);
    }

    public boolean isValid()
    {
        return identity.hasValidCredentials();
    }
}