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

package org.finos.legend.engine.connection.jdbc;

import org.finos.legend.authentication.credentialprovider.CredentialBuilder;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import scala.collection.immutable.Map;

import java.sql.Connection;
import java.sql.Driver;

public abstract class LegendJdbcConnectionProvider extends LegendConnectionProvider
{
    public LegendJdbcConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    public Credential makeCredential(AuthenticationSpecification authenticationSpecification, Identity identity) throws Exception
    {
        return CredentialBuilder.makeCredential(this.credentialProviderProvider, authenticationSpecification, identity);
    }

    public abstract boolean canHandle(Driver driver, Map<String, String> options);

    public abstract Connection getConnection(Driver driver, Map<String, String> options) throws Exception;
}
