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

package org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;

import java.util.List;

public class FieldHandlerRecordType extends FlatDataRecordType
{
    public FieldHandlerRecordType(FlatDataRecordType recordType, List<FieldHandler> fieldHandlers)
    {
        fields = ListIterate.collect(recordType.fields, field -> new FieldHandlerRecordField(field, fieldHandlers));
    }
}
