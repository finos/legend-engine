package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer;

public class AuthorizerPointer extends Authorizer
{
    public String authorizer;

    @Override
    public <T> T accept(AuthorizerVisitor<T> authorizerVisitor) {
        return authorizerVisitor.visit(this);
    }
}
