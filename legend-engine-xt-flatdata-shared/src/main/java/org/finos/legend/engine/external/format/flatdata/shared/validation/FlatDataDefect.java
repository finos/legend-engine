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

package org.finos.legend.engine.external.format.flatdata.shared.validation;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.Objects;

public class FlatDataDefect
{
    private final FlatData flatData;
    private final FlatDataSection section;
    private final String message;

    public FlatDataDefect(FlatData flatData, String message)
    {
        this(flatData, null, message);
    }

    public FlatDataDefect(FlatData flatData, FlatDataSection section, String message)
    {
        this.flatData = Objects.requireNonNull(flatData);
        this.section = section;
        this.message = Objects.requireNonNull(message);
    }

    public FlatData getFlatData()
    {
        return flatData;
    }

    public FlatDataSection getSection()
    {
        return section;
    }

    @Override
    public String toString()
    {
        return message +
                (section == null ? "" : " in section '" + section.getName() + "'");
    }
}
