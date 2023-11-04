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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.util.List;
import java.util.Objects;

public class IdentitySpecification
{
    private final String name;
    private final List<CommonProfile> profiles;
    private final Subject subject;
    private final List<Credential> credentials;

    private IdentitySpecification(String name, List<CommonProfile> profiles, Subject subject, List<Credential> credentials)
    {
        this.name = name;
        this.profiles = profiles;
        this.subject = subject;
        this.credentials = credentials;
    }

    public String getName()
    {
        return name;
    }

    public List<CommonProfile> getProfiles()
    {
        return profiles;
    }

    public Subject getSubject()
    {
        return subject;
    }

    public List<Credential> getCredentials()
    {
        return credentials;
    }

    public static class Builder
    {
        private String name;
        private final List<CommonProfile> profiles = Lists.mutable.empty();
        private Subject subject;
        private final List<Credential> credentials = Lists.mutable.empty();

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder withProfiles(List<CommonProfile> profiles)
        {
            this.profiles.addAll(profiles);
            return this;
        }

        public Builder withProfile(CommonProfile profile)
        {
            this.profiles.add(profile);
            return this;
        }

        public Builder withSubject(Subject subject)
        {
            this.subject = subject;
            return this;
        }

        public Builder withCredentials(List<Credential> credentials)
        {
            this.credentials.addAll(credentials);
            return this;
        }

        public Builder withCredential(Credential credential)
        {
            this.credentials.add(credential);
            return this;
        }

        public IdentitySpecification build()
        {
            return new IdentitySpecification(
                    this.name,
                    this.profiles,
                    this.subject,
                    this.credentials
            );
        }
    }
}
