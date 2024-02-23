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

package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.external.format.flatdata.grammar.fromPure.FlatDataSchemaParseException;
import org.finos.legend.engine.external.format.flatdata.grammar.fromPure.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.grammar.toPure.FlatDataSchemaComposer;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.MetamodelToProtocolTranslator;
import org.finos.legend.engine.external.format.flatdata.metamodel.ProtocolToMetamodelTranslator;
import org.finos.legend.engine.external.format.flatdata.transformation.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.transformation.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.format.flatdata.validation.FlatDataValidation;
import org.finos.legend.engine.external.format.flatdata.validation.FlatDataValidationResult;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ExternalFormatSchemaGenerationExtension;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.ExternalFormatModelGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatData;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_transformation_fromPure_ModelToFlatDataConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_transformation_toPure_FlatDataToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_transformation_fromPure_ModelToSchemaConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_format_shared_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;

import java.util.stream.Collectors;

public class FlatDataExternalFormatExtension implements ExternalFormatSchemaGenerationExtension<Root_meta_external_format_flatdata_metamodel_FlatData, ModelToFlatDataConfiguration>, ExternalFormatModelGenerationExtension<Root_meta_external_format_flatdata_metamodel_FlatData, FlatDataToModelConfiguration>
{
    private static final Root_meta_external_format_shared_ExternalFormatContract<Root_meta_external_format_flatdata_metamodel_FlatData> flatDataContract = (Root_meta_external_format_shared_ExternalFormatContract<Root_meta_external_format_flatdata_metamodel_FlatData>) core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_contract_flatDataFormatContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());
    public static final String TYPE = flatDataContract._id();

    @Override
    public String type()
    {
        return "MIX_Model_Generation_&_Schema_Generation";
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "FlatData");
    }

    @Override
    public Root_meta_external_format_shared_ExternalFormatContract<Root_meta_external_format_flatdata_metamodel_FlatData> getExternalFormatContract()
    {
        return flatDataContract;
    }

    @Override
    public Root_meta_external_format_flatdata_metamodel_FlatData compileSchema(ExternalSchemaCompileContext context)
    {
        try
        {
            FlatData flatData = new FlatDataSchemaParser(context.getContent()).parse();
            FlatDataValidationResult validationResult = FlatDataValidation.validate(flatData);
            if (!validationResult.isValid())
            {
                String message = validationResult.getDefects().stream().map(Object::toString).collect(Collectors.joining(", "));
                throw new ExternalFormatSchemaException(message);
            }
            return new ProtocolToMetamodelTranslator().translate(flatData, context.getPureModel());
        }
        catch (FlatDataSchemaParseException e)
        {
            throw new ExternalFormatSchemaException(e.getMessage(), e.getStartLine(), e.getStartColumn(), e.getEndLine(), e.getEndColumn());
        }
    }

    @Override
    public String metamodelToText(Root_meta_external_format_flatdata_metamodel_FlatData schemaDetail, PureModel pureModel)
    {
        return FlatDataSchemaComposer.toGrammar(new MetamodelToProtocolTranslator().translate(schemaDetail));
    }

    @Override
    public Root_meta_external_format_shared_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(FlatDataToModelConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_flatdata_transformation_toPure_FlatDataToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::format::flatdata::transformation::toPure::FlatDataToModelConfiguration"))
                ._targetPackage(configuration.targetPackage)
                ._purifyNames(configuration.purifyNames)
                ._schemaClassName(configuration.schemaClassName);
    }

    @Override
    public Root_meta_external_format_shared_transformation_fromPure_ModelToSchemaConfiguration compileModelToSchemaConfiguration(ModelToFlatDataConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_flatdata_transformation_fromPure_ModelToFlatDataConfiguration_Impl("", null, pureModel.getClass("meta::external::format::flatdata::transformation::fromPure::ModelToFlatDataConfiguration"))
                ._targetSchemaSet(configuration.targetSchemaSet);
    }
}
