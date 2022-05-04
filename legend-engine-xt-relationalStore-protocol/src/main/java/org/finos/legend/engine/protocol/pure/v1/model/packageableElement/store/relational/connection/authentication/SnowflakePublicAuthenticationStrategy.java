package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication;

public class SnowflakePublicAuthenticationStrategy extends AuthenticationStrategy
{
    public String privateKeyVaultReference;
    public String passPhraseVaultReference;
    public String publicUserName;

    @Override
    public <T> T accept(AuthenticationStrategyVisitor<T> authenticationStrategyVisitor)
    {
        return authenticationStrategyVisitor.visit(this);
    }
}
