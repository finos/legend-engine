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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.AuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.KerberosAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.UserPasswordAuthenticationConfiguration;

import java.util.List;
import java.util.Map;

public class ConnectionProtocolExtension implements PureProtocolExtension
{
    public static final String CONNECTION_CLASSIFIER_PATH = "meta::pure::metamodel::connection::Connection";

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.of(() -> Lists.fixedSize.of(
                // Packageable Element
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        // TODO: ideally we should be able to set this as `connection`, but this will clash with `PackageableElement`
                        .withSubtype(Connection.class, "databaseConnection")
                        .build(),
                // Authentication
                ProtocolSubTypeInfo.newBuilder(AuthenticationConfiguration.class)
                        .withSubtype(EncryptedPrivateKeyPairAuthenticationConfiguration.class, "KeyPair")
                        .withSubtype(UserPasswordAuthenticationConfiguration.class, "UserPassword")
                        .withSubtype(KerberosAuthenticationConfiguration.class, "Kerberos")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(
                Connection.class, CONNECTION_CLASSIFIER_PATH
        );
    }
}
