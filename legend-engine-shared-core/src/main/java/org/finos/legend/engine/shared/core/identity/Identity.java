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

package org.finos.legend.engine.shared.core.identity;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Identity
{
    private String name;
    private final List<Credential> credentials = new ArrayList<>();

    public Identity(String name, Credential credential)
    {
        this.name = name;
        this.credentials.add(credential);
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

    public <T extends Credential> Optional<T> getCredential(Class<T> credentialType)
    {
        // TODO : Can there be more than one cred of the same type ??
        Optional<Credential> holder = this.credentials.stream().filter(c -> credentialType.isInstance(c)).findFirst();
        if (!holder.isPresent())
        {
            return Optional.empty();
        }
        Credential raw = holder.get();
        return Optional.of(credentialType.cast(raw));
    }

    public Credential getFirstCredential()
    {
        if (this.credentials.isEmpty())
        {
            String message = String.format("Invalid method call. Calling code assumes single credential but none was found");
            throw new RuntimeException(message);
        }
        if (this.credentials.size() > 1)
        {
            String credentialNames = this.credentials.stream().map(c -> c.getClass().getCanonicalName()).collect(Collectors.joining(","));
            String message = String.format("Invalid method call. Cannot return 'first' credential when the identity has more than one credential. Credentials=%s", credentialNames);
            throw new RuntimeException(message);
        }
        return this.credentials.get(0);
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
        return credentials.isEmpty()|| credentials.stream().allMatch( c -> c.isValid());
    }

}
