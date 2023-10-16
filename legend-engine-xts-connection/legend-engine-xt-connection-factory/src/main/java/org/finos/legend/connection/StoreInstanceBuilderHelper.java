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

import org.eclipse.collections.impl.utility.ListIterate;

public class StoreInstanceBuilderHelper
{
    public static StoreSupport buildStoreSupport(org.finos.legend.engine.protocol.pure.v1.connection.StoreSupport storeSupport, LegendEnvironment environment)
    {
        return new StoreSupport.Builder()
                .withIdentifier(storeSupport.identifier)
                .withAuthenticationMechanismConfigurations(
                        ListIterate.collect(storeSupport.authenticationMechanisms, mechanism ->
                                new AuthenticationMechanismConfiguration.Builder(environment.getAuthenticationMechanism(mechanism.mechanism))
                                        .withAuthenticationConfigurationTypes(
                                                ListIterate.collect(mechanism.configurationTypes, environment::getAuthenticationConfigurationType)
                                        )
                                        .build())
                )
                .build();
    }

    public static StoreInstance buildStoreInstance(org.finos.legend.engine.protocol.pure.v1.connection.StoreInstance protocol, LegendEnvironment environment)
    {
        return new StoreInstance.Builder(environment.getStoreSupport(protocol.storeSupport))
                .withIdentifier(protocol.identifier)
                .withAuthenticationMechanismConfigurations(
                        ListIterate.collect(protocol.authenticationMechanisms, mechanism ->
                                new AuthenticationMechanismConfiguration.Builder(environment.getAuthenticationMechanism(mechanism.mechanism))
                                        .withAuthenticationConfigurationTypes(
                                                ListIterate.collect(mechanism.configurationTypes, environment::getAuthenticationConfigurationType)
                                        )
                                        .build())
                )
                .withConnectionSpecification(protocol.connectionSpecification)
                .build();
    }
}
