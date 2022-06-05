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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;

import java.util.Collections;
import java.util.List;

public class NoValuesRawFlatData extends AbstractRawFlatData
{
    public NoValuesRawFlatData(long number, long lineNumber, String record)
    {
        super(number, lineNumber, record);
    }

    @Override
    protected List<RawFlatDataValue> createValues()
    {
        return Collections.emptyList();
    }
}
