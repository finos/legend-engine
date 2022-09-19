//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.shared.format.model.transformation.toModel;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatToPureDescriptor;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;
import org.finos.legend.pure.generated.core_pure_generation_generations;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

import java.util.List;

/**
 * Defines an extension to be implemented to define an external format to pure model generation.
 * <p>
 * To implement a ExternalFormatModelGenerationExtension it is necessary to implement a SchemaToModelConfiguration
 * that models configuration required for the translation.  This is a metamodel class expressed in PURE. It
 * is also necessary to define the translation from schema set to pure model (via externalFormatToPureDescriptor)
 * in externalFormatContract modelled in pure.
 */
public interface ExternalFormatModelGenerationExtension<Metamodel extends Root_meta_external_shared_format_metamodel_SchemaDetail, ModelGenConfig extends SchemaToModelConfiguration> extends ExternalFormatExtension<Metamodel>
{
    /**
     * Called to compile an external format SchemaToModelConfiguration.
     */
    Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(ModelGenConfig configuration, PureModel pureModel);

    /**
     * Provides the properties used to configure model generation.
     */
    default RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getModelGenerationProperties(PureModel pureModel)
    {
        Root_meta_external_shared_format_ExternalFormatToPureDescriptor<?> descriptor = getExternalFormatContract()._externalFormatToPureDescriptor();
        if (descriptor == null)
        {
            throw new EngineException("Format - " + this.getFormat() + " does not support model generation");
        }
        return core_pure_generation_generations.Root_meta_pure_generation_describeConfiguration_Class_1__Any_1__String_MANY__GenerationParameter_MANY_(descriptor._configuration(), descriptor._defaultConfig(), Lists.immutable.empty(), pureModel.getExecutionSupport());
    }

    /**
     * Called when an external schema of this format is used to generate a PURE model.
     */
    default List<? extends PackageableElement> generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, ModelGenConfig config, PureModel pureModel)
    {
        Root_meta_external_shared_format_ExternalFormatToPureDescriptor descriptor = getExternalFormatContract()._externalFormatToPureDescriptor();
        if (descriptor == null)
        {
            throw new EngineException("Format - " + this.getFormat() + " does not support model generation");
        }
        Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration configuration = compileSchemaToModelConfiguration(config, pureModel);
        return descriptor.generate(schemaSet, configuration, pureModel.getExecutionSupport()).toList();
    }
}
