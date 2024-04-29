/*
 Copyright 2021 Goldman Sachs

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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TrinoDelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.flows.DatabaseAuthenticationFlowKey;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_authentication_TrinoDelegatedKerberosAuthenticationStrategy_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_TrinoDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_TrinoSSLSpecification_Impl;

import java.util.List;

public class TrinoCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_alloy_connections_alloy_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Lists.mutable.with((authenticationStrategy, context) ->
        {
            if (authenticationStrategy instanceof TrinoDelegatedKerberosAuthenticationStrategy)
            {
                TrinoDelegatedKerberosAuthenticationStrategy trinoAuthenticationStrategy = (TrinoDelegatedKerberosAuthenticationStrategy) authenticationStrategy;
                return new Root_meta_pure_alloy_connections_alloy_authentication_TrinoDelegatedKerberosAuthenticationStrategy_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::authentication::TrinoDelegatedKerberosAuthenticationStrategy"))
                        ._serverPrincipal(trinoAuthenticationStrategy.serverPrincipal)
                        ._kerberosRemoteServiceName(trinoAuthenticationStrategy.kerberosRemoteServiceName)
                        ._kerberosUseCanonicalHostname(trinoAuthenticationStrategy.kerberosUseCanonicalHostname);
            }
            return null;
        });
    }

    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof TrinoDatasourceSpecification)
            {
                TrinoDatasourceSpecification trinoDatasourceSpecification = (TrinoDatasourceSpecification) datasourceSpecification;
                return new Root_meta_pure_alloy_connections_alloy_specification_TrinoDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::TrinoDatasourceSpecification"))
                        ._host(trinoDatasourceSpecification.host)
                        ._port(trinoDatasourceSpecification.port)
                        ._catalog(trinoDatasourceSpecification.catalog)
                        ._schema(trinoDatasourceSpecification.schema)
                        ._clientTags(trinoDatasourceSpecification.clientTags)
                        ._sslSpecification(trinoDatasourceSpecification == null ? null : new Root_meta_pure_alloy_connections_alloy_specification_TrinoSSLSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::TrinoSSLSpecification"))
                                ._ssl(trinoDatasourceSpecification.sslSpecification.ssl)
                                ._trustStorePathVaultReference(trinoDatasourceSpecification.sslSpecification.trustStorePathVaultReference)
                                ._trustStorePasswordVaultReference(trinoDatasourceSpecification.sslSpecification.trustStorePasswordVaultReference)
                        );
            }
            return null;
        });
    }

    @Override
    public CompilerExtension build()
    {
        return new TrinoCompilerExtension();
    }

    @Override
    public List<DatabaseAuthenticationFlowKey> getFlowKeys()
    {
        return Lists.mutable.of(DatabaseAuthenticationFlowKey.newKey(DatabaseType.Trino, TrinoDatasourceSpecification.class, TrinoDelegatedKerberosAuthenticationStrategy.class),
                DatabaseAuthenticationFlowKey.newKey(DatabaseType.Trino, TrinoDatasourceSpecification.class, UserNamePasswordAuthenticationStrategy.class));
    }
}

