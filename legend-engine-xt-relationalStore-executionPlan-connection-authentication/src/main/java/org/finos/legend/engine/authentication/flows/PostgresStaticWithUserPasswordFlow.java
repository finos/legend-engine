package org.finos.legend.engine.authentication.flows;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

public class PostgresStaticWithUserPasswordFlow implements DatabaseAuthenticationFlow<StaticDatasourceSpecification, UserNamePasswordAuthenticationStrategy>
{
    @Override
    public Class<StaticDatasourceSpecification> getDatasourceClass()
    {
        return StaticDatasourceSpecification.class;
    }

    @Override
    public Class<UserNamePasswordAuthenticationStrategy> getAuthenticationStrategyClass()
    {
        return UserNamePasswordAuthenticationStrategy.class;
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Postgres;
    }

    @Override
    public Credential makeCredential(Identity identity, StaticDatasourceSpecification datasourceSpecification, UserNamePasswordAuthenticationStrategy authStrategy) throws Exception
    {
        String userNameVaultKey = authStrategy.baseVaultReference == null ? authStrategy.userNameVaultReference : authStrategy.baseVaultReference + authStrategy.userNameVaultReference;
        String passwordVaultKey = authStrategy.baseVaultReference == null ? authStrategy.passwordVaultReference : authStrategy.baseVaultReference + authStrategy.passwordVaultReference;
        String userName = Vault.INSTANCE.getValue(userNameVaultKey);
        String password = Vault.INSTANCE.getValue(passwordVaultKey);
        return new PlaintextUserPasswordCredential(userName, password);
    }
}
