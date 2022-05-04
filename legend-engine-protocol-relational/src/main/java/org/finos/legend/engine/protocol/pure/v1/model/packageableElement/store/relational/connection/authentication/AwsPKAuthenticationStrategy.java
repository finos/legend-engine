package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

public class AwsPKAuthenticationStrategy extends AuthenticationStrategy
{
    public String secretArn;
    public String user;

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }
}
