//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.functionActivator.api.output;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.domain.Class;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_protocols_pure_vX_X_X_metamodel_domain_Class;
import org.finos.legend.pure.generated.core_external_format_json_toJSON;
import org.finos.legend.pure.generated.core_pure_protocol_generation_serialization_scan;
import org.finos.legend.pure.generated.core_pure_protocol_vX_X_X_transfers_metamodel;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

public class ConfigurationInfo
{
    public String topElement;
    public String packageableElementJSONType;
    public List<Class> model;

    public ConfigurationInfo(String configurationElement, String packageableElementJSONType, PureModel pureModel)
    {
        this.topElement = configurationElement;
        this.model = buildModelFromElement(configurationElement, pureModel, "vX_X_X");
        this.packageableElementJSONType = packageableElementJSONType;
    }

    public ConfigurationInfo()
    {
    }

    private MutableList<Class> buildModelFromElement(String element, PureModel pureModel, String version)
    {
        PackageableElement elem = pureModel.getPackageableElement(element);
        RichIterable<? extends org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>> classes = core_pure_protocol_generation_serialization_scan.Root_meta_protocols_generation_scan_scanClass_Class_1__Class_MANY_((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) elem, pureModel.getExecutionSupport());
        return classes.collect(x ->
        {
            try
            {
                Root_meta_protocols_pure_vX_X_X_metamodel_domain_Class protocolClass = core_pure_protocol_vX_X_X_transfers_metamodel.Root_meta_protocols_pure_vX_X_X_transformation_fromPureGraph_domain_transformClass_Class_1__Extension_MANY__Class_1_(x, Lists.mutable.empty(), pureModel.getExecutionSupport());
                return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(serializeToJSON(protocolClass, pureModel), Class.class);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    private static String serializeToJSON(Object protocolPlan, PureModel pureModel)
    {
        return core_external_format_json_toJSON.Root_meta_json_toJSON_Any_MANY__Integer_$0_1$__Config_1__String_1_(
                org.eclipse.collections.api.factory.Lists.mutable.with(protocolPlan),
                1000L,
                core_external_format_json_toJSON.Root_meta_json_config_Boolean_1__Boolean_1__Boolean_1__Boolean_1__Config_1_(false, false, true, true, pureModel.getExecutionSupport()),
                pureModel.getExecutionSupport()
        );
    }
}
