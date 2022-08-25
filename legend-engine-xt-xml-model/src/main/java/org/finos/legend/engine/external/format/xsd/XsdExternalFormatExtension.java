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

package org.finos.legend.engine.external.format.xsd;

import org.finos.legend.engine.external.format.xsd.compile.XsdCompiler;
import org.finos.legend.engine.external.format.xsd.toModel.XsdToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.transformation.toModel.ExternalFormatModelGenerationExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_transformation_toPure_XsdToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatContract;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration;
import org.finos.legend.pure.generated.core_external_format_xml_externalFormatContract;

public class XsdExternalFormatExtension implements ExternalFormatModelGenerationExtension<Root_meta_external_format_xml_metamodel_xsd_XsdSchema, XsdToModelConfiguration>
{
    private static final Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_xml_metamodel_xsd_XsdSchema> xsdContract = (Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_xml_metamodel_xsd_XsdSchema>) core_external_format_xml_externalFormatContract.Root_meta_external_format_xml_contract_xsdFormatContract__ExternalFormatContract_1_(PureModel.CORE_PURE_MODEL.getExecutionSupport());
    public static final String TYPE = xsdContract._id();

    @Override
    public Root_meta_external_shared_format_ExternalFormatContract<Root_meta_external_format_xml_metamodel_xsd_XsdSchema> getExternalFormatContract()
    {
        return xsdContract;
    }

    @Override
    public Root_meta_external_format_xml_metamodel_xsd_XsdSchema compileSchema(ExternalSchemaCompileContext context)
    {
        return new XsdCompiler(context).compile();
    }

    @Override
    public String metamodelToText(Root_meta_external_format_xml_metamodel_xsd_XsdSchema schemaDetail, PureModel pureModel)
    {
        // TODO XSD Model to text
        return null;
    }

    @Override
    public Root_meta_external_shared_format_transformation_toPure_SchemaToModelConfiguration compileSchemaToModelConfiguration(XsdToModelConfiguration configuration, PureModel pureModel)
    {
        return new Root_meta_external_format_xml_transformation_toPure_XsdToModelConfiguration_Impl("", null, pureModel.getClass("meta::external::format::xml::transformation::toPure::XsdToModelConfiguration"))
                ._sourceSchemaId(configuration.sourceSchemaId)
                ._targetPackage(configuration.targetPackage)
                ._inlineCollectionClasses(configuration.inlineCollectionClasses)
                ._includeUnreachableClasses(configuration.includeUnreachableClasses);
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        // TODO XSD Correlation
        return null;
    }
}
