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

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.xml.XmlExternalFormatPureExtension;
import org.finos.legend.engine.external.format.xsd.compile.XsdCompiler;
import org.finos.legend.engine.external.format.xsd.compile.XsdParser;
import org.finos.legend.engine.external.format.xsd.compile.parseTree.XsdSchema;
import org.finos.legend.engine.external.format.xsd.fromModel.ModelToXsdConfiguration;
import org.finos.legend.engine.external.format.xsd.toModel.XsdToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_validation_BindingDetail;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_Schema;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_metamodel_xsd_XsdSchema;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_binding_toPure_XsdToModelConfiguration;
import org.finos.legend.pure.generated.Root_meta_external_format_xml_binding_toPure_XsdToModelConfiguration_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_metamodel_SchemaSet;
import org.finos.legend.pure.generated.core_external_format_xml_binding_xsdToPure;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class XsdExternalFormatExtension implements ExternalFormatExtension<Root_meta_external_format_xml_metamodel_xsd_XsdSchema, XsdToModelConfiguration, ModelToXsdConfiguration>
{
    private static final String TYPE = "XSD";
    private static final boolean DEBUG_MODEL_GEN = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains(":jdwp");

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(XmlExternalFormatPureExtension.CONTENT_TYPE);
    }

    @Override
    public Root_meta_external_format_xml_metamodel_xsd_XsdSchema compileSchema(ExternalSchemaCompileContext context)
    {
        return new XsdCompiler(context).compile();
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        // TODO XSD Correlation
        return null;
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, XsdToModelConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_xml_binding_toPure_XsdToModelConfiguration configuration = new Root_meta_external_format_xml_binding_toPure_XsdToModelConfiguration_Impl("")
                ._sourceSchemaId(config.sourceSchemaId)
                ._targetPackage(config.targetPackage)
                ._targetBinding(config.targetBinding)
                ._inlineCollectionClasses(config.inlineCollectionClasses)
                ._includeUnreachableClasses(config.includeUnreachableClasses);
        return DEBUG_MODEL_GEN
            ? core_external_format_xml_binding_xsdToPure.Root_meta_external_format_xml_binding_toPure_xsdToPureWithDebug_SchemaSet_1__XsdToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport())
            : core_external_format_xml_binding_xsdToPure.Root_meta_external_format_xml_binding_toPure_xsdToPure_SchemaSet_1__XsdToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToXsdConfiguration modelToXsdConfiguration, PureModel pureModel)
    {
        // TODO XSD from model
        return null;
    }

    @Override
    public String metamodelToText(Root_meta_external_format_xml_metamodel_xsd_XsdSchema schemaDetail)
    {
        // TODO XSD Model to text
        return null;
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        List<String> versions = Arrays.asList("vX_X_X", "v1_21_0");
        return ListIterate.collect(versions, v -> "meta::protocols::pure::"  + v + "::external::format::xml::serializerExtension_String_1__SerializerExtension_1_");
    }
}
