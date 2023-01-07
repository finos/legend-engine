package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public class AWSDefaultCredentials extends AWSCredentials
{
    public AWSDefaultCredentials()
    {
    }

    @Override
    public <T> T accept(AWSCredentialsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }
}
