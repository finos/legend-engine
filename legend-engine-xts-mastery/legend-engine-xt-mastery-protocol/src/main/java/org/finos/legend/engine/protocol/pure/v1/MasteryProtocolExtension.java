// Copyright 2022 Goldman Sachs
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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.DESDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.Decryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.FileAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.KafkaAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.LegendServiceAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.PGPDecryption;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.RestAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.NTLMAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.TokenAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.FTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.HTTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.KafkaConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.dataProvider.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;

import java.util.List;
import java.util.Map;

public class MasteryProtocolExtension implements PureProtocolExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mastery");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.of(() -> Lists.fixedSize.of(

                // Packageable element
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(MasterRecordDefinition.class, "mastery")
                        .withSubtype(DataProvider.class, "dataProvider")
                        .withSubtype(Connection.class, "masteryConnection")
                        .build(),


                // Acquisition protocol
                ProtocolSubTypeInfo.newBuilder(AcquisitionProtocol.class)
                        .withSubtype(RestAcquisitionProtocol.class, "restAcquisitionProtocol")
                        .withSubtype(FileAcquisitionProtocol.class, "fileAcquisitionProtocol")
                        .withSubtype(KafkaAcquisitionProtocol.class, "kafkaAcquisitionProtocol")
                        .withSubtype(LegendServiceAcquisitionProtocol.class, "legendServiceAcquisitionProtocol")
                        .build(),

                // Decryption
                ProtocolSubTypeInfo.newBuilder(Decryption.class)
                        .withSubtype(PGPDecryption.class, "pgpDecryption")
                        .withSubtype(DESDecryption.class, "desDecryption")
                        .build(),

                // Trigger
                ProtocolSubTypeInfo.newBuilder(Trigger.class)
                        .withSubtype(ManualTrigger.class, "manualTrigger")
                        .withSubtype(CronTrigger.class, "cronTrigger")
                        .build(),

                // Authentication strategy
                ProtocolSubTypeInfo.newBuilder(AuthenticationStrategy.class)
                        .withSubtype(TokenAuthenticationStrategy.class, "tokenAuthenticationStrategy")
                        .withSubtype(NTLMAuthenticationStrategy.class, "ntlmAuthenticationStrategy")
                        .build(),

                // Connection
                ProtocolSubTypeInfo.newBuilder(Connection.class)
                        .withSubtype(FTPConnection.class, "ftpConnection")
                        .withSubtype(HTTPConnection.class, "httpConnection")
                        .withSubtype(KafkaConnection.class, "kafkaConnection")
                        .build()
                ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        MutableMap<Class<? extends PackageableElement>, String> classfierMap = Maps.mutable.with(
                MasterRecordDefinition.class, "meta::pure::mastery::metamodel::MasterRecordDefinition",
                DataProvider.class, "meta::pure::mastery::metamodel::DataProvider",
                FTPConnection.class, "meta::pure::mastery::metamodel::connection::FTPConnection",
                HTTPConnection.class, "meta::pure::mastery::metamodel::connection::HTTPConnection");
        classfierMap.put(KafkaConnection.class, "meta::pure::mastery::metamodel::connection::KafkaConnection");
        return classfierMap;
    }
}
