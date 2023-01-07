package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.aws;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
public abstract class AWSCredentials
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AWSCredentialsVisitor<T> visitor);
}
