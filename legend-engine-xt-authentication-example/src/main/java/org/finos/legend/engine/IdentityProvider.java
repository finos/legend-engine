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

package org.finos.legend.engine;

import org.finos.legend.engine.shared.core.identity.Identity;

import java.util.Optional;

public interface IdentityProvider
{
    Optional<Identity> getCurrentIdentity();

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private Identity identity = null;

        public Builder with(Identity identity)
        {
            this.identity = identity;
            return this;
        }

        public IdentityProvider build()
        {
            return new IdentityProvider()
            {
                @Override
                public Optional<Identity> getCurrentIdentity()
                {
                    return Optional.ofNullable(identity);
                }
            };
        }
    }
}
