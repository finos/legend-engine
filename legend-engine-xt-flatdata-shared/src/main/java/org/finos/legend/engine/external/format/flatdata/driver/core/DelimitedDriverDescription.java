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

package org.finos.legend.engine.external.format.flatdata.driver.core;

import org.finos.legend.engine.external.format.flatdata.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RecordTypeMultiplicity;

import java.util.List;

public abstract class DelimitedDriverDescription extends StreamingDriverDescription
{
    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(super.getSectionProperties())
                .requiredStringProperty(DelimitedReadDriver.DELIMITER)
                .optionalStringProperty(DelimitedReadDriver.QUOTE_CHAR)
                .optionalStringProperty(DelimitedReadDriver.ESCAPING_CHAR)
                .optionalRepeatableStringProperty(DelimitedReadDriver.NULL_STRING)
                .build();
    }

    @Override
    public RecordTypeMultiplicity getRecordTypeMultiplicity()
    {
        return RecordTypeMultiplicity.MANDATORY;
    }
}
