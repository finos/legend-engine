// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.authentication.provider;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;

import java.util.ServiceLoader;

public class DynamicMergeDatabaseAuthenticationFlowProvider extends AbstractDatabaseAuthenticationFlowProvider
{
    private ImmutableList<DatabaseAuthenticationFlow<? extends DatasourceSpecification, ? extends AuthenticationStrategy>> flows()
    {
        return Lists.immutable.withAll(Iterate.addAllTo(ServiceLoader.load(DatabaseAuthenticationFlowProvider.class), Lists.mutable.empty())
                .collect(c ->
                        {
                            c.configure(null);
                            return c;
                        }
                ).flatCollect(x -> x.getFlows().values()).collect(a -> (DatabaseAuthenticationFlow<?, ?>) a));
    }

    @Override
    public void configure(DatabaseAuthenticationFlowProviderConfiguration configuration)
    {
        flows().forEach(this::registerFlow);
    }
}
