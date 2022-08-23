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

package org.finos.legend.engine.external.shared.format.model.exampleSchema;

import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.ExternalFormatModelGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_tests_ExampleSchema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_tests_ExampleSchemaToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_tests_ExampleSchema_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_tests_ModelToExampleSchemaConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.core_pure_binding_transformation_tests_externalFormatContract;

public class ExampleExternalFormatExtension implements ExternalFormatSchemaGenerationExtension<Root_meta_external_shared_format_transformation_tests_ExampleSchema, ModelToExampleSchemaConfiguration>, ExternalFormatModelGenerationExtension<Root_meta_external_shared_format_transformation_tests_ExampleSchema, ExampleSchemaToModelConfiguration>
{
    private static final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_shared_format_transformation_tests_ExampleSchema> externalFormatContract = (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_shared_format_transformation_tests_ExampleSchema>) core_pure_binding_transformation_tests_externalFormatContract.Root_meta_external_shared_format_transformation_tests_exampleFormatContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());

    public static final String TYPE = externalFormatContract._id();

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_shared_format_transformation_tests_ExampleSchema> getExternalFormatContract()
    {
        return externalFormatContract;
    }

    @Override
    public Root_meta_external_shared_format_transformation_tests_ExampleSchema compileSchema(ExternalSchemaCompileContext context)
    {
        return new Root_meta_external_shared_format_transformation_tests_ExampleSchema_Impl("", null, context.getPureModel().getClass("meta::external::shared::format::transformation::tests::ExampleSchema"));
    }

    @Override
    public String metamodelToText(Root_meta_external_shared_format_transformation_tests_ExampleSchema schemaDetail, PureModel pureModel)
    {
        return "";
    }

    @Override
    public Root_meta_external_shared_format_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(ModelToExampleSchemaConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_shared_format_transformation_tests_ModelToExampleSchemaConfiguration_Impl("", null, pureModel.getClass("meta::external::shared::format::transformation::tests::ModelToExampleSchemaConfiguration"));
    }

    @Override
    public Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(ExampleSchemaToModelConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_shared_format_transformation_tests_ExampleSchemaToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::shared::format::transformation::tests::ExampleSchemaToModelConfiguration"));
    }
}
