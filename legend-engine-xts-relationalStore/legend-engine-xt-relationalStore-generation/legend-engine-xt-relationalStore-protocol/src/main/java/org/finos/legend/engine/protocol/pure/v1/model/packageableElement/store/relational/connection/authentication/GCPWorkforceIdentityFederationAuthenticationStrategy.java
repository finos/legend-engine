package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

public class GCPWorkforceIdentityFederationAuthenticationStrategy extends AuthenticationStrategy
{
    public GCPWorkforceIdentityFederationAuthenticationStrategy()
    {
    }

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }
}
