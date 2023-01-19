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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.AWSDefaultCredentials;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws.StaticAWSCredentials;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class AWSCredentialsParser
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final PureGrammarParserContext context;
    private final CredentialVaultSecretParser credentialVaultSecretParser;

    public AWSCredentialsParser(ParseTreeWalkerSourceInformation walkerSourceInformation, PureGrammarParserContext context, CredentialVaultSecretParser credentialVaultSecretParser)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.context = context;
        this.credentialVaultSecretParser = credentialVaultSecretParser;
    }

    public AWSCredentials visitAWSCredentials(AuthenticationParserGrammar.AwsCredentialsValueContext awsCredentialsContext)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(awsCredentialsContext);

        if (awsCredentialsContext.awsDefaultCredentialsValue() != null)
        {
            return this.visitAWSDefaultCredentials(awsCredentialsContext.awsDefaultCredentialsValue());
        }

        if (awsCredentialsContext.awsStaticCredentialsValue() != null)
        {
            return this.visitAWSStaticCredentials(awsCredentialsContext.awsStaticCredentialsValue());
        }
        throw new EngineException("Unrecognized aws credentials", sourceInformation, EngineErrorType.PARSER);
    }

    private AWSCredentials visitAWSDefaultCredentials(AuthenticationParserGrammar.AwsDefaultCredentialsValueContext awsDefaultCredentialsValue)
    {
        return new AWSDefaultCredentials();
    }

    private AWSCredentials visitAWSStaticCredentials(AuthenticationParserGrammar.AwsStaticCredentialsValueContext awsStaticCredentialsValue)
    {
        StaticAWSCredentials staticAWSCredentials = new StaticAWSCredentials();
        staticAWSCredentials.sourceInformation = walkerSourceInformation.getSourceInformation(awsStaticCredentialsValue);

        AuthenticationParserGrammar.AwsStaticCredentialsValue_accessKeyIdContext accessKeyIdContext = PureGrammarParserUtility.validateAndExtractRequiredField(awsStaticCredentialsValue.awsStaticCredentialsValue_accessKeyId(), "accessKeyId", staticAWSCredentials.sourceInformation);
        staticAWSCredentials.accessKeyId = this.credentialVaultSecretParser.visitCredentialVaultSecret(accessKeyIdContext.secret_value());

        AuthenticationParserGrammar.AwsStaticCredentialsValue_secretAccessKeyContext secretAccessKeyContext = PureGrammarParserUtility.validateAndExtractRequiredField(awsStaticCredentialsValue.awsStaticCredentialsValue_secretAccessKey(), "secretAccessKey", staticAWSCredentials.sourceInformation);
        staticAWSCredentials.secretAccessKey = this.credentialVaultSecretParser.visitCredentialVaultSecret(secretAccessKeyContext.secret_value());
        return staticAWSCredentials;
    }
}
