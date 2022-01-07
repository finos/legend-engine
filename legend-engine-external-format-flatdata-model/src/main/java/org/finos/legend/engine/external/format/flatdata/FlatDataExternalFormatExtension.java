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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.external.format.flatdata.compile.FlatDataSchemaCompiler;
import org.finos.legend.engine.external.format.flatdata.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataGrammarHelper;
import org.finos.legend.engine.external.format.flatdata.shared.model.*;
import org.finos.legend.engine.external.format.flatdata.toModel.FlatDataToModelConfiguration;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatExtension;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.pure.generated.*;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

public class FlatDataExternalFormatExtension implements ExternalFormatExtension<Root_meta_external_format_flatdata_metamodel_FlatData, FlatDataToModelConfiguration, ModelToFlatDataConfiguration>
{
    private static final String TYPE = "FlatData";
    private static final boolean IN_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().contains(":jdwp");

    @Override
    public String getFormat()
    {
        return TYPE;
    }

    @Override
    public List<String> getContentTypes()
    {
        return Collections.singletonList(FlatDataExternalFormatPureExtension.CONTENT_TYPE);
    }

    @Override
    public Root_meta_external_format_flatdata_metamodel_FlatData compileSchema(ExternalSchemaCompileContext context)
    {
        return new FlatDataSchemaCompiler(context).compile();
    }

    @Override
    public Root_meta_external_shared_format_binding_validation_BindingDetail bindDetails(Root_meta_external_shared_format_binding_Binding binding, CompileContext context)
    {
        return core_external_format_flatdata_binding_validation.Root_meta_external_format_flatdata_binding_validation_bindDetails_Binding_1__BindingDetail_1_(binding, context.getExecutionSupport());
    }

    @Override
    public String metamodelToText(Root_meta_external_format_flatdata_metamodel_FlatData schemaDetail)
    {
        Root_meta_external_format_flatdata_metamodel_FlatData flatData = schemaDetail;
        return FlatDataGrammarHelper.toGrammar(transformFlatData(flatData));
    }

    @Override
    public boolean supportsModelGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getModelGenerationProperties(PureModel pureModel)
    {
        return core_external_format_flatdata_binding_flatDataToPure.Root_meta_external_format_flatdata_binding_toPure_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateModel(Root_meta_external_shared_format_metamodel_SchemaSet schemaSet, FlatDataToModelConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_flatdata_binding_toPure_FlatDataToModelConfiguration configuration = new Root_meta_external_format_flatdata_binding_toPure_FlatDataToModelConfiguration_Impl("")
                ._targetBinding(config.targetBinding)
                ._targetPackage(config.targetPackage)
                ._purifyNames(config.purifyNames)
                ._schemaClassName(config.schemaClassName);
        return IN_DEBUG
               ? core_external_format_flatdata_binding_flatDataToPure.Root_meta_external_format_flatdata_binding_toPure_flatDataToPureWithDebug_SchemaSet_1__FlatDataToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport())
               : core_external_format_flatdata_binding_flatDataToPure.Root_meta_external_format_flatdata_binding_toPure_flatDataToPure_SchemaSet_1__FlatDataToModelConfiguration_1__Binding_1_(schemaSet, configuration, pureModel.getExecutionSupport());
    }

    @Override
    public boolean supportsSchemaGeneration()
    {
        return true;
    }

    @Override
    public RichIterable<? extends Root_meta_pure_generation_metamodel_GenerationParameter> getSchemaGenerationProperties(PureModel pureModel)
    {
        return core_external_format_flatdata_binding_pureToFlatData.Root_meta_external_format_flatdata_binding_fromPure_describeConfiguration__GenerationParameter_MANY_(pureModel.getExecutionSupport());
    }

    @Override
    public Root_meta_external_shared_format_binding_Binding generateSchema(ModelToFlatDataConfiguration config, PureModel pureModel)
    {
        Root_meta_external_format_flatdata_binding_fromPure_ModelToFlatDataConfiguration configuration = new Root_meta_external_format_flatdata_binding_fromPure_ModelToFlatDataConfiguration_Impl("")
                ._targetBinding(config.targetBinding)
                ._targetSchemaSet(config.targetSchemaSet);

        config.sourceModel.forEach(pe -> configuration._sourceModelAdd(pureModel.getPackageableElement(pe)));

        return IN_DEBUG
               ? core_external_format_flatdata_binding_pureToFlatData.Root_meta_external_format_flatdata_binding_fromPure_pureToFlatDataWithDebug_ModelToFlatDataConfiguration_1__Binding_1_(configuration, pureModel.getExecutionSupport())
               : core_external_format_flatdata_binding_pureToFlatData.Root_meta_external_format_flatdata_binding_fromPure_pureToFlatData_ModelToFlatDataConfiguration_1__Binding_1_(configuration, pureModel.getExecutionSupport());
    }

    @Override
    public List<String> getRegisterablePackageableElementNames()
    {
        ImmutableList<String> versions = PureClientVersions.versionsSince("v1_21_0");
        return versions.collect(v -> "meta::protocols::pure::" + v + "::external::format::flatdata::serializerExtension_String_1__SerializerExtension_1_").toList();
    }

    private FlatData transformFlatData(Root_meta_external_format_flatdata_metamodel_FlatData flatData)
    {
        return new FlatData().withSections(flatData._sections().collect(this::transformSection));
    }

    private FlatDataSection transformSection(Root_meta_external_format_flatdata_metamodel_FlatDataSection section)
    {
        return new FlatDataSection(section._name(), section._driverId())
                .withProperties(section._sectionProperties().collect(this::transformSectionProperty))
                .withRecordType(this.transformRecordType(section._recordType()));
    }

    private FlatDataProperty transformSectionProperty(Root_meta_external_format_flatdata_metamodel_FlatDataProperty property)
    {
        return new FlatDataProperty(property._name(), property._value());
    }

    private FlatDataRecordType transformRecordType(Root_meta_external_format_flatdata_metamodel_FlatDataRecordType recordType)
    {
        return recordType == null ? null : new FlatDataRecordType().withFields(recordType._fields().collect(this::transformField));
    }

    private FlatDataRecordField transformField(Root_meta_external_format_flatdata_metamodel_FlatDataRecordField field)
    {
        FlatDataDataType type;
        if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataString)
        {
            type = new FlatDataString(field._type()._optional());
        }
        else if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataBoolean)
        {
            Root_meta_external_format_flatdata_metamodel_FlatDataBoolean b = (Root_meta_external_format_flatdata_metamodel_FlatDataBoolean) field._type();
            type = new FlatDataBoolean(b._optional()).withFalseString(b._falseString()).withTrueString(b._trueString());
        }
        else if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataInteger)
        {
            Root_meta_external_format_flatdata_metamodel_FlatDataInteger i = (Root_meta_external_format_flatdata_metamodel_FlatDataInteger) field._type();
            type = new FlatDataInteger(i._optional()).withFormat(i._format());
        }
        else if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataDecimal)
        {
            Root_meta_external_format_flatdata_metamodel_FlatDataDecimal d = (Root_meta_external_format_flatdata_metamodel_FlatDataDecimal) field._type();
            type = new FlatDataDecimal(d._optional()).withFormat(d._format());
        }
        else if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataDate)
        {
            Root_meta_external_format_flatdata_metamodel_FlatDataDate d = (Root_meta_external_format_flatdata_metamodel_FlatDataDate) field._type();
            FlatDataTemporal date = new FlatDataDate(d._optional());
            d._format().forEach(date::addFormat);
            type = date;
        }
        else if (field._type() instanceof Root_meta_external_format_flatdata_metamodel_FlatDataDateTime)
        {
            Root_meta_external_format_flatdata_metamodel_FlatDataDateTime dt = (Root_meta_external_format_flatdata_metamodel_FlatDataDateTime) field._type();
            FlatDataTemporal dateTime = new FlatDataDateTime(dt._optional()).withTimeZone(dt._timeZone());
            dt._format().forEach(dateTime::addFormat);
            type = dateTime;
        }
        else
        {
            throw new IllegalStateException("Unknown data type");
        }
        return new FlatDataRecordField(field._label(), type, field._address());
    }

}
