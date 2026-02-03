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

package org.finos.legend.engine.shared.core.identity;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Identity
{
    private String name;
    private final List<Credential> credentials = new ArrayList<>();
    private static final Identity ANONYMOUS_IDENTITY = new Identity("Anonymous");
    private static final MutableList<IdentityFactory> FACTORIES = Iterate.addAllTo(ServiceLoader.load(IdentityFactory.class), Lists.mutable.empty());

    public Identity(String name, Credential... credentials)
    {
        this.name = name;
        this.credentials.addAll(Lists.mutable.of(credentials));
    }

    public Identity(String name, List<Credential> credentials)
    {
        this.name = name;
        this.credentials.addAll(credentials);
    }

    public Identity(String name)
    {
        this.name = name;
        this.credentials.add(new AnonymousCredential());
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public static Identity getAnonymousIdentity()
    {
        return ANONYMOUS_IDENTITY;
    }

    public <T extends Credential> Optional<T> getCredential(Class<T> credentialType)
    {
        // TODO : Can there be more than one cred of the same type ??
        Optional<Credential> holder = this.credentials.stream().filter(credentialType::isInstance).findFirst();
        if (!holder.isPresent())
        {
            return Optional.empty();
        }
        Credential raw = holder.get();
        return Optional.of(credentialType.cast(raw));
    }

    public int countCredentials()
    {
        return this.credentials.size();
    }

    public ImmutableList<Credential> getCredentials()
    {
        return Lists.immutable.withAll(this.credentials);
    }

    public boolean hasValidCredentials()
    {
        return credentials.isEmpty() || credentials.stream().allMatch(Credential::isValid);
    }

    public static Identity makeUnknownIdentity()
    {
        return new Identity("_UNKNOWN_");
    }

    public static Identity makeIdentity(Object authenticationSource)
    {
        List<Identity> identities = FACTORIES.collect(f -> f.makeIdentity(authenticationSource))
                .stream().filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        if (identities.isEmpty())
        {
            return makeUnknownIdentity();
        }

        String name = identities.get(0).getName();
        List<Credential> allCredentials = identities.stream()
                .flatMap(identity -> identity.getCredentials().stream())
                .collect(Collectors.toList());

        return new Identity(name, allCredentials);

    }
}
