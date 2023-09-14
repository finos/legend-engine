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

package org.finos.legend.engine.language.hostedService.generation.control;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.kerberos.ProfileManagerHelper;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_Ownership;
import org.finos.legend.pure.generated.Root_meta_external_function_activator_hostedService_UserList;
import org.pac4j.core.profile.CommonProfile;

public class UserListOwnerValidator implements HostedServiceOwnerValidator<Root_meta_external_function_activator_hostedService_UserList>
{
    @Override
    public boolean isOwner(MutableList<CommonProfile> profiles,  Root_meta_external_function_activator_hostedService_UserList users)
    {
        return users._users().contains(SubjectTools.getKerberos(ProfileManagerHelper.extractSubject(profiles))); //use profile
    }

    @Override
    public boolean supports(Root_meta_external_function_activator_hostedService_Ownership ownershipModel)
    {
        return ownershipModel instanceof Root_meta_external_function_activator_hostedService_UserList;
    }
}
