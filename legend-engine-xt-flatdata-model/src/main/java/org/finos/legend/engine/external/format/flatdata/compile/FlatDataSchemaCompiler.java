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

package org.finos.legend.engine.external.format.flatdata.compile;

import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParseException;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidation;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidationResult;
import org.finos.legend.engine.external.shared.format.model.ExternalSchemaCompileContext;
import org.finos.legend.engine.external.shared.format.model.compile.ExternalFormatSchemaException;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatData;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataBoolean_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDataType;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDateTime_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDate_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDecimal_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataInteger_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataProperty;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataProperty_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordField;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordField_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordType;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordType_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataSection;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataSection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataString_Impl;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatData_Impl;

import java.util.stream.Collectors;

public class FlatDataSchemaCompiler
{
    private final ExternalSchemaCompileContext context;

    public FlatDataSchemaCompiler(ExternalSchemaCompileContext context)
    {
        this.context = context;
    }

    public Root_meta_external_format_flatdata_metamodel_FlatData compile()
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
            return convert(flatData);
        }
        catch (FlatDataSchemaParseException e)
        {
            throw new ExternalFormatSchemaException(e.getMessage(), e.getStartLine(), e.getStartColumn(), e.getEndLine(), e.getEndColumn());
        }
    }

    private Root_meta_external_format_flatdata_metamodel_FlatData convert(FlatData flatData)
    {
        return new Root_meta_external_format_flatdata_metamodel_FlatData_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatData"))
                ._sections(ListIterate.collect(flatData.getSections(), this::convertSection));
    }

    private Root_meta_external_format_flatdata_metamodel_FlatDataSection convertSection(FlatDataSection section)
    {
        return new Root_meta_external_format_flatdata_metamodel_FlatDataSection_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataSection"))
                ._name(section.getName())
                ._driverId(section.getDriverId())
                ._sectionProperties(ListIterate.collect(section.getSectionProperties(), this::convertSectionProperty))
                ._recordType(section.getRecordType() == null ? null : convertFlatDataRecordType(section.getRecordType()));
    }

    private Root_meta_external_format_flatdata_metamodel_FlatDataProperty convertSectionProperty(FlatDataProperty property)
    {
        return new Root_meta_external_format_flatdata_metamodel_FlatDataProperty_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataProperty"))
                ._name(property.getName())
                ._value(ListAdapter.adapt(property.getValues()));
    }

    private Root_meta_external_format_flatdata_metamodel_FlatDataRecordType convertFlatDataRecordType(FlatDataRecordType recordType)
    {
        return new Root_meta_external_format_flatdata_metamodel_FlatDataRecordType_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataRecordType"))
                ._fields(ListIterate.collect(recordType.getFields(), this::convertField));
    }

    private Root_meta_external_format_flatdata_metamodel_FlatDataRecordField convertField(FlatDataRecordField field)
    {
        Root_meta_external_format_flatdata_metamodel_FlatDataDataType type;
        if (field.getType() instanceof FlatDataBoolean)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataBoolean_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataBoolean"))
                    ._trueString(((FlatDataBoolean) field.getType()).getTrueString())
                    ._falseString(((FlatDataBoolean) field.getType()).getFalseString());
        }
        else if (field.getType() instanceof FlatDataString)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataString_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataString"));
        }
        else if (field.getType() instanceof FlatDataInteger)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataInteger_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataInteger"))
                    ._format(((FlatDataInteger) field.getType()).getFormat());
        }
        else if (field.getType() instanceof FlatDataDecimal)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataDecimal_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataDecimal"))
                    ._format(((FlatDataDecimal) field.getType()).getFormat());
        }
        else if (field.getType() instanceof FlatDataDate)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataDate_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataDate"))
                    ._format(ListAdapter.adapt(((FlatDataDate) field.getType()).getFormat()));
        }
        else if (field.getType() instanceof FlatDataDateTime)
        {
            type = new Root_meta_external_format_flatdata_metamodel_FlatDataDateTime_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataDateTime"))
                    ._timeZone(((FlatDataDateTime) field.getType()).getTimeZone())
                    ._format(ListAdapter.adapt(((FlatDataDateTime) field.getType()).getFormat()));
        }
        else
        {
            throw new IllegalStateException("Unknown flat data type: " + field.getType().getClass().getSimpleName());
        }

        return new Root_meta_external_format_flatdata_metamodel_FlatDataRecordField_Impl("", null, context.getPureModel().getClass("meta::external::format::flatdata::metamodel::FlatDataRecordField"))
                ._label(field.getLabel())
                ._address(field.getAddress())
                ._type(type._optional(field.getType().isOptional()));
    }
}
