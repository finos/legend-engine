package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.CredentialVaultSecret;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public class StaticAWSCredentials extends AWSCredentials
{
    public CredentialVaultSecret accessKeyId;
    public CredentialVaultSecret secretAccessKey;

    public StaticAWSCredentials()
    {
    }

    public StaticAWSCredentials(CredentialVaultSecret accessKeyId, CredentialVaultSecret secretAccessKey)
    {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
    }

    @Override
    public <T> T accept(AWSCredentialsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
