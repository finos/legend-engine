// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.identity.extension.kerberos;

import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendConstrainedKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

public class KerberosSubjectIdentityFactory implements IdentityFactory
{

    public static final String USER_SUBJECT = "USER_SUBJECT";
    public static final String SERVICE_SUBJECT = "SERVICE_SUBJECT";

    @Override
    public Optional<Identity> makeIdentity(Object authenticationSource)
    {
        if (!(authenticationSource instanceof Map))
        {
            return Optional.empty();
        }
        Map<String,Subject> mapOfSubjects = (Map<String,Subject>) authenticationSource;
        Subject subject = mapOfSubjects.get(USER_SUBJECT);
        Subject serviceSubject = mapOfSubjects.get(SERVICE_SUBJECT);
        Principal principal = SubjectTools.getPrincipalFromSubject(subject);
        if (principal == null)
        {
            return Optional.of(Identity.makeUnknownIdentity());
        }
        String principalName = principal.getName();
        int atIndex = principalName.indexOf('@');
        String name = atIndex >= 0 ? principalName.substring(0, atIndex) : principalName;
        if (subject.getPrivateCredentials().isEmpty())
        {
            return Optional.of(new Identity(name, new LegendConstrainedKerberosCredential(subject,serviceSubject)));
        }

        return Optional.of(new Identity(name, new LegendKerberosCredential(subject)));
    }
}
