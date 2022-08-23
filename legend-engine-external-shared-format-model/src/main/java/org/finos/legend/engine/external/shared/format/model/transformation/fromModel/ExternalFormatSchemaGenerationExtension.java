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

package org.finos.legend.engine.external.shared.format.model.transformation.fromModel;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatFromPureDescriptor;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_generics_GenericType_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit;
import org.finos.legend.pure.generated.Root_meta_pure_model_unit_ModelUnit_Impl;
import org.finos.legend.pure.generated.core_pure_generation_generations;

/**
 * Defines an extension to be implemented to define a pure model to external format schema generation.
 * <p>
 * To implement a ExternalFormatSchemaGenerationExtension it is necessary to implement a ModelToSchemaConfiguration
 * that models configuration required for the translation.  This is a metamodel class expressed in PURE. It
 * is also necessary to define the translation from pure model to schema set (via externalFormatFromPureDescriptor)
 * in externalFormatContract modelled in pure.
 */
public interface ExternalFormatSchemaGenerationExtension<Metamodel extends Root_meta_external_shared_format_metamodel_SchemaDetail, SchemaGenConfig extends ModelToSchemaConfiguration> extends ExternalFormatExtension<Metamodel>
{
    /**
     * Called to compile an external format ModelToSchemaConfiguration.
     */
    Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(SchemaGenConfig configuration, PureModel pureModel);

    /**
     * Provides the properties used to configure model generation.
     */
    default RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getSchemaGenerationProperties(PureModel pureModel)
    {
        Root_meta_external_shared_format_ExternalFormatFromPureDescriptor<?> descriptor = getExternalFormatContract()._externalFormatFromPureDescriptor();
        if (descriptor == null)
        {
            throw new EngineException("Format - " + this.getFormat() + " does not support schema generation");
        }
        return core_pure_generation_generations.Root_meta_pure_generation_describeConfiguration_Class_1__Any_1__String_MANY__GenerationParameter_MANY_(descriptor._configuration(), descriptor._defaultConfig(), Lists.immutable.empty(), pureModel.getExecutionSupport());
    }

    /**
     * Called when a PURE model is used to generate an external schema of this format.
     */
    default Root_meta_external_shared_format_metamodel_SchemaSet generateSchema(SchemaGenConfig config, ModelUnit sourceModelUnit, PureModel pureModel)
    {
        Root_meta_external_shared_format_ExternalFormatFromPureDescriptor descriptor = getExternalFormatContract()._externalFormatFromPureDescriptor();
        if (descriptor == null)
        {
            throw new EngineException("Format - " + this.getFormat() + " does not support schema generation");
        }
        Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration configuration = compileModelToSchemaConfiguration(config, pureModel);

        Root_meta_pure_model_unit_ModelUnit modelUnit = new Root_meta_pure_model_unit_ModelUnit_Impl("")
                ._classifierGenericType(new Root_meta_pure_metamodel_type_generics_GenericType_Impl("")._rawType(pureModel.getType("meta::pure::model::unit::ModelUnit")))
                ._packageableElementIncludes(ListIterate.collect(sourceModelUnit.packageableElementIncludes, pureModel::getPackageableElement))
                ._packageableElementExcludes(ListIterate.collect(sourceModelUnit.packageableElementExcludes, pureModel::getPackageableElement));

        return descriptor.generate(modelUnit, configuration, pureModel.getExecutionSupport());
    }
}
