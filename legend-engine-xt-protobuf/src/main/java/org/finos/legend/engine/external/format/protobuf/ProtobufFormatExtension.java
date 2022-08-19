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

package org.finos.legend.engine.external.format.protobuf;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.protobuf.fromModel.ModelToProtobufConfiguration;
import org.finos.legend.engine.external.format.protobuf.toModel.ProtobufToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_binding_ProtobufSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_binding_fromPure_ModelToProtobufDataConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_binding_fromPure_ModelToProtobufDataConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationParameter;
import org.finos.legend.pure.generated.core_external_format_protobuf_metamodel_metamodel_serialization;
import org.finos.legend.pure.generated.core_external_format_protobuf_transformation_transformation;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.List;

public class ProtobufFormatExtension implements ExternalFormatExtension<Root_meta_external_format_protobuf_binding_ProtobufSchema, ProtobufToModelConfiguration, ModelToProtobufConfiguration>
{
    public static final String TYPE = "Protobuf";

    //There is no standard content type for protobuf yet
    private static final String CONTENT_TYPE_1 = "application/protobuf";
    private static final String CONTENT_TYPE_2 = " application/vnd.google.protobuf";

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Lists.fixedSize.of(CONTENT_TYPE_1, CONTENT_TYPE_2);
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return false;
    }

    @Override
    public Root_meta_external_format_protobuf_binding_ProtobufSchema compileSchema(ExternalSchemaCompileContext context)
    {
        throw new RuntimeException("TODO");

    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_format_protobuf_binding_ProtobufSchema schemaDetail, PureModel pureModel)
    {
        return core_external_format_protobuf_metamodel_metamodel_serialization.Root_meta_external_format_protobuf_metamodel_serialization_toString_ProtoFile_1__String_1_(schemaDetail._file(), pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schema, ProtobufToModelConfiguration protobufToModelConfiguration, PureModel pureModel)
    {
        return null;
    }

    @Override
    public boolean supportsSchemaGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getSchemaGenerationProperties(PureModel pureModel)
    {
        return core_external_format_protobuf_transformation_transformation.Root_meta_external_format_protobuf_binding_fromPure_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToProtobufConfiguration modelToSchemaConfiguration, PureModel pureModel)
    {
        Root_meta_external_format_protobuf_binding_fromPure_ModelToProtobufDataConfiguration configuration = new Root_meta_external_format_protobuf_binding_fromPure_ModelToProtobufDataConfiguration_Impl("", null, pureModel.getClass("meta::external::format::protobuf::binding::fromPure::ModelToProtobufDataConfiguration"))
                ._sourceModel(ListIterate.collect(modelToSchemaConfiguration.sourceModel, pureModel::getClass))
                ._targetBinding(modelToSchemaConfiguration.targetBinding)
                ._targetSchemaSet(modelToSchemaConfiguration.targetSchemaSet)
                ._javaPackage(modelToSchemaConfiguration.javaPackage)
                ._javaOuterClassname(modelToSchemaConfiguration.javaOuterClassname)
                ._javaMultipleFiles(modelToSchemaConfiguration.javaMultipleFiles)
                ._optimizeFor(modelToSchemaConfiguration.optimizeFor == null ? null : pureModel.getEnumValue("meta::external::format::protobuf::binding::fromPure::OptimizeMode", modelToSchemaConfiguration.optimizeFor.name()))
                ._customOptions(new PureMap(modelToSchemaConfiguration.customOptions));

        modelToSchemaConfiguration.sourceModel.forEach(pe -> configuration._sourceModelAdd(pureModel.getPackageableElement(pe)));

        return core_external_format_protobuf_transformation_transformation.Root_meta_external_format_protobuf_binding_fromPure_pureToProtobuf_ModelToProtobufDataConfiguration_1__Binding_1_(configuration, pureModel.getExecutionSupport());
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        return Lists.mutable.empty();
    }
}
