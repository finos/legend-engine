package org.finos.legend.engine.plan.execution.stores.relational.connection.extension;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.UserPasswordAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.RelationalConnectionPlugin;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

public class SnowflakeRelationalConnectionPlugin implements RelationalConnectionPlugin
{
    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.Snowflake;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return new SnowflakeManager();
    }

    @Override
    public DataSourceSpecification buildDatasourceSpecification(RelationalDatabaseConnection relationalDatabaseConnectionProtocol, RelationalExecutorInfo relationalExecutorInfo)
    {
        AuthenticationStrategy authenticationStrategy = buildAuthenticationStrategy(relationalDatabaseConnectionProtocol.authenticationStrategy);
        SnowflakeDataSourceSpecificationKey datasourceSpecificationKey = buildDatasourceSpecificationKey(relationalDatabaseConnectionProtocol);
        return new SnowflakeDataSourceSpecification(datasourceSpecificationKey, this.getDatabaseManager(), authenticationStrategy, relationalExecutorInfo);
    }

    @Override
    public ConnectionKey buildConnectionKey(RelationalDatabaseConnection relationalDatabaseConnectionProtocol)
    {
        AuthenticationStrategyKey authenticationStrategyKey = buildAuthenticationStrategyKey(relationalDatabaseConnectionProtocol.authenticationStrategy);
        SnowflakeDataSourceSpecificationKey datasourceSpecificationKey = buildDatasourceSpecificationKey(relationalDatabaseConnectionProtocol);
        return new ConnectionKey(datasourceSpecificationKey, authenticationStrategyKey);
    }

    private SnowflakeDataSourceSpecificationKey buildDatasourceSpecificationKey(RelationalDatabaseConnection relationalDatabaseConnectionProtocol) {
        DatasourceSpecification datasourceSpecification = relationalDatabaseConnectionProtocol.datasourceSpecification;
        if (!(datasourceSpecification instanceof org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification))
        {
            throw new UnsupportedOperationException("Unsupported datasource specification : " + datasourceSpecification.getClass());
        }
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification snowflakeDatasourceSpecification =
                (org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification) datasourceSpecification;
        return new SnowflakeDataSourceSpecificationKey(
                snowflakeDatasourceSpecification.accountName,
                snowflakeDatasourceSpecification.region,
                snowflakeDatasourceSpecification.warehouseName,
                snowflakeDatasourceSpecification.databaseName,
                snowflakeDatasourceSpecification.cloudType,
                snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase);
    }

    private AuthenticationStrategyKey buildAuthenticationStrategyKey(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy authenticationStrategy)
    {
        return this.buildAuthenticationStrategy(authenticationStrategy).getKey();
    }

    private AuthenticationStrategy buildAuthenticationStrategy(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy authenticationStrategy) {
        // TODO : epsstan : assert on auth type
        return new UserPasswordAuthenticationStrategy((org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserPasswordAuthenticationStrategy)authenticationStrategy);
    }
}
