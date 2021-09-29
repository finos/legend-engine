package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authorizer;

public interface AuthorizerVisitor<T> {

    T visit(Authorizer authorizer);

    T visit(AuthorizerPointer authorizerPointer);
}
