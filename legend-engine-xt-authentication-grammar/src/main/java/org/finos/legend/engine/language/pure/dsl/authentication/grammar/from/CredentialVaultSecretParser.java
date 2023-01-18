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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.EnvironmentCredentialVaultSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.SystemPropertiesSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSSecretsManagerSecret;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class CredentialVaultSecretParser
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext context;

    public CredentialVaultSecretParser(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext context)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.context = context;
    }

    public CredentialVaultSecret visitCredentialVaultSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(secretContext);
        if (secretContext.propertiesSecret() != null)
        {
            return this.visitPropertiesFileSecret(secretContext);
        }
        else if (secretContext.environmentSecret() != null)
        {
            return this.visitEnvironmentSecret(secretContext);
        }
        else if (secretContext.systemPropertiesSecret() != null)
        {
            return this.visitSystemPropertiesSecret(secretContext);
        }
        else if (secretContext.awsSecretsManagerSecret() != null)
        {
            return this.awsSecretsManagerCredentialVaultSecret(secretContext);
        }
        throw new EngineException("Unrecognized secret", sourceInformation, EngineErrorType.PARSER);
    }

    private AWSSecretsManagerSecret awsSecretsManagerCredentialVaultSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        AWSSecretsManagerSecret credentialVaultSecret = new AWSSecretsManagerSecret();
        AuthenticationParserGrammar.AwsSecretsManagerSecret_secretIdContext secretIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.awsSecretsManagerSecret().awsSecretsManagerSecret_secretId(), "secretId", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.secretId = PureGrammarParserUtility.fromGrammarString(secretIdContext.STRING().getText(), true);

        AuthenticationParserGrammar.VersionIdContext versionIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.awsSecretsManagerSecret().versionId(), "versionId", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.versionId = PureGrammarParserUtility.fromGrammarString(versionIdContext.STRING().getText(), true);

        AuthenticationParserGrammar.VersionStageContext versionStage = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.awsSecretsManagerSecret().versionStage(), "versionStage", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.versionStage = PureGrammarParserUtility.fromGrammarString(versionStage.STRING().getText(), true);

        AuthenticationParserGrammar.AwsSecretsManagerSecret_awsCredentialsContext awsCredentialsContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.awsSecretsManagerSecret().awsSecretsManagerSecret_awsCredentials(), "awsCredentials", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.awsCredentials = this.visitAWSCredentials(awsCredentialsContext.awsCredentialsValue());

        return credentialVaultSecret;
    }

    private CredentialVaultSecret visitSystemPropertiesSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        SystemPropertiesSecret credentialVaultSecret = new SystemPropertiesSecret();
        credentialVaultSecret.sourceInformation = walkerSourceInformation.getSourceInformation(secretContext);
        AuthenticationParserGrammar.SystemPropertiesSecret_systemPropertyNameContext systemPropertyNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.systemPropertiesSecret().systemPropertiesSecret_systemPropertyName(), "systemPropertyName", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.systemPropertyName = PureGrammarParserUtility.fromGrammarString(systemPropertyNameContext.STRING().getText(), true);

        return credentialVaultSecret;
    }


    private EnvironmentCredentialVaultSecret visitEnvironmentSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        EnvironmentCredentialVaultSecret credentialVaultSecret = new EnvironmentCredentialVaultSecret();
        credentialVaultSecret.sourceInformation = walkerSourceInformation.getSourceInformation(secretContext);
        AuthenticationParserGrammar.EnvironmentSecret_envVariableNameContext envVariableNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.environmentSecret().environmentSecret_envVariableName(), "envVariableName", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.envVariableName = PureGrammarParserUtility.fromGrammarString(envVariableNameContext.STRING().getText(), true);

        return credentialVaultSecret;
    }

    private PropertiesFileSecret visitPropertiesFileSecret(AuthenticationParserGrammar.Secret_valueContext secretContext)
    {
        PropertiesFileSecret credentialVaultSecret = new PropertiesFileSecret();
        credentialVaultSecret.sourceInformation = walkerSourceInformation.getSourceInformation(secretContext);
        AuthenticationParserGrammar.PropertiesSecret_propertyNameContext propertyNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(secretContext.propertiesSecret().propertiesSecret_propertyName(), "propertyName", credentialVaultSecret.sourceInformation);
        credentialVaultSecret.propertyName = PureGrammarParserUtility.fromGrammarString(propertyNameContext.STRING().getText(), true);

        return credentialVaultSecret;
    }

    private AWSCredentials visitAWSCredentials(AuthenticationParserGrammar.AwsCredentialsValueContext awsCredentialsContext)
    {
        AWSCredentialsParser awsCredentialsParser = new AWSCredentialsParser(walkerSourceInformation, context, this);
        return awsCredentialsParser.visitAWSCredentials(awsCredentialsContext);
    }
}
