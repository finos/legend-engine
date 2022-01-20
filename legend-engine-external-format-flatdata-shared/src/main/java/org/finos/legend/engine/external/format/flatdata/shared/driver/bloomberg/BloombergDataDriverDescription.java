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

package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RecordTypeMultiplicity;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;

/* Driver to handle Bloomberg getdata format */
public class BloombergDataDriverDescription extends AbstractBloombergDriverDescription
{
    public static final String ID = "BloombergData";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public <T> FlatDataReadDriver<T> newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof InputStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new BloombergDataReadDriver<>(section, context);
    }

    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder()
                .booleanProperty(AbstractBloombergReadDriver.MANDATORY_SECTION_PROPERTY)
                .optionalRepeatableStringProperty(FILTER_PROPERTY)
                .build();
    }

    @Override
    public RecordTypeMultiplicity getRecordTypeMultiplicity()
    {
        return RecordTypeMultiplicity.MANDATORY;
    }
}

