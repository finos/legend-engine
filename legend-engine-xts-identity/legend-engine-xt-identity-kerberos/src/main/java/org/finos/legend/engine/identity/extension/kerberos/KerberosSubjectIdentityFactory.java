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
import org.finos.legend.engine.shared.core.identity.credential.KerberosUtils;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;

import javax.security.auth.Subject;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Optional;

public class KerberosSubjectIdentityFactory implements IdentityFactory
{
    @Override
    public Optional<Identity> makeIdentity(Object authenticationSource)
    {
        if (authenticationSource == null || !(authenticationSource instanceof  Subject))
        {
            return Optional.empty();
        }
        Subject subject = (Subject) authenticationSource;
        Principal principal = SubjectTools.getPrincipalFromSubject(subject);
        if (principal == null)
        {
            throw new IllegalArgumentException("Subject does not contain a KerberosPrincipal");
        }
        String name = principal != null ? principal.getName().split("@")[0] : null;
        return Optional.of(new Identity(name, new LegendKerberosCredential(subject)));
    }
}
