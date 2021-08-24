/*
package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeDataSourceSpecificationKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategyVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;

import java.util.List;
import java.util.function.Function;

public class SnowflakeDataSourceSpecificationExtension implements StrategicConnectionExtension
{
    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators() {
      return new AuthenticationStrategyVisitor<AuthenticationStrategyKey>() {
          @Override
          public AuthenticationStrategyKey visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy authenticationStrategy) {
              // TODO : remove null
              return null;
          }
      };
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles) {
        return new AuthenticationStrategyVisitor<AuthenticationStrategy>() {
            @Override
            public AuthenticationStrategy visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy authenticationStrategy) {
                // TODO : remove null
                return null;
            }
        };
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort) {
        return new Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>() {
            @Override
            public DatasourceSpecificationVisitor<DataSourceSpecificationKey> apply(RelationalDatabaseConnection relationalDatabaseConnection) {
                return new DatasourceSpecificationVisitor<DataSourceSpecificationKey>() {
                    @Override
                    public DataSourceSpecificationKey visit(DatasourceSpecification datasourceSpecification) {
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
                };
            }
        };
    }

    @Override
    public Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>> getExtraDataSourceSpecificationTransformerGenerators(List<OAuthProfile> oauthProfiles, RelationalExecutorInfo relationalExecutorInfo) {

        return new Function2<RelationalDatabaseConnection, ConnectionKey, DatasourceSpecificationVisitor<DataSourceSpecification>>() {
            @Override
            public DatasourceSpecificationVisitor<DataSourceSpecification> value(RelationalDatabaseConnection relationalDatabaseConnection, ConnectionKey connectionKey) {
                return new DatasourceSpecificationVisitor<DataSourceSpecification>() {
                    @Override
                    public DataSourceSpecification visit(DatasourceSpecification datasourceSpecification) {
                        return new SnowflakeDataSourceSpecification(
                                (SnowflakeDataSourceSpecificationKey) connectionKey.getDataSourceSpecificationKey(),
                                new SnowflakeManager(),
                                resolveAuthenticationStrategy(relationalDatabaseConnection, oauthProfiles, connectionKey),
                                relationalExecutorInfo
                        );
                    }
                };
            }
        };
    }

    private AuthenticationStrategy resolveAuthenticationStrategy(RelationalDatabaseConnection r, List<OAuthProfile> oauthProfiles, ConnectionKey connectionKey)
    {
        AuthenticationStrategyVisitor<AuthenticationStrategy> extraAuthenticationStrategyTransformGenerators = new UserPasswordAuthenticationStrategyExtension().getExtraAuthenticationStrategyTransformGenerators(oauthProfiles);
        AuthenticationStrategy visit = extraAuthenticationStrategyTransformGenerators.visit(r.authenticationStrategy);
        return visit;
    }
}
*/
