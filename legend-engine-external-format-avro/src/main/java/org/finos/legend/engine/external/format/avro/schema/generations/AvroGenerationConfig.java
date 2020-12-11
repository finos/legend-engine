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

package org.finos.legend.engine.external.format.avro.schema.generations;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.generations.GenerationConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_avro_generation_AvroConfig;
import org.finos.legend.pure.generated.core_external_format_avro_integration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.pure.generated.core_pure_corefunctions_metaExtension.Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_;

public class AvroGenerationConfig extends GenerationConfiguration
{
    /**
     * Adds namespace derived from package to Avro schema. Default: true.
     */
    public Boolean includeNamespace;

    /**
     * Includes properties from super types. Default: true.
     */
    public Boolean includeSuperTypes;

    /**
     * Includes properties from associations. Default: true.
     */
    public Boolean includeAssociations;

    /**
     * Includes generated milestoning properties. Default: false.
     */
    public Boolean includeGeneratedMilestoning;

    /**
     * Timestamp logical type. Default is timestamp-micros. Possible values: timestamp-millis, timestamp-micros, or any other registered types.
     */
    public String timestampLogicalType;

    /**
     * Generates properties from specified profile tags
     */
    public List<String> propertyProfile = Collections.emptyList();

    /**
     * Override namespace in generated schema
     */
    public Map<String, String> namespaceOverride = UnifiedMap.newMap();

    public Root_meta_external_format_avro_generation_AvroConfig process(PureModel pureModel)
    {
        Root_meta_external_format_avro_generation_AvroConfig avroConfig = core_external_format_avro_integration.Root_meta_external_format_avro_generation_defaultConfig__AvroConfig_1_(pureModel.getExecutionSupport());
        List<PackageableElement> scopeElements = ListIterate.collect(this.generationScope(), e -> Root_meta_pure_functions_meta_pathToElement_String_1__PackageableElement_1_(e, pureModel.getExecutionSupport()));
        avroConfig._scopeElements((RichIterable<? extends PackageableElement>) scopeElements);
        if (includeNamespace != null)
        {
            avroConfig._includeNamespace(includeNamespace);
        }
        if (includeSuperTypes != null)
        {
            avroConfig._includeSuperTypes(includeSuperTypes);
        }

        if (includeAssociations != null)
        {
            avroConfig._includeAssociations(includeAssociations);
        }

        if (includeGeneratedMilestoning != null)
        {
            avroConfig._includeGeneratedMilestoning(includeGeneratedMilestoning);
        }

        if (timestampLogicalType != null)
        {
            avroConfig._timestampLogicalType(timestampLogicalType);
        }
        if (!propertyProfile.isEmpty())
        {
            avroConfig._propertyProfile(ListIterate.collect(propertyProfile, pureModel::getProfile));
        }
        PureMap _namespaceOverride = null;
        if (this.namespaceOverride != null)
        {
            UnifiedMap<String, String> modifiedQualifiers = UnifiedMap.newMap();
            this.namespaceOverride.forEach(modifiedQualifiers::put);
            _namespaceOverride = new PureMap(modifiedQualifiers);
        }
        avroConfig._namespaceOverride(_namespaceOverride);
        return avroConfig;
    }
}
