// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.driver.core;

import java.util.Iterator;
import org.finos.legend.engine.external.format.flatdata.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RecordTypeMultiplicity;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.validation.FlatDataValidator;

import java.util.ArrayList;
import java.util.List;

public class FixedWidthDriverDescription extends StreamingDriverDescription implements FlatDataValidator
{
    public static final String ID = "FixedWidth";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public FlatDataReadDriver newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof InputStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new FixedWidthReadDriver(section, context);

    }

    @Override
    public List<FlatDataDefect> validate(FlatData store, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();
        Iterator<FlatDataRecordField> fieldIterator = section.recordType.fields.iterator();

        while (fieldIterator.hasNext())
        {
            FlatDataRecordField field = fieldIterator.next();
            if (fieldIterator.hasNext())
            {
                if (field.address == null || field.address != null && !field.address.matches("^\\d+:\\d+$"))
                {
                    defects.add(new FlatDataDefect(store, section, "Invalid address for '" + field.label + "' (Expected start:end column numbers)"));
                }
            }
            else
            {
                if (field.address == null || field.address != null && !(field.address.matches("^\\d+:(\\d+|EOL)$")))
                {
                    defects.add(new FlatDataDefect(store, section, "Invalid address for '" + field.label + "' (Expected start:end column numbers or EOL for final column end index)"));
                }
            }
        }
        return defects;
    }

    @Override
    public boolean isSelfDescribing()
    {
        return false;
    }

    @Override
    public RecordTypeMultiplicity getRecordTypeMultiplicity()
    {
        return RecordTypeMultiplicity.MANDATORY;
    }
}
