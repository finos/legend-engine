package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

public class UserPasswordAuthenticationStrategy extends AuthenticationStrategy
{
    public String userName;
    public String passwordVaultReference;

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }
}
