/*
package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications;

import org.eclipse.collections.api.block.function.Function2;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.OAuthProfile;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
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

public class SnowflakeDataSourceSpecificationTransformer implements StrategicConnectionExtension
{
    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategyKey> getExtraAuthenticationKeyGenerators() {
        return null;
    }

    @Override
    public AuthenticationStrategyVisitor<AuthenticationStrategy> getExtraAuthenticationStrategyTransformGenerators(List<OAuthProfile> oauthProfiles) {
        return null;
    }

    @Override
    public Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>> getExtraDataSourceSpecificationKeyGenerators(int testDbPort) {
        return new Function<RelationalDatabaseConnection, DatasourceSpecificationVisitor<DataSourceSpecificationKey>>(){

            @Override
            public DatasourceSpecificationVisitor<DataSourceSpecificationKey> apply(RelationalDatabaseConnection relationalDatabaseConnection) {
                return new DatasourceSpecificationVisitor<DataSourceSpecificationKey>() {
                    @Override
                    public DataSourceSpecificationKey visit(DatasourceSpecification datasourceSpecification) {
                        return new SnowflakeDataSourceSpecificationKey(null, null, null, null, null, false);
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
                        return null;
                    }
                };
            }
        };
    }
}
*/
