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

package org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtensionLoader;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaConfiguration;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.ConfigurationProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.schemaGeneration.SchemaGenerationSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_SchemaGenerationSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_SchemaGenerationSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit_Impl;

public class HelperSchemaGenerationElementBuilder
{

    private final Map<String, ExternalFormatExtension<?>> externalFormatExtensions;

    public HelperSchemaGenerationElementBuilder()
    {
        externalFormatExtensions = ExternalFormatExtensionLoader.extensions();
    }

    public Root_meta_pure_generation_metamodel_SchemaGenerationSpecification processSchemaGeneration(SchemaGenerationSpecification element, CompileContext context)
    {
        org.finos.legend.pure.m3.coreinstance.Package pack = context.pureModel.getOrCreatePackage(element._package);
        Root_meta_pure_generation_metamodel_SchemaGenerationSpecification metamodel =
            new Root_meta_pure_generation_metamodel_SchemaGenerationSpecification_Impl(element.name, null, context.pureModel.getClass("meta::pure::generation::metamodel::SchemaGenerationSpecification"))
                ._name(element.name)
                ._package(pack);

        ExternalFormatExtension<?> extension = getExtension(element.format, element.sourceInformation);
        if (!(extension instanceof ExternalFormatSchemaGenerationExtension))
        {
            throw new EngineException("External Extension format '" + element.name + " +' does not support schema generation", element.sourceInformation, EngineErrorType.COMPILATION);
        }

        ExternalFormatSchemaGenerationExtension schemaGenerationExtension = (ExternalFormatSchemaGenerationExtension) extension;
        Map<String, Object> configPropertiesMap = Maps.mutable.empty();
        if (element.config != null && !element.config.isEmpty())
        {
            element.config.forEach(e -> configPropertiesMap.put(e.name, e.value));
        }
        // if special conversion from config properties to protocol is needed, the extension will define it otherwise we will leverage reflection
        ModelToSchemaConfiguration protocolConfig = schemaGenerationExtension.rawConfigPropertiesToSchemaConfig(configPropertiesMap);

        if (protocolConfig == null)
        {
            Class<ModelToSchemaConfiguration> schemaConfigClass = Arrays.stream(schemaGenerationExtension.getClass().getGenericInterfaces()).filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast).filter(pt -> pt.getRawType().equals(ExternalFormatSchemaGenerationExtension.class)).findFirst().map(pt -> pt.getActualTypeArguments()[1])
                .map(Class.class::cast).orElseThrow(() -> new EngineException("Cannot obtain model generation configuration type"));
            try
            {
                ModelToSchemaConfiguration schemaConfigInstance = schemaConfigClass.getDeclaredConstructor().newInstance();
                schemaConfigInstance.format = element.format;
                if (element.config != null && !element.config.isEmpty())
                {
                    // duplicate check
                    Set<String> configPropertyNames = new HashSet<>();
                    element.config.forEach(configurationProperty ->
                    {
                        if (!configPropertyNames.add(configurationProperty.name))
                        {
                            throw new EngineException("Duplicated configuration property name: '" + configurationProperty.name + "'");
                        }
                    });
                    // build
                    element.config.forEach(e -> buildModelToSchemaConfig(element, schemaConfigInstance, e));
                }
                protocolConfig = schemaConfigInstance;
            }
            catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException noSuchMethodException)
            {
                throw new EngineException("Unable to build model to schema config class '" + schemaConfigClass.getName() + "' for format type '" + element.format + "'", element.sourceInformation, EngineErrorType.COMPILATION);
            }
        }

        protocolConfig.format = element.format;
        // compile config
        Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration schemaConfiguration = schemaGenerationExtension.compileModelToSchemaConfiguration(protocolConfig, context.pureModel);
        schemaConfiguration._targetSchemaSet("target::package::GeneratedSchemaSet");
        schemaConfiguration._format(element.format);

        // build model Unit
        Root_meta_pure_model_unit_ModelUnit modelUnit = new Root_meta_pure_model_unit_ModelUnit_Impl("", null, context.pureModel.getClass("meta::pure::model::unit::ModelUnit"))._classifierGenericType(
                new Root_meta_pure_metamodel_type_generics_GenericType_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::type::generics::GenericType"))._rawType(
                    context.pureModel.getType("meta::pure::model::unit::ModelUnit")))
            ._packageableElementIncludes(ListIterate.collect(element.modelUnit.packageableElementIncludes, pe -> context.pureModel.getPackageableElement(pe, element.sourceInformation)))
            ._packageableElementExcludes(ListIterate.collect(element.modelUnit.packageableElementExcludes, pe -> context.pureModel.getPackageableElement(pe, element.sourceInformation)));

        return metamodel._format(element.format)._modelToCodeConfiguration(schemaConfiguration)._modelUnit(modelUnit);
    }

    private void buildModelToSchemaConfig(SchemaGenerationSpecification schemaGenerationSpecification, ModelToSchemaConfiguration configuration, ConfigurationProperty configurationProperty)
    {
        Class<?> clazz = configuration.getClass();
        String paramName = configurationProperty.name;
        Object value = configurationProperty.value;
        Field field;
        try
        {
            Object fieldValue = value;
            field = clazz.getField(paramName);
            if (field.getType().isEnum())
            {
                fieldValue = Enum.valueOf((Class<Enum>) field.getType(), (String) fieldValue);
            }
            Assert.assertTrue(field.getType().isAssignableFrom(fieldValue.getClass()),
                () -> "Type '" + value.getClass().getSimpleName() + "' not assignable to type '" + field.getType().getSimpleName() + "' for config property '" + paramName + "'");
            field.setAccessible(true);
            field.set(configuration, fieldValue);
        }
        catch (Exception e)
        {
            String setMethodName = "set" + StringUtils.capitalize(paramName);
            try
            {
                Method m = clazz.getMethod(setMethodName, value.getClass());
                m.invoke(configuration, value);
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException error)
            {
                throw new EngineException("Configuration property '" + paramName + "' not found in schema generation type'" + schemaGenerationSpecification.format + "'");
            }
        }
    }

    private ExternalFormatExtension<?> getExtension(String name, SourceInformation sourceInformation)
    {
        return externalFormatExtensions.values().stream().filter(ext -> ext.getFormat().equals(name)).findFirst()
            .orElseThrow(() -> new EngineException("Unknown external format extension for schema generation '" + name + "'", sourceInformation, EngineErrorType.COMPILATION));
    }
}
