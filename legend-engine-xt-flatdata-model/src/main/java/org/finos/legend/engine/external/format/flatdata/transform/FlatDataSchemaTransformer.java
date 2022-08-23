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

package org.finos.legend.engine.external.format.flatdata.transform;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataTemporal;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatData;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataBoolean;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDate;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDateTime;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataDecimal;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataInteger;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataProperty;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordField;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataRecordType;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataSection;
import org.finos.legend.pure.generated.Root_meta_external_format_flatdata_metamodel_FlatDataString;

public class FlatDataSchemaTransformer
{
    private final Root_meta_external_format_flatdata_metamodel_FlatData flatData;

    public FlatDataSchemaTransformer(Root_meta_external_format_flatdata_metamodel_FlatData flatdata)
    {
        this.flatData = flatdata;
    }

    public FlatData transform()
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
