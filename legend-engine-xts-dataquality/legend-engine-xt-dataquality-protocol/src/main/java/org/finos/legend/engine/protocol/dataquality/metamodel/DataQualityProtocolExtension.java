// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.dataquality.metamodel;

import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.extension.ProtocolSubTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.PropertyGraphFetchTree;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.RootGraphFetchTree;

import java.util.List;
import java.util.Map;

public class DataQualityProtocolExtension implements PureProtocolExtension
{
    public static String packageJSONType = "dataQualityValidation";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataQualityValidation");
    }

    @Override
    public List<Function0<List<ProtocolSubTypeInfo<?>>>> getExtraProtocolSubTypeInfoCollectors()
    {
        return Lists.fixedSize.with(() -> Lists.mutable.with(
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(DataQuality.class, packageJSONType)
                        .build(),
                ProtocolSubTypeInfo.newBuilder(RootGraphFetchTree.class)
                        .withSubtype(DataQualityRootGraphFetchTree.class, "dataQualityRootGraphFetchTree")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(PropertyGraphFetchTree.class)
                        .withSubtype(DataQualityPropertyGraphFetchTree.class, "dataQualityPropertyGraphFetchTree")
                        .build(),
                ProtocolSubTypeInfo.newBuilder(PackageableElement.class)
                        .withSubtype(DataqualityRelationValidation.class, "dataqualityRelationValidation")
                        .build()
        ));
    }

    @Override
    public Map<Class<? extends PackageableElement>, String> getExtraProtocolToClassifierPathMap()
    {
        return Maps.mutable.with(DataQuality.class, "meta::external::dataquality::DataQuality",
                                 DataqualityRelationValidation.class, "meta::external::dataquality::DataQualityRelationValidation");
    }
}
