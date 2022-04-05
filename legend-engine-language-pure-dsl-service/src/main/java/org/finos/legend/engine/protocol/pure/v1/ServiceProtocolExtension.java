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

package org.finos.legend.engine.protocol.pure.v1;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureMultiExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.PureSingleExecution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Service;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTestSuite;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest_Legacy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionTest;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestSuite;

import java.util.List;
import java.util.Map;

public class ServiceProtocolExtension implements PureProtocolExtension
{
    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.mutable.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.Builder
                        .newInstance(PackageableElement.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(Service.class, "service")
                        )).build(),
                ProtocolSubTypeInfo.Builder
                        .newInstance(Execution.class)
                        .withDefaultSubType(PureSingleExecution.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(PureSingleExecution.class, "pureSingleExecution"),
                                Tuples.pair(PureMultiExecution.class, "pureMultiExecution")
                        )).build(),
                ProtocolSubTypeInfo.Builder
                        .newInstance(ServiceTest_Legacy.class)
                        .withDefaultSubType(SingleExecutionTest.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(SingleExecutionTest.class, "singleExecutionTest"),
                                Tuples.pair(MultiExecutionTest.class, "multiExecutionTest")
                        )).build(),
                ProtocolSubTypeInfo.Builder
                        .newInstance(TestSuite.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(ServiceTestSuite.class, "serviceTestSuite")
                        )).build(),
                ProtocolSubTypeInfo.Builder
                        .newInstance(Test.class)
                        .withSubtypes(FastList.newListWith(
                                Tuples.pair(ServiceTest.class, "serviceTest")
                        )).build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(Service.class, "meta::legend::service::metamodel::Service");
    }
}
