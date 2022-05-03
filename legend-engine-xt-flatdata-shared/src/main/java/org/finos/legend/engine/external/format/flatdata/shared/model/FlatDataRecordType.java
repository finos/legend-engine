// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.model;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;

import java.util.Collections;
import java.util.List;

public class FlatDataRecordType
{
    private List<FlatDataRecordField> fields = Lists.mutable.empty();

    public FlatDataRecordType withFields(Iterable<FlatDataRecordField> fields)
    {
        fields.forEach(this::withField);
        return this;
    }

    public FlatDataRecordType withField(String label, FlatDataDataType type)
    {
        return withField(new FlatDataRecordField(label, type));
    }

    public FlatDataRecordType withField(String label, FlatDataDataType type,  String address)
    {
        return withField(new FlatDataRecordField(label, type, address));
    }

    public FlatDataRecordType withField(FlatDataRecordField field)
    {
        fields.add(field);
        return this;
    }

    public List<FlatDataRecordField> getFields()
    {
        return Collections.unmodifiableList(fields);
    }
}
