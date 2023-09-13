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

package org.finos.legend.connection;

import org.junit.Test;

import java.util.Properties;

public class ConnectionFactoryTest
{
    @Test
    public void testConnection() throws Exception
    {
        Properties properties = new Properties();
        String PASS_REF = "passwordRef1";
        String TEST_STORE_SUPPORT = "test-store";
        properties.put(PASS_REF, "password");

//        RelationalDatabaseStoreSupport storeSupport = new RelationalDatabaseStoreSupport.Builder()
//                .withIdentifier("Postgres")
//                .withDatabaseType("Postgres")
//                .withAuthenticationSpecificationTypes(Lists.mutable.of(
//                        UserPasswordAuthenticationSpecification.class,
//                        ApiKeyAuthenticationSpecification.class
//                ))
//                .build();
//
//        EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration.Builder()
//                .withVault(new PropertiesFileCredentialVault(properties))
//                .withStoreSupport(storeSupport)
//                .build();
//
//        IdentityFactory identityFactory = new IdentityFactory.Builder(environmentConfiguration)
//                .build();
//
//        ConnectionFactory connectionFactory = new ConnectionFactory.Builder(environmentConfiguration)
//                .withCredentialBuilderFlows(Lists.mutable.of(
//                        new UserPasswordCredentialBuilder()
//                ))
//                .build();
//
//        // --------------------------------- USAGE ---------------------------------
//
//        Identity identity = identityFactory.createIdentity(
//                new IdentitySpecification.Builder()
//                        .withName("test-user")
//                        .build()
//        );

//        ConnectionSpecification connectionSpecification = new DemoConnectionSpecification();
//        StoreInstance demoStore = new StoreInstance.Builder(environmentConfiguration)
//                .withIdentifier("my-demo-store")
//                .withStoreSupportIdentifier("Postgres")
//                .withAuthenticationSpecificationType(UserPasswordAuthenticationSpecification.class)
//                .withConnectionSpecification(connectionSpecification)
//                .build();
//        connectionFactory.registerStoreInstance(demoStore);
//
//        AuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification("username", new PropertiesFileSecret(PASS_REF));
//        ConnectionAuthentication connectionAuthentication = connectionFactory.authenticate(identity, "my-demo-store", authenticationSpecification);
//        Connection conn = connectionFactory.getConnection(connectionAuthentication, "my-demo-store");
    }
}
