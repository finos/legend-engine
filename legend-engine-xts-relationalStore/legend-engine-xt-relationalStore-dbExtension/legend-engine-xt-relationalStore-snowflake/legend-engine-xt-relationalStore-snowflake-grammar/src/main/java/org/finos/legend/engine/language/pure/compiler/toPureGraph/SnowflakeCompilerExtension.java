/*
 Copyright 2023 Goldman Sachs

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http:www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl;

import java.util.List;

public class SnowflakeCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Snowflake");
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((authenticationStrategy, context) ->
        {
            if (authenticationStrategy instanceof SnowflakePublicAuthenticationStrategy)
            {
                return new Root_meta_pure_alloy_connections_alloy_authentication_SnowflakePublicAuthenticationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::authentication::SnowflakePublicAuthenticationStrategy"))
                        ._publicUserName(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).publicUserName)
                        ._privateKeyVaultReference(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).privateKeyVaultReference)
                        ._passPhraseVaultReference(((SnowflakePublicAuthenticationStrategy) authenticationStrategy).passPhraseVaultReference);
            }
            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
            {
                SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
                if (snowflakeDatasourceSpecification.tempTableDb != null ^ snowflakeDatasourceSpecification.tempTableSchema != null)
                {
                    throw new RuntimeException("One of Database name and schema name for temp tables is missing. Please specify both tempTableDb and tempTableSchema");
                }
                Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification _snowflake = new Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::SnowflakeDatasourceSpecification"));
                _snowflake._accountName(snowflakeDatasourceSpecification.accountName);
                _snowflake._region(snowflakeDatasourceSpecification.region);
                _snowflake._warehouseName(snowflakeDatasourceSpecification.warehouseName);
                _snowflake._databaseName(snowflakeDatasourceSpecification.databaseName);
                _snowflake._cloudType(snowflakeDatasourceSpecification.cloudType);
                _snowflake._quotedIdentifiersIgnoreCase(snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase);
                _snowflake._enableQueryTags(snowflakeDatasourceSpecification.enableQueryTags);
                _snowflake._proxyHost(snowflakeDatasourceSpecification.proxyHost);
                _snowflake._proxyPort(snowflakeDatasourceSpecification.proxyPort);
                _snowflake._nonProxyHosts(snowflakeDatasourceSpecification.nonProxyHosts);
                _snowflake._tempTableDb(snowflakeDatasourceSpecification.tempTableDb);
                _snowflake._tempTableSchema(snowflakeDatasourceSpecification.tempTableSchema);
                if (snowflakeDatasourceSpecification.accountType != null)
                {
                    _snowflake._accountType(context.pureModel.getEnumValue("meta::pure::alloy::connections::alloy::specification::SnowflakeAccountType", snowflakeDatasourceSpecification.accountType));
                }
                _snowflake._organization(snowflakeDatasourceSpecification.organization);
                _snowflake._role(snowflakeDatasourceSpecification.role);

                return _snowflake;
            }
            return null;
        });
    }

    @Override
    public CompilerExtension build()
    {
        return new SnowflakeCompilerExtension();
    }

    @Override
    public List<DatabaseAuthenticationFlowKey> getFlowKeys()
    {
        return Lists.mutable.of(DatabaseAuthenticationFlowKey.newKey(DatabaseType.Snowflake, SnowflakeDatasourceSpecification.class, SnowflakePublicAuthenticationStrategy.class));
    }
}

