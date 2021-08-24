package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.UserPasswordAuthenticationStrategyKey;
import org.finos.legend.engine.shared.core.vault.Vault;

public class UserPasswordAuthenticationStrategy extends AuthenticationStrategy  {

    private org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserPasswordAuthenticationStrategy protocol;

    public UserPasswordAuthenticationStrategy(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserPasswordAuthenticationStrategy protocol)
    {
        this.protocol = protocol;
    }
    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new UserPasswordAuthenticationStrategyKey(this.protocol.publicUserName, this.protocol.passwordVaultReference);
    }

    public String getPassword()
    {
        String value = Vault.INSTANCE.getValue(this.protocol.passwordVaultReference);
        return value;
    }
}
