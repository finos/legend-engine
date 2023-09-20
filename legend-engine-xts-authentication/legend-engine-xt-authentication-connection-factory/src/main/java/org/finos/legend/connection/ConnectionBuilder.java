// Copyright 2023 Goldman Sachs
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

package org.finos.legend.connection;

import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.finos.legend.engine.shared.core.identity.Credential;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;

public abstract class ConnectionBuilder<T, CRED extends Credential, SPEC extends ConnectionSpecification>
{
    public abstract T getConnection(CRED credential, SPEC connectionSpecification, StoreInstance storeInstance) throws Exception;

    public Class<? extends Credential> getCredentialType()
    {
        return (Class<? extends Credential>) actualTypeArguments()[1];
    }

    public Class<? extends ConnectionSpecification> getConnectionSpecificationType()
    {
        return (Class<? extends ConnectionSpecification>) actualTypeArguments()[2];
    }

    private Type[] actualTypeArguments()
    {
        Type genericSuperClass = this.getClass().getGenericSuperclass();
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperClass;
        return parameterizedType.getActualTypeArguments();
    }

    public static class Key
    {
        private final Class<? extends ConnectionSpecification> connectionSpecificationType;
        private final Class<? extends Credential> credentialType;

        public Key(Class<? extends ConnectionSpecification> connectionSpecificationType, Class<? extends Credential> credentialType)
        {
            this.connectionSpecificationType = connectionSpecificationType;
            this.credentialType = credentialType;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            ConnectionBuilder.Key that = (ConnectionBuilder.Key) o;
            return this.connectionSpecificationType.equals(that.connectionSpecificationType) &&
                    this.credentialType.equals(that.credentialType);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(connectionSpecificationType, credentialType);
        }
    }
}
