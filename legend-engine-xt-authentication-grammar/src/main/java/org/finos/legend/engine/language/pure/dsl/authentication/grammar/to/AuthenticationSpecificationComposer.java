// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.to;

import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.GCPWIFWithAWSIdPAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecretVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentialsVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSDefaultCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSTSAssumeRoleCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSecretsManagerSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSStaticCredentials;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class AuthenticationSpecificationComposer implements AuthenticationSpecificationVisitor<String>
{
    private final int indentLevel;
    private final PureGrammarComposerContext context;

    public AuthenticationSpecificationComposer(int indentLevel, PureGrammarComposerContext context)
    {
        this.indentLevel = indentLevel;
        this.context = context;
    }

    @Override
    public String visit(ApiKeyAuthenticationSpecification authenticationSpecification)
    {
        return getTabString(indentLevel) + "authentication: ApiKey\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "location: '" + authenticationSpecification.location.name().toLowerCase() + "';\n" +
                getTabString(indentLevel + 1) + "keyName: '" + authenticationSpecification.keyName + "';\n" +
                getTabString(indentLevel + 1) + "value: " + renderCredentialVaultSecret(authenticationSpecification.value, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel) + "}\n";
    }

    @Override
    public String visit(UserPasswordAuthenticationSpecification authenticationSpecification)
    {
        CredentialVaultSecret credentialVaultSecret = authenticationSpecification.password;
        return getTabString(indentLevel) + "authentication: UserPassword\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "username: '" + authenticationSpecification.username + "';\n" +
                getTabString(indentLevel + 1) + "password: " + renderCredentialVaultSecret(credentialVaultSecret, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel) + "}\n";
    }

    @Override
    public String visit(EncryptedPrivateKeyPairAuthenticationSpecification authenticationSpecification)
    {
        return getTabString(indentLevel) + "authentication: EncryptedPrivateKey\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "userName: '" + authenticationSpecification.userName + "';\n" +
                getTabString(indentLevel + 1) + "privateKey: " + renderCredentialVaultSecret(authenticationSpecification.privateKey, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel + 1) + "passphrase: " + renderCredentialVaultSecret(authenticationSpecification.passphrase, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel) + "}\n";
    }

    @Override
    public String visit(GCPWIFWithAWSIdPAuthenticationSpecification authenticationSpecification)
    {
        return getTabString(indentLevel) + "authentication: GCPWIFWithAWSIdP\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "serviceAccountEmail: '" + authenticationSpecification.serviceAccountEmail + "';\n" +
                getTabString(indentLevel + 1) + "idP: " + this.renderGCPWIFWithAWSIdPIdP(authenticationSpecification.idPConfiguration, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel + 1) + "workload: " + this.renderGCPWIFWithAWSIdPWorkload(authenticationSpecification.workloadConfiguration, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel) + "}\n";
    }

    private String renderGCPWIFWithAWSIdPWorkload(GCPWIFWithAWSIdPAuthenticationSpecification.WorkloadConfiguration workloadConfiguration, int indentLevel, PureGrammarComposerContext context)
    {
        return "GCPWorkload\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "projectNumber: '" + workloadConfiguration.projectNumber + "';\n" +
                getTabString(indentLevel + 1) + "providerId: '" + workloadConfiguration.providerId + "';\n" +
                getTabString(indentLevel + 1) + "poolId: '" + workloadConfiguration.poolId + "';\n" +
                getTabString(indentLevel) + "}";
    }

    private String renderGCPWIFWithAWSIdPIdP(GCPWIFWithAWSIdPAuthenticationSpecification.IdPConfiguration idPConfiguration, int indentLevel, PureGrammarComposerContext context)
    {
        return "AWSIdP\n" +
                getTabString(indentLevel) + "{\n" +
                getTabString(indentLevel + 1) + "accountId: '" + idPConfiguration.accountId + "';\n" +
                getTabString(indentLevel + 1) + "region: '" + idPConfiguration.region + "';\n" +
                getTabString(indentLevel + 1) + "role: '" + idPConfiguration.role + "';\n" +
                getTabString(indentLevel + 1) + "awsCredentials: " + renderAWSCredentials(idPConfiguration.awsCredentials, indentLevel + 1, context) + "\n" +
                getTabString(indentLevel) + "}";
    }

    private static String renderCredentialVaultSecret(CredentialVaultSecret credentialVaultSecret, int indentLevel, PureGrammarComposerContext context)
    {
        CredentialVaultSecretComposer composer = new CredentialVaultSecretComposer(indentLevel, context);
        return credentialVaultSecret.accept(composer);
    }

    public static class CredentialVaultSecretComposer implements CredentialVaultSecretVisitor<String>
    {
        private final int indentLevel;
        private final PureGrammarComposerContext context;

        CredentialVaultSecretComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(PropertiesFileSecret propertiesFileSecret)
        {
            return "PropertiesFileSecret\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "propertyName: '" + propertiesFileSecret.propertyName + "';\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(EnvironmentCredentialVaultSecret environmentCredentialVaultSecret)
        {
            return "EnvironmentSecret\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "envVariableName: '" + environmentCredentialVaultSecret.envVariableName + "';\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(SystemPropertiesSecret systemPropertiesCredentialVaultSecret)
        {
            return "SystemPropertiesSecret\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "systemPropertyName: '" + systemPropertiesCredentialVaultSecret.systemPropertyName + "';\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(AWSSecretsManagerSecret awsSecretsManagerSecret)
        {
            return "AWSSecretsManagerSecret\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "secretId: '" + awsSecretsManagerSecret.secretId + "';\n" +
                    getTabString(indentLevel + 1) + "versionId: '" + awsSecretsManagerSecret.versionId + "';\n" +
                    getTabString(indentLevel + 1) + "versionStage: '" + awsSecretsManagerSecret.versionStage + "';\n" +
                    getTabString(indentLevel + 1) + "awsCredentials: " + renderAWSCredentials(awsSecretsManagerSecret.awsCredentials, indentLevel + 1, context) + "\n" +
                    getTabString(indentLevel) + "}";
        }
    }

    public static class AWSCredentialsComposer implements AWSCredentialsVisitor<String>
    {
        private int indentLevel;
        private PureGrammarComposerContext context;

        public AWSCredentialsComposer(int indentLevel, PureGrammarComposerContext context)
        {
            this.indentLevel = indentLevel;
            this.context = context;
        }

        @Override
        public String visit(AWSStaticCredentials awsStaticCredentials)
        {
            return "Static\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "accessKeyId: " + renderCredentialVaultSecret(awsStaticCredentials.accessKeyId, indentLevel + 1, context) + "\n" +
                    getTabString(indentLevel + 1) + "secretAccessKey: " + renderCredentialVaultSecret(awsStaticCredentials.secretAccessKey, indentLevel + 1, context) + "\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(AWSDefaultCredentials awsDefaultCredentials)
        {
            return "Default\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel) + "}";
        }

        @Override
        public String visit(AWSSTSAssumeRoleCredentials awsSTSAssumeRoleCredentials)
        {
            String roleSessionNameLine = "";
            if (awsSTSAssumeRoleCredentials.roleSessionName != null)
            {
                roleSessionNameLine = getTabString(indentLevel + 1) + "roleSessionName: '" + awsSTSAssumeRoleCredentials.roleSessionName + "';\n";
            }

            return "STSAssumeRole\n" +
                    getTabString(indentLevel) + "{\n" +
                    getTabString(indentLevel + 1) + "roleArn: '" + awsSTSAssumeRoleCredentials.roleArn + "';\n" +
                    roleSessionNameLine +
                    getTabString(indentLevel + 1) + "awsCredentials: " + renderAWSCredentials(awsSTSAssumeRoleCredentials.awsCredentials, indentLevel + 1, context) + "\n" +
                    getTabString(indentLevel) + "}";
        }

    }

    private static String renderAWSCredentials(AWSCredentials awsCredentials, int indentLevel, PureGrammarComposerContext context)
    {
        AWSCredentialsComposer awsCredentialsComposer = new AWSCredentialsComposer(indentLevel, context);
        return awsCredentials.accept(awsCredentialsComposer);
    }
}
