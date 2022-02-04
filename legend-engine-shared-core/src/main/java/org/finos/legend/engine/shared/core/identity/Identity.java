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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public int countCredentials()
    {
        return this.credentials.size();
    }

    public boolean hasValidCredentials()
    {
        return credentials.isEmpty()|| credentials.stream().allMatch( c -> c.isValid());
    }
}
