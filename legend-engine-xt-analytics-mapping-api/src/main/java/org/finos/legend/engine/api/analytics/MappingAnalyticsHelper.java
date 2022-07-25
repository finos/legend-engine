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

package org.finos.legend.engine.api.analytics;

import org.finos.legend.engine.api.analytics.model.EntityMappedProperty;
import org.finos.legend.engine.api.analytics.model.EnumMappedProperty;
import org.finos.legend.engine.api.analytics.model.MappedEntity;
import org.finos.legend.engine.api.analytics.model.MappedEntityInfo;
import org.finos.legend.engine.api.analytics.model.MappedProperty;
import org.finos.legend.engine.api.analytics.model.MappedPropertyInfo;
import org.finos.legend.engine.api.analytics.model.MappedPropertyType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappedEntity;
import org.finos.legend.pure.generated.Root_meta_analytics_mapping_modelCoverage_MappedProperty;
import org.finos.legend.pure.generated.platform_pure_corefunctions_multiplicity;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;

import java.util.List;

public class MappingAnalyticsHelper
{
    public static MappedEntity buildMappedEntity(Root_meta_analytics_mapping_modelCoverage_MappedEntity mappedEntity, boolean returnMappedEntityInfo, boolean returnMappedPropertyInfo, CompiledExecutionSupport es)
    {
        List<MappedProperty> mappedProperties = mappedEntity._properties().collect(p -> buildMappedProperty(p, returnMappedPropertyInfo, es)).toList();
        MappedEntity result = new MappedEntity(mappedEntity._path(), mappedProperties);
        if (returnMappedEntityInfo)
        {
            if (mappedEntity._info() != null)
            {
                if (mappedEntity._info()._subClasses() != null)
                {
                    List<String> subClasses = mappedEntity._info()._subClasses().collect((String p) -> p).toList();
                    if (mappedEntity._info()._isRootEntity() != null)
                    {
                        result.info = new MappedEntityInfo(mappedEntity._info()._isRootEntity(), subClasses);
                    }
                    else
                    {
                        result.info = new MappedEntityInfo(subClasses);
                    }
                }
                else if (mappedEntity._info()._isRootEntity() != null)
                {
                    result.info = new MappedEntityInfo(mappedEntity._info()._isRootEntity());
                }
            }
        }
        return result;
    }

    private static MappedProperty buildMappedProperty(Root_meta_analytics_mapping_modelCoverage_MappedProperty mappedProperty, boolean returnMappedPropertyInfo, CompiledExecutionSupport es)
    {
        MappedProperty result;
        if (mappedProperty instanceof Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty)
        {
            result = new EntityMappedProperty(mappedProperty._name(), ((Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty) mappedProperty)._entityPath(), ((Root_meta_analytics_mapping_modelCoverage_EntityMappedProperty) mappedProperty)._subType());
        }
        else if (mappedProperty instanceof Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty)
        {
            result = new EnumMappedProperty(mappedProperty._name(), ((Root_meta_analytics_mapping_modelCoverage_EnumMappedProperty) mappedProperty)._enumPath());
        }
        else
        {
            result = new MappedProperty(mappedProperty._name());
        }
        if (returnMappedPropertyInfo && mappedProperty._info() != null)
        {
            if (mappedProperty._info()._multiplicity() != null)
            {
                Multiplicity multiplicity = new Multiplicity(
                        (int) platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_getLowerBound_Multiplicity_1__Integer_1_(
                                mappedProperty._info()._multiplicity().getFirst(), es),
                        platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_hasUpperBound_Multiplicity_1__Boolean_1_(
                                mappedProperty._info()._multiplicity().getFirst(), es) ?
                                (int) platform_pure_corefunctions_multiplicity.Root_meta_pure_functions_multiplicity_getUpperBound_Multiplicity_1__Integer_1_(
                                        mappedProperty._info()._multiplicity().getFirst(), es) : Integer.MAX_VALUE);
                if (mappedProperty._info()._type() != null)
                {
                    result.mappedPropertyInfo = new MappedPropertyInfo(multiplicity, MappedPropertyType.valueOf(mappedProperty._info()._type().getFirst()._name()));
                }
                else
                {
                    result.mappedPropertyInfo = new MappedPropertyInfo(multiplicity);
                }
            }
            else if (mappedProperty._info()._type() != null)
            {
                result.mappedPropertyInfo = new MappedPropertyInfo(MappedPropertyType.valueOf(mappedProperty._info()._type().getFirst()._name()));
            }
        }
        return result;
    }
}
