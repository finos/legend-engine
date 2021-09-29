package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        // Pointer to `PackageableAuthorizer`
        @JsonSubTypes.Type(value = AuthorizerPointer.class, name = "authorizerPointer"),
})
public abstract class Authorizer
{
    public SourceInformation sourceInformation;

    public abstract <T> T accept(AuthorizerVisitor<T> authorizerVisitor);
}
