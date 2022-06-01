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

package org.finos.legend.engine.shared.core.identity.factory;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.List;

public interface IdentityFactory
{
    Identity makeIdentity(Subject subject);

    Identity makeIdentity(MutableList<CommonProfile> profiles);

    Identity makeIdentityForTesting(String name);

    /*
        A helper function to translate from Identity to Profile.
        Legend code base makes use of both Identity and Profile.
        Some "upper" layers (e.g ServiceRunner APIs)  have been switched to Identity. Some "lower" layers (e.g connection code) have been switched to Identity.
        However, the "middle" layers still use Profile. So we switch between Identity and Profile.
        Profile should eventually be removed from the "middle" layers.
    */
    default List<CommonProfile> adapt(Identity identity)
    {
        return Collections.EMPTY_LIST;
    }
}