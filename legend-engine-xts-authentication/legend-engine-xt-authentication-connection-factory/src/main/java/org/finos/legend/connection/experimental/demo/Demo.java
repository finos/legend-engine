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

package org.finos.legend.connection.experimental.demo;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.experimental.ConnectionAuthentication;
import org.finos.legend.connection.experimental.ConnectionFactory;
import org.finos.legend.connection.experimental.ConnectionSpecification;
import org.finos.legend.connection.experimental.EnvironmentConfiguration;
import org.finos.legend.connection.experimental.IdentityFactory;
import org.finos.legend.connection.experimental.IdentitySpecification;
import org.finos.legend.connection.experimental.StoreInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import java.sql.Connection;
import java.util.Properties;

public class Demo
{
    public static void main(String[] args) throws Exception
    {
        Properties properties = new Properties();
        String passRef = "passwordRef1";
        properties.put(passRef, "password");

        RelationalDatabaseStoreSupport storeSupport = new RelationalDatabaseStoreSupport.Builder()
                .withIdentifier("Postgres")
                .withDatabaseType("Postgres")
                .withAuthenticationSpecificationTypes(Lists.mutable.of(
                        UserPasswordAuthenticationSpecification.class,
                        ApiKeyAuthenticationSpecification.class
                ))
                .build();

        EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration.Builder()
                .withVault(new PropertiesFileCredentialVault(properties))
                .withStoreSupport(storeSupport)
                .build();

        IdentityFactory identityFactory = new IdentityFactory.Builder(environmentConfiguration)
                .build();

        ConnectionFactory connectionFactory = new ConnectionFactory.Builder(environmentConfiguration)
                .withCredentialBuilderFlows(Lists.mutable.of(
                        new UserPasswordCredentialBuilder()
                ))
                .build();

        // --------------------------------- USAGE ---------------------------------

        Identity identity = identityFactory.createIdentity(
                new IdentitySpecification.Builder()
                        .withName("test-user")
                        // .withProfiles(profiles)
                        // .withSubject(subject)
                        // .with(new KeytabCredential("ref1"))
                        .withCredential(new PlaintextUserPasswordCredential("username", "password1"))
                        .build()
        );

        ConnectionSpecification connectionSpecification = new DemoConnectionSpecification();
        StoreInstance demoStore = new StoreInstance.Builder(environmentConfiguration)
                .withIdentifier("my-demo-store")
                .withStoreSupportIdentifier("Postgres")
                .withAuthenticationSpecificationType(UserPasswordAuthenticationSpecification.class)
                .withConnectionSpecification(connectionSpecification)
                .build();
        connectionFactory.registerStoreInstance(demoStore);

        AuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification("username", new PropertiesFileSecret("passwordRef1"));
        ConnectionAuthentication connectionAuthentication = connectionFactory.authenticate(identity, "my-demo-store", authenticationSpecification);
        Connection conn = connectionFactory.getConnection(connectionAuthentication, "my-demo-store");
    }
}
