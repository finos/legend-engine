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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.factory.Maps;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OAuthProfile
{
    public String key;
    public String discoveryUrl;
    public String clientId;
    public String secret;
    public Map<String, String> customParams = Maps.mutable.empty();

    public static OAuthProfile makeUnknownProfile()
    {
        OAuthProfile oAuthProfile = new OAuthProfile();
        oAuthProfile.clientId = "UNKNOWN";
        oAuthProfile.key = "UNKNOWN";
        oAuthProfile.discoveryUrl = "UNKNOWN";
        oAuthProfile.secret = "UNKNOWN";
        return oAuthProfile;
    }

    public static OAuthProfile findOAuthProfile(List<OAuthProfile> profiles, String key)
    {
        /*
            "Profile" is not an OAuth concept.
            Legend is using "profile" as a way by which the Legend Engine server can look up OAuth client ids that are tied to specific database connections.
            Also, as we add expand support for OAuth, OAuth client ids are not database specific.
            Also, a database can be accessed in multiple ways. Some users might access the database using a database specific client id and some with a platform specific client id.

            To support these use cases, we do not throw if we cannot find a matching profile.
            Other code in the connection code path will handle this "null" profile.

            Eventually the use of the profile will be deprecated.
         */
        Optional<OAuthProfile> holder = profiles.stream().filter(profile -> profile.key.equals(key)).findFirst();
        if (holder.isPresent())
        {
            return holder.get();
        }
        return OAuthProfile.makeUnknownProfile();
    }
}
