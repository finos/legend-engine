package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElementVisitor;

public class PackageableAuthorizer extends PackageableElement
{
    public Authorizer authorizerValue;

    @Override
    public <T> T accept(PackageableElementVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
