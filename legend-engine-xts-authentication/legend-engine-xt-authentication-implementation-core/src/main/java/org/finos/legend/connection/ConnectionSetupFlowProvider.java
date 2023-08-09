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

import org.finos.legend.engine.shared.core.identity.Credential;

import java.util.Objects;
import java.util.Optional;

public interface ConnectionSetupFlowProvider
{
    <T> Optional<ConnectionSetupFlow<T, ConnectionSetupSpecification<T>, Credential>> lookupFlow(ConnectionSetupSpecification<T> connectionSetupSpecification, Credential credential);

    default <T> ConnectionSetupFlow<T, ConnectionSetupSpecification<T>, Credential> lookupFlowOrThrow(ConnectionSetupSpecification<T> connectionSetupSpecification, Credential credential)
    {
        Optional<ConnectionSetupFlow<T, ConnectionSetupSpecification<T>, Credential>> flowHolder = this.lookupFlow(connectionSetupSpecification, credential);
        return flowHolder.orElseThrow(() -> new RuntimeException(String.format("Unsupported connection setup flow: Specification=%s, Credential=%s",
                connectionSetupSpecification.getClass().getSimpleName(),
                credential.getClass().getSimpleName())));
    }

    /**
     * TODO: if we want to get advanced, we can mimic what we do for DatabaseAuthenticationFlowProvider
     * where a flow provider implementation is basically a collection of flows that the system should support
     * we can also allow configuring the flow provider to pick and finding in the classpath via service loader
     */
    void configure();

    public static class ConnectionSetupFlowKey
    {
        private final Class<? extends ConnectionSetupSpecification> connecionSetupSpecificationClass;
        private final Class<? extends Credential> credentialClass;

        public ConnectionSetupFlowKey(Class<? extends ConnectionSetupSpecification> connecionSetupSpecificationClass, Class<? extends Credential> credentialClass)
        {
            this.connecionSetupSpecificationClass = connecionSetupSpecificationClass;
            this.credentialClass = credentialClass;
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
            ConnectionSetupFlowKey that = (ConnectionSetupFlowKey) o;
            return connecionSetupSpecificationClass.equals(that.connecionSetupSpecificationClass) &&
                    credentialClass.equals(that.credentialClass);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(connecionSetupSpecificationClass, credentialClass);
        }
    }
}
