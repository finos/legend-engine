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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;

import java.util.function.Function;

public class StringFieldHandler extends FieldHandler
{
    public StringFieldHandler(FlatDataRecordField field, int fieldIndex, Function<RawFlatData, String> rawDataAccessor)
    {
        super(field, fieldIndex, s -> null, rawDataAccessor);
    }

    @Override
    public String validate(String raw)
    {
        return null;
    }

    @Override
    public String getString(String raw)
    {
        return raw;
    }
}
