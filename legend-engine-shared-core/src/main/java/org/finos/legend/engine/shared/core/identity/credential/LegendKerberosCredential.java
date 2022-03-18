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

package org.finos.legend.engine.shared.core.identity.credential;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosTicket;
import org.finos.legend.engine.shared.core.identity.Credential;
import java.util.Iterator;

/*
    Credential that wraps a javax.security.subject.
    Class name is prefixed with Legend to differentiate from other KerberosCredential classes
 */
public class LegendKerberosCredential implements Credential
{
    private Subject subject;

    public LegendKerberosCredential(Subject subject)
    {
        this.subject = subject;
    }

    public Subject getSubject()
    {
        return subject;
    }

    public void setSubject(Subject subject)
    {
        this.subject = subject;
    }

    @Override
    public boolean isValid()
    {
        Set<KerberosTicket> credentials = subject.getPrivateCredentials(KerberosTicket.class);
        if ( credentials != null)
        {
            Iterator<KerberosTicket> iterator = credentials.iterator();
            return iterator != null && iterator.hasNext() && iterator.next().isCurrent();
        }
        return false;
    }
}
