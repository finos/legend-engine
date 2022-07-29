// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.shared.core.kerberos;

import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Principal;
import java.util.Iterator;

public class SubjectTools
{
    public static String getPrincipal(Subject subject)
    {
        return subject == null ? "_UNKNOWN_" : subject.getPrincipals().iterator().next().getName();
    }

    public static String getKerberos(Subject subject)
    {
        if (subject == null)
        {
            return "_UNKNOWN_";
        }
        String principal = subject.getPrincipals().iterator().next().getName();
        return getUsername(principal);
    }

    public static String getUsername(String principal)
    {
        return principal.substring(0, principal.indexOf("@"));
    }

    public static Subject getLocalSubject()
    {
        try
        {
            Configuration config = new LocalLoginConfiguration();
            LoginContext ctx = new LoginContext("", null, null, config);
            ctx.login();
            return ctx.getSubject();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Subject getSubjectFromKeytab(String keytabLocation, String principal, boolean initiator)
    {
        try
        {
            Configuration config = new SystemAccountLoginConfiguration(keytabLocation, principal, initiator);
            LoginContext loginContext = new LoginContext("", null, null, config);
            loginContext.login();
            return loginContext.getSubject();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Subject getCurrentSubject()
    {
        AccessControlContext controlContext = AccessController.getContext();
        if (controlContext.getDomainCombiner() instanceof SubjectDomainCombiner)
        {
            return ((SubjectDomainCombiner) controlContext.getDomainCombiner()).getSubject();
        }
        return null;
    }

    public static Principal getPrincipalFromSubject(Subject subject)
    {
        if (subject != null)
        {
            Iterator<KerberosPrincipal> iterator = subject.getPrincipals(KerberosPrincipal.class).iterator();
            if (iterator.hasNext())
            {
                return iterator.next();
            }
        }
        return null;
    }

    public static Principal getCurrentPrincipal()
    {
        return getPrincipalFromSubject(getCurrentSubject());
    }

    private static String getUsernameFromPrincipal(Principal principal)
    {
        return principal != null ? principal.getName().split("@")[0] : null;
    }

    public static String getCurrentUsername()
    {
        return getUsernameFromPrincipal(getCurrentPrincipal());
    }

}
