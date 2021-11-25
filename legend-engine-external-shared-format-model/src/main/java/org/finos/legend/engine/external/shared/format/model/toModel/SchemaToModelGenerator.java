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

package org.finos.legend.engine.external.shared.format.model.toModel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.format.model.Generator;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperExternalFormat;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.Protocol;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ResolvedModelUnit;
import org.finos.legend.pure.generated.core_pure_model_modelUnit;
import org.finos.legend.pure.generated.core_pure_protocol_protocol;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.Objects;

public class SchemaToModelGenerator extends Generator
{
    private final String pureVersion;
    private final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final Map<String, ExternalFormatExtension> schemaExtensions = ExternalFormatExtensionLoader.extensions();
    private final Method classTransform;
    private final Method associationTransform;
    private final Method enumTransform;

    public SchemaToModelGenerator(PureModel pureModel, String pureVersion)
    {
        super(pureModel);
        this.pureVersion = pureVersion;

        try
        {
            Class<?> cls = Class.forName("org.finos.legend.pure.generated.core_pure_protocol_" + pureVersion + "_transfers_metamodel");
            this.classTransform = cls.getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformClass_Class_1__RouterExtension_MANY__Class_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            this.associationTransform = cls.getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformAssociation_Association_1__RouterExtension_MANY__Association_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association.class, RichIterable.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
            this.enumTransform = cls.getMethod("Root_meta_protocols_pure_" + pureVersion + "_transformation_fromPureGraph_domain_transformEnum_Enumeration_1__Enumeration_1_", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration.class, org.finos.legend.pure.m3.execution.ExecutionSupport.class);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public PureModelContextData generate(SchemaToModelConfiguration configuration)
    {
        Root_meta_external_shared_format_metamodel_SchemaSet schemaSet = HelperExternalFormat.getSchemaSet(configuration.sourceSchemaSet, pureModel.getContext());

        ExternalFormatExtension schemaExtension = schemaExtensions.get(schemaSet._format());
        if (schemaExtension == null)
        {
            throw new IllegalArgumentException("Unknown schema format: " + schemaSet._format());
        }

        Class<?> configClass = (Class<?>) ((ParameterizedType)schemaExtension.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
        if (!configClass.isInstance(configuration))
        {
            throw new IllegalArgumentException("Invalid configuration for " + schemaSet._format() + " model generation: " + configuration.getClass().getSimpleName());
        }

        return toPureModelContextData(schemaExtension.generateModel(schemaSet, configuration, pureModel));
    }

    private PureModelContextData toPureModelContextData(Root_meta_external_shared_format_binding_Binding binding)
    {
        PureModelContextData.Builder builder = PureModelContextData.newBuilder().withSerializer(new Protocol("pure", pureVersion));
        Root_meta_pure_model_unit_ResolvedModelUnit resolved = core_pure_model_modelUnit.Root_meta_pure_model_unit_resolve_ModelUnit_1__ResolvedModelUnit_1_(binding._modelUnit(), pureModel.getExecutionSupport());
        LazyIterate.collect(resolved._packageableElements(), e ->
        {
            if (e instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
            {
                return transformClass((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) e);
            }
            else if (e instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association)
            {
                return transformAssociation((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association) e);
            }
            else if (e instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration)
            {
                return transformEnumeration((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?>) e);
            }
            else
            {
                return null;
            }
        }).select(Objects::nonNull).forEach(builder::addElement);
        builder.addElement(transformBinding(binding));
        return builder.build();
    }

    private org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class transformClass(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cls)
    {
        try
        {
            Object result = classTransform.invoke(null, cls, Lists.mutable.empty(), pureModel.getExecutionSupport());
            String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(result, pureModel.getExecutionSupport());
            return objectMapper.readValue(json, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Class.class);
        }
        catch (IOException | ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association transformAssociation(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association assoc)
    {
        try
        {
            Object result = associationTransform.invoke(null, assoc, Lists.mutable.empty(), pureModel.getExecutionSupport());
            String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(result, pureModel.getExecutionSupport());
            return objectMapper.readValue(json, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Association.class);
        }
        catch (IOException | ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration transformEnumeration(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration<?> enumeration)
    {
        try
        {
            Object result = enumTransform.invoke(null, enumeration, pureModel.getExecutionSupport());
            String json = core_pure_protocol_protocol.Root_meta_alloy_metadataServer_alloyToJSON_Any_1__String_1_(result, pureModel.getExecutionSupport());
            return objectMapper.readValue(json, org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Enumeration.class);
        }
        catch (IOException | ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }
}

