// Copyright 2026 Goldman Sachs
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

import org.finos.legend.engine.shared.core.identity.Credential;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import javax.security.auth.Subject;
import java.util.Iterator;
import java.util.Set;


/*
    Credential that wraps a javax.security.subject that  does  not have a TGT
    Intended to be used with constrained Delegation

 */
public class LegendConstrainedKerberosCredential implements Credential
{
    private Subject subject;
    private Subject frontendSubject;
    //TODO: Need a reference to the delegating subject to get the TGT


    public LegendConstrainedKerberosCredential(Subject subject, Subject frontendSubject)
    {
        this.subject = subject;
        this.frontendSubject = frontendSubject;
    }


    public Subject getSubject()
    {
        return subject;
    }

    public void setSubject(Subject subject)
    {
        this.subject = subject;
    }

    public Subject getFrontendSubject()
    {
        return frontendSubject;
    }

    public void setFrontendSubject(Subject frontendSubject)
    {
        this.frontendSubject = frontendSubject;
    }

    public Subject getMergedSubject()
    {
        Subject mergedSubject = new Subject();
        mergedSubject.getPrincipals().addAll(frontendSubject.getPrincipals());
        mergedSubject.getPublicCredentials().addAll(subject.getPublicCredentials());
        mergedSubject.getPrivateCredentials().addAll(frontendSubject.getPrivateCredentials());
        return mergedSubject;
    }

    @Override
    public boolean isValid()
    {

        Set<GSSCredential> credentials = subject.getPublicCredentials(GSSCredential.class);
        Iterator<GSSCredential> iterator = credentials.iterator();
        try
        {
            return iterator.hasNext() && iterator.next().getRemainingLifetime() > 0;
        }
        catch (GSSException gssException)
        {
            throw new RuntimeException(gssException);
        }
  }
}


