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

import org.finos.legend.engine.external.format.protobuf.fromModel.ModelToProtobufConfiguration;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.language.protobuf3.grammar.from.Protobuf3GrammarParser;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.protobuf3.metamodel.ProtoFile;
import org.finos.legend.engine.protocol.protobuf3.metamodel.Translator;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_metamodel_ProtoFile;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_metamodel_ProtobufSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_metamodel_ProtobufSchema_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_transformation_fromPure_ModelToProtobufDataConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.core_external_format_protobuf_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_protobuf_metamodel_metamodel_serialization;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

public class ProtobufFormatExtension implements ExternalFormatSchemaGenerationExtension<Root_meta_external_format_protobuf_metamodel_ProtobufSchema, ModelToProtobufConfiguration>
{
    private static final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_protobuf_metamodel_ProtobufSchema> protobufContract = (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_protobuf_metamodel_ProtobufSchema>) core_external_format_protobuf_externalFormatContract.Root_meta_external_format_protobuf_contract_protobufFormatContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());
    public static final String TYPE = protobufContract._id();

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_protobuf_metamodel_ProtobufSchema> getExternalFormatContract()
    {
        return protobufContract;
    }

    @Override
    public Root_meta_external_format_protobuf_metamodel_ProtobufSchema compileSchema(ExternalSchemaCompileContext context)
    {
        ProtoFile protoFile = Protobuf3GrammarParser.newInstance().parseProto(context.getContent());
        Root_meta_external_format_protobuf_metamodel_ProtoFile pureProtoFile = new Translator().translate(protoFile, context.getPureModel());

        Root_meta_external_format_protobuf_metamodel_ProtobufSchema protobufSchema = new Root_meta_external_format_protobuf_metamodel_ProtobufSchema_Impl("", null, context.getPureModel().getClass("meta::external::format::protobuf::metamodel::ProtobufSchema"));
        protobufSchema._file(pureProtoFile);
        protobufSchema._fileName(context.getLocation());

        return protobufSchema;
    }

    @Override
    public String metamodelToText(Root_meta_external_format_protobuf_metamodel_ProtobufSchema schemaDetail, PureModel pureModel)
    {
        return core_external_format_protobuf_metamodel_metamodel_serialization.Root_meta_external_format_protobuf_metamodel_serialization_toString_ProtoFile_1__String_1_(schemaDetail._file(), pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(ModelToProtobufConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_protobuf_transformation_fromPure_ModelToProtobufDataConfiguration_Impl("", null, pureModel.getClass("meta::external::format::protobuf::transformation::fromPure::ModelToProtobufDataConfiguration"))
                ._targetSchemaSet(configuration.targetSchemaSet)
                ._javaPackage(configuration.javaPackage)
                ._javaOuterClassname(configuration.javaOuterClassname)
                ._javaMultipleFiles(configuration.javaMultipleFiles)
                ._optimizeFor(configuration.optimizeFor == null ? null : pureModel.getEnumValue("meta::external::format::protobuf::transformation::fromPure::OptimizeMode", configuration.optimizeFor.name()))
                ._customOptions(new PureMap(configuration.customOptions));
    }
}
