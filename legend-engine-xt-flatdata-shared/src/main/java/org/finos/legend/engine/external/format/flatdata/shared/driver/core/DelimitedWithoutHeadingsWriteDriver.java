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

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.io.OutputStream;
import java.io.PrintWriter;

public class DelimitedWithoutHeadingsWriteDriver<T> extends DelimitedWriteDriver<T>
{
    DelimitedWithoutHeadingsWriteDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new DelimitedDriverHelper(section, context));
    }

    @Override
    public String getId()
    {
        return DelimitedWithHeadingsDriverDescription.ID;
    }

    @Override
    public void write(OutputStream stream)
    {
        PrintWriter writer = new PrintWriter(stream);
        writeDataLines(writer);
        writer.flush();
    }
}
