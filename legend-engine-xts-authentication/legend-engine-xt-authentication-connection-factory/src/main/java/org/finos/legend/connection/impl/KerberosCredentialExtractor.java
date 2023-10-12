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

package org.finos.legend.connection.impl;

import org.finos.legend.connection.CredentialBuilder;
import org.finos.legend.connection.LegendEnvironment;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;

import java.util.Optional;

public class KerberosCredentialExtractor extends CredentialBuilder<KerberosAuthenticationConfiguration, LegendKerberosCredential, LegendKerberosCredential>
{
    @Override
    public LegendKerberosCredential makeCredential(Identity identity, KerberosAuthenticationConfiguration authenticationConfiguration, LegendKerberosCredential credential, LegendEnvironment environment) throws Exception
    {
        Optional<LegendKerberosCredential> credentialOptional = identity.getCredential(LegendKerberosCredential.class);
        if (!credentialOptional.isPresent())
        {
            throw new RuntimeException(String.format("Can't extract credential of type '%s' from the specified identity", LegendKerberosCredential.class.getSimpleName()));
        }
        return credentialOptional.get();
    }
}
