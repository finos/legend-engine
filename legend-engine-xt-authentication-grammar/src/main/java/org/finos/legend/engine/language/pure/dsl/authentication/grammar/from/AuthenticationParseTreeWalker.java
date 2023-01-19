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

package org.finos.legend.engine.language.pure.dsl.authentication.grammar.from;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.EncryptedPrivateKeyPairAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.GCPWIFWithAWSIdPAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class AuthenticationParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext context;

    public AuthenticationParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.context = null;
    }

    public AuthenticationParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext context)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.context = context;
    }

    public AuthenticationSpecification visitAuthenticationSpecification(AuthenticationParserGrammar.AuthenticationContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        AuthenticationParserGrammar.UserPasswordAuthenticationContext userPasswordAuthenticationContext = ctx.userPasswordAuthentication();
        if (userPasswordAuthenticationContext != null)
        {
            return this.visitUserPasswordAuthentication(userPasswordAuthenticationContext);
        }
        AuthenticationParserGrammar.ApiKeyAuthenticationContext apiKeyAuthenticationContext = ctx.apiKeyAuthentication();
        if (apiKeyAuthenticationContext != null)
        {
            return this.visitApiKeyAuthentication(apiKeyAuthenticationContext);
        }
        AuthenticationParserGrammar.EncryptedPrivateKeyAuthenticationContext encryptedPrivateKeyAuthenticationContext = ctx.encryptedPrivateKeyAuthentication();
        if (encryptedPrivateKeyAuthenticationContext != null)
        {
            return this.visitEncryptedKeyPairAuthentication(encryptedPrivateKeyAuthenticationContext);
        }

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthenticationContext gcpWIFWithAWSIdPAuthenticationContext = ctx.gcpWIFWithAWSIdPAuthentication();
        if (gcpWIFWithAWSIdPAuthenticationContext != null)
        {
            return this.visitGcpWIFWithAWSIdPAuthenticationContext(gcpWIFWithAWSIdPAuthenticationContext);
        }

        throw new EngineException("Unsupported authentication", sourceInformation, EngineErrorType.PARSER);
    }

    public AuthenticationSpecification visitGcpWIFWithAWSIdPAuthenticationContext(AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthenticationContext ctx)
    {
        GCPWIFWithAWSIdPAuthenticationSpecification authenticationSpecification = new GCPWIFWithAWSIdPAuthenticationSpecification();
        authenticationSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_serviceAccountEmailContext serviceAccountEmailContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_serviceAccountEmail(), "serviceAccountEmail", authenticationSpecification.sourceInformation);
        String serviceAccountEmail = PureGrammarParserUtility.fromGrammarString(serviceAccountEmailContext.STRING().getText(), true);
        authenticationSpecification.serviceAccountEmail = serviceAccountEmail;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdpContext idpContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_awsIdp(), "idP", authenticationSpecification.sourceInformation);
        authenticationSpecification.idPConfiguration = this.visitGCPWithAWSIdPIdp(idpContext);

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_gcpWorkloadContext workloadContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_gcpWorkload(), "workload", authenticationSpecification.sourceInformation);
        authenticationSpecification.workloadConfiguration = this.visitGCPWithAWSIdPWorkload(workloadContext);

        return authenticationSpecification;
    }

    public GCPWIFWithAWSIdPAuthenticationSpecification.IdPConfiguration visitGCPWithAWSIdPIdp(AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdpContext ctx)
    {
        GCPWIFWithAWSIdPAuthenticationSpecification.IdPConfiguration configuration = new GCPWIFWithAWSIdPAuthenticationSpecification.IdPConfiguration();

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdp_accountIdContext accountIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_awsIdp_accountId(), "accountId", walkerSourceInformation.getSourceInformation(ctx));
        String accountId = PureGrammarParserUtility.fromGrammarString(accountIdContext.STRING().getText(), true);
        configuration.accountId = accountId;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdp_regionContext regionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_awsIdp_region(), "region", walkerSourceInformation.getSourceInformation(ctx));
        String region = PureGrammarParserUtility.fromGrammarString(regionContext.STRING().getText(), true);
        configuration.region = region;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdp_roleContext roleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_awsIdp_role(), "role", walkerSourceInformation.getSourceInformation(ctx));
        String role = PureGrammarParserUtility.fromGrammarString(roleContext.STRING().getText(), true);
        configuration.role = role;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdp_awsCredentialsContext awsCredentialsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_awsIdp_awsCredentials(), "awsCredentials", walkerSourceInformation.getSourceInformation(ctx));
        configuration.awsCredentials = this.visitAWSCredentials(awsCredentialsContext);

        return configuration;
    }

    public GCPWIFWithAWSIdPAuthenticationSpecification.WorkloadConfiguration visitGCPWithAWSIdPWorkload(AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_gcpWorkloadContext ctx)
    {
        GCPWIFWithAWSIdPAuthenticationSpecification.WorkloadConfiguration workloadConfiguration = new GCPWIFWithAWSIdPAuthenticationSpecification.WorkloadConfiguration();

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_gcpWorkload_projectNumberContext projectNumberContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_gcpWorkload_projectNumber(), "projectNumber", walkerSourceInformation.getSourceInformation(ctx));
        String projectNumber = PureGrammarParserUtility.fromGrammarString(projectNumberContext.STRING().getText(), true);
        workloadConfiguration.projectNumber = projectNumber;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_gcpWorkload_providerIdContext providerIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_gcpWorkload_providerId(), "providerId", walkerSourceInformation.getSourceInformation(ctx));
        String providerId = PureGrammarParserUtility.fromGrammarString(providerIdContext.STRING().getText(), true);
        workloadConfiguration.providerId = providerId;

        AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_gcpWorkload_poolIdContext poolIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.gcpWIFWithAWSIdPAuthentication_gcpWorkload_poolId(), "poolId", walkerSourceInformation.getSourceInformation(ctx));
        String poolId = PureGrammarParserUtility.fromGrammarString(poolIdContext.STRING().getText(), true);
        workloadConfiguration.poolId  = poolId;

        return workloadConfiguration;
    }

    public AuthenticationSpecification visitApiKeyAuthentication(AuthenticationParserGrammar.ApiKeyAuthenticationContext ctx)
    {
        ApiKeyAuthenticationSpecification authenticationSpecification = new ApiKeyAuthenticationSpecification();
        authenticationSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationParserGrammar.ApiKeyAuthentication_locationContext locationContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.apiKeyAuthentication_location(), "location", authenticationSpecification.getSourceInformation());
        String location = PureGrammarParserUtility.fromGrammarString(locationContext.STRING().getText(), true);
        authenticationSpecification.location = ApiKeyAuthenticationSpecification.Location.valueOf(location.toUpperCase());

        AuthenticationParserGrammar.ApiKeyAuthentication_keyNameContext keyNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.apiKeyAuthentication_keyName(), "keyName", authenticationSpecification.getSourceInformation());
        String keyName = PureGrammarParserUtility.fromGrammarString(keyNameContext.STRING().getText(), true);
        authenticationSpecification.keyName = keyName;

        AuthenticationParserGrammar.ApiKeyAuthentication_valueContext valueContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.apiKeyAuthentication_value(), "value", authenticationSpecification.getSourceInformation());
        authenticationSpecification.value = visitCredentialVaultSecret(valueContext.secret_value());

        return authenticationSpecification;
    }

    public AuthenticationSpecification visitUserPasswordAuthentication(AuthenticationParserGrammar.UserPasswordAuthenticationContext ctx)
    {
        UserPasswordAuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification();
        authenticationSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationParserGrammar.UserPasswordAuthentication_usernameContext usernameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.userPasswordAuthentication_username(), "userName", authenticationSpecification.getSourceInformation());
        String userNameValue = PureGrammarParserUtility.fromGrammarString(usernameContext.STRING().getText(), true);
        authenticationSpecification.username = userNameValue;

        AuthenticationParserGrammar.UserPasswordAuthentication_passwordContext passwordContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.userPasswordAuthentication_password(), "password", authenticationSpecification.getSourceInformation());
        authenticationSpecification.password = visitCredentialVaultSecret(passwordContext.secret_value());

        return authenticationSpecification;
    }

    public EncryptedPrivateKeyPairAuthenticationSpecification visitEncryptedKeyPairAuthentication(AuthenticationParserGrammar.EncryptedPrivateKeyAuthenticationContext ctx)
    {
        EncryptedPrivateKeyPairAuthenticationSpecification authenticationSpecification = new EncryptedPrivateKeyPairAuthenticationSpecification();
        authenticationSpecification.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        AuthenticationParserGrammar.EncryptedPrivateKeyAuthentication_privateKeyContext privateKeyContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.encryptedPrivateKeyAuthentication_privateKey(), "privateKey", authenticationSpecification.getSourceInformation());
        authenticationSpecification.privateKey = visitCredentialVaultSecret(privateKeyContext.secret_value());

        AuthenticationParserGrammar.EncryptedPrivateKeyAuthentication_passphraseContext passphraseContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.encryptedPrivateKeyAuthentication_passphrase(), "passphrase", authenticationSpecification.getSourceInformation());
        authenticationSpecification.passphrase = visitCredentialVaultSecret(passphraseContext.secret_value());

        return authenticationSpecification;
    }

    public CredentialVaultSecret visitCredentialVaultSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        CredentialVaultSecretParseTreeWalker credentialVaultSecretParseTreeWalker = new CredentialVaultSecretParseTreeWalker(walkerSourceInformation, context);
        return credentialVaultSecretParseTreeWalker.visitCredentialVaultSecret(secretContext);
    }

    public AWSCredentials visitAWSCredentials(AuthenticationParserGrammar.GcpWIFWithAWSIdPAuthentication_awsIdp_awsCredentialsContext awsCredentialsContext)
    {
        AWSCredentialsParser awsCredentialsParser = new AWSCredentialsParser(walkerSourceInformation, context, new CredentialVaultSecretParseTreeWalker(walkerSourceInformation, context));
        return awsCredentialsParser.visitAWSCredentials(awsCredentialsContext.awsCredentialsValue());
    }
}
