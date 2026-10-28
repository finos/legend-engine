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

package org.finos.legend.engine.identity.extensions.pac4j;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.lang.reflect.Method;

public class TestPac4jAuthenticationSourceProvider
{
    private final Pac4jAuthenticationSourceProvider provider = new Pac4jAuthenticationSourceProvider();
    private final Identity identity = new Identity("testUser", new LegendKerberosCredential(new Subject()));

    @Test
    public void valueForIsAnInstanceOfTheDeclaredParameterType()
    {
        Assertions.assertTrue(provider.parameterType().isInstance(provider.valueFor(identity)));
    }

    @Test
    public void aMethodDeclaringTheParameterTypeIsFoundAndInvokedWithTheValue() throws Exception
    {
        Method method = TestPac4jAuthenticationSourceProvider.class.getMethod("generatedExecute", provider.parameterType());

        MutableList<?> profiles = (MutableList<?>) method.invoke(null, provider.valueFor(identity));

        Assertions.assertEquals(1, profiles.size());
        Assertions.assertInstanceOf(KerberosProfile.class, profiles.get(0));
    }

    @Test
    public void clientNameComesFromTheFirstProfile()
    {
        Assertions.assertEquals(provider.valueFor(identity).get(0).getClientName(), provider.clientName(identity));
    }

    public static MutableList<CommonProfile> generatedExecute(MutableList<CommonProfile> profiles)
    {
        return profiles;
    }
}
