// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.mongodb.auth;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.connection.ConnectionProvider;
import org.finos.legend.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.KerberosAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.KerberosUtils;
import org.finos.legend.engine.shared.core.identity.credential.LegendKerberosCredential;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;

import javax.security.auth.kerberos.KerberosPrincipal;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MongoDBStoreConnectionProvider extends ConnectionProvider<Supplier<MongoClient>>
{

    private static final String ADMIN_DB = "admin";

    public MongoDBStoreConnectionProvider(CredentialProviderProvider credentialProviderProvider)
    {
        super(credentialProviderProvider);
    }

    @Override
    public Supplier<MongoClient> makeConnection(ConnectionSpecification connectionSpec, AuthenticationSpecification authenticationSpec, Identity identity) throws Exception
    {
        if (!(connectionSpec instanceof MongoDBConnectionSpecification && (authenticationSpec instanceof UserPasswordAuthenticationSpecification || authenticationSpec instanceof KerberosAuthenticationSpecification)))
        {
            throw new IllegalStateException("Invalid ConnectionSpecification/AuthenticationSpecification. Please reach out to dev team");
        }

        MongoDBConnectionSpecification mongoDBConnectionSpec = (MongoDBConnectionSpecification) connectionSpec;

        List<ServerAddress> serverAddresses = mongoDBConnectionSpec.getServerAddresses();
        MongoClientSettings.Builder clientSettingsBuilder = MongoClientSettings.builder().applyToClusterSettings(builder -> builder.hosts(serverAddresses)).applicationName("Legend Execution Server");

        Supplier<MongoClient> mongoClientSupplier;
        if (authenticationSpec instanceof KerberosAuthenticationSpecification)
        {
            Optional<LegendKerberosCredential> kerberosHolder = identity.getCredential(LegendKerberosCredential.class);
            if (!kerberosHolder.isPresent())
            {
                throw new UnsupportedOperationException("Expected Kerberos credential was not found, for KerberosAuthenticationSpecification");
            }
            LegendKerberosCredential kerberosCredential = kerberosHolder.get();

            if (kerberosCredential.getSubject().getPrincipals().stream().noneMatch(KerberosPrincipal.class::isInstance))
            {
                String errMesg = String.format("Invalid Subject: Expected at least 1 KerberosPrincipal but got [%s]",
                        kerberosCredential.getSubject().getPrincipals().stream().map(k -> k.getClass().getName()).collect(Collectors.joining(", ")));
                throw new IllegalStateException(errMesg);
            }

            KerberosPrincipal kerberosPrincipal = kerberosCredential.getSubject().getPrincipals(KerberosPrincipal.class).stream().findFirst().get();

            MongoCredential mongoCredential = MongoCredential.createGSSAPICredential(kerberosPrincipal.getName());
            MongoClientSettings clientSettings = clientSettingsBuilder.credential(mongoCredential).build();
            mongoClientSupplier = () -> KerberosUtils.doAs(identity, (PrivilegedAction<MongoClient>) () -> MongoClients.create(clientSettings));
        }
        else
        {
            // authenticationSpec instanceof UserPasswordAuthenticationSpecification
            Credential credential = makeCredential(authenticationSpec, identity);

            if (credential instanceof PlaintextUserPasswordCredential)
            {
                PlaintextUserPasswordCredential plaintextCredential = (PlaintextUserPasswordCredential) credential;
                MongoCredential mongoCredential = MongoCredential.createCredential(plaintextCredential.getUser(), ADMIN_DB, plaintextCredential.getPassword().toCharArray());
                MongoClientSettings clientSettings = clientSettingsBuilder.credential(mongoCredential).build();
                mongoClientSupplier = () -> MongoClients.create(clientSettings);
            }
            else
            {
                String errMesg = String.format("Within UserPasswordAuthenticationSpecification only PlaintextUserPasswordCredential is supported, but got: %s", credential.getClass().getName());
                throw new IllegalStateException(errMesg);
            }

        }
        return mongoClientSupplier;
    }
}
