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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.Collections;
import java.util.List;

public class DelimitedLine extends SimpleLine
{
    private final List<String> values;
    private final List<IDefect> defects;

    public DelimitedLine(long lineNumber, String text, List<String> values, List<IDefect> defects)
    {
        super(lineNumber, text);
        this.values = Collections.unmodifiableList(values);
        this.defects = Collections.unmodifiableList(defects);
    }

    public List<String> getValues()
    {
        return values;
    }

    public List<IDefect> getDefects()
    {
        return defects;
    }

    @Override
    public boolean isEmpty()
    {
        return super.isEmpty() && values.size() < 2;
    }
}
