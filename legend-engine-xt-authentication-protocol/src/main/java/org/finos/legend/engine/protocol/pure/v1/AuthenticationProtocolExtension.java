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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.demo.AuthenticationDemo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.GCPWIFWithAWSIdPAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSDefaultCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSTSAssumeRoleCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSecretsManagerSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSStaticCredentials;

import java.util.List;
import java.util.Map;

public class AuthenticationProtocolExtension implements PureProtocolExtension
{
    public static final String AUTHENTICATION_DEMO_CLASSIFIER_PATH = "meta::pure::runtime::connection::authentication::demo::AuthenticationDemo";

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.of(() -> Lists.fixedSize.of(
                // Packageable element
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(AuthenticationDemo.class, "authenticationDemo")
                        .build(),
                // Authentication
                ProtocolSubTypeInfo.newBuilder(AuthenticationSpecification.class)
                        .withSubtype(ApiKeyAuthenticationSpecification.class, "apiKey")
                        .withSubtype(UserPasswordAuthenticationSpecification.class, "userPassword")
                        .withSubtype(EncryptedPrivateKeyPairAuthenticationSpecification.class, "encryptedPrivateKey")
                        .withSubtype(GCPWIFWithAWSIdPAuthenticationSpecification.class, "gcpWithAWSIdP")
                        .build(),
                // vault secret
                ProtocolSubTypeInfo.newBuilder(CredentialVaultSecret.class)
                        .withSubtype(PropertiesFileSecret.class, "properties")
                        .withSubtype(EnvironmentCredentialVaultSecret.class, "environment")
                        .withSubtype(SystemPropertiesSecret.class, "systemproperties")
                        .withSubtype(AWSSecretsManagerSecret.class, "awssecretsmanager")
                        .build(),
                // aws credentials
                ProtocolSubTypeInfo.newBuilder(AWSCredentials.class)
                        .withSubtype(AWSStaticCredentials.class, "awsStatic")
                        .withSubtype(AWSDefaultCredentials.class, "awsDefault")
                        .withSubtype(AWSSTSAssumeRoleCredentials.class, "awsSTSAssumeRole")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(
                AuthenticationDemo.class, AUTHENTICATION_DEMO_CLASSIFIER_PATH
        );
    }
}
