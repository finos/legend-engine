// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.authentication;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;
import org.finos.legend.engine.shared.core.identity.Identity;

/*
    A "flow" produces a credential that can be used to acquire a database connection.

    Producing a credential requires the following inputs :
    1/ identity - This is the caller identity e.g GSSSO cookie
    2/ datasource spec - This is metadata about the database to connect to e.g AWS account name
    3/ authentication spec - This is metadata that influences the type of credential to be produced e.g OAuth token

    We have cases where the type of the datasource spec does not uniquely identify the database type. e.g StaticDatasourceSpecification.
    To support these use cases, a flow declares the database type that it supports. This declared type is used in resolving a user request to the appropriate authentication flow.

    Thread Safety :
    A flow object can be accessed by multiple threads. Flow objects should be stateless and therefore thread safe. Or if they maintain state, the state should be thread safe.
    Acquiring a credential might require multiple network round trips. Holding locks for the entire duration of the flow might cause performance issues.

 */
public interface DatabaseAuthenticationFlow<D extends DatasourceSpecification, A extends AuthenticationStrategy>
{
    Class<D> getDatasourceClass();

    Class<A> getAuthenticationStrategyClass();

    DatabaseType getDatabaseType();

    /*
        A flow provides a database credential.
        In cases like GCP Application Default Credentials, where the credential might already be available in the runtime environment (e.g GCP K8s), the flow still has to provide a no-op credential object.
     */
    Credential makeCredential(Identity identity, D datasourceSpecification, A authenticationStrategy) throws Exception;

    default Credential makeCredential(Identity identity, D datasourceSpecification, A authenticationStrategy, RuntimeContext credentialAcquisitionContext) throws Exception
    {
           return this.makeCredential(identity, datasourceSpecification, authenticationStrategy);
    }

    /*
        This is intended to carry 'runtime context' that is not part of the model (i.e datasource spec and auth spec), but is needed to create a credential.
        For e.g. this can be used to inject server/platform configuration required to connect to external vault.
     */
    class RuntimeContext
    {
        private ImmutableMap<String, String> contextParams = Maps.immutable.empty();

        public static RuntimeContext newWith(ImmutableMap<String, String> contextParams)
        {
            return new RuntimeContext(contextParams);
        }

        public static RuntimeContext empty()
        {
            return new RuntimeContext();
        }

        private RuntimeContext(ImmutableMap<String, String> contextParams)
        {
            this.contextParams = contextParams;
        }

        private RuntimeContext()
        {
        }

        public ImmutableMap<String, String> getContextParams()
        {
            return contextParams;
        }
    }
}