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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.ObjectStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataWriteDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;

public class DelimitedWithHeadingsDriverDescription extends DelimitedDriverDescription
{
    public static final String ID = "DelimitedWithHeadings";

    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(super.getSectionProperties())
                .booleanProperty(DelimitedWithHeadingsReadDriver.MODELLED_COUMNNS_REQIURED)
                .booleanProperty(DelimitedWithHeadingsReadDriver.ONLY_MODELLED_COLUMNS)
                .booleanProperty(DelimitedWithHeadingsReadDriver.MATCH_COLUMNS_CASE_INSENSITIVE)
                .build();
    }

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
        return new DelimitedWithHeadingsReadDriver(section, context);
    }

    @Override
    public <T> FlatDataWriteDriver<T> newWriteDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof ObjectStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new DelimitedWithHeadingsWriteDriver(section, context);
    }

    @Override
    public boolean isSelfDescribing()
    {
        return true;
    }
}
