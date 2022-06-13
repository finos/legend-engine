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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BloombergKeyValues
{
    static final FlatDataVariable VARIABLE_LAST_METADATA = new FlatDataVariable("lastBloombergMetedata", VariableType.Object);

    private final FlatDataProcessingContext context;
    private final List<IDefect> defects = new ArrayList<>();
    private final Map<String, String> keyValues = new LinkedHashMap<>();
    private final List<String> lines = new ArrayList<>();
    private long firstLineNumber;

    BloombergKeyValues(FlatDataProcessingContext context)
    {
        this.context = context;
    }

    void parse(LineReader.Line line)
    {
        if (line.getText().trim().length() > 0)
        {
            recordLine(line);
            int eqPos = line.getText().indexOf('=');
            if (eqPos > 0)
            {
                String name = line.getText().substring(0, eqPos);
                String value = eqPos + 1 < line.getText().length() ? line.getText().substring(eqPos + 1) : "";
                keyValues.put(name, value);
            }
            else
            {
                defects.add(BasicDefect.newInvalidInputErrorDefect("Ignoring malformed line: '" + line.getText() + "'", context.getDefiningPath()));
            }
        }
    }

    private void recordLine(LineReader.Line line)
    {
        if (lines.isEmpty())
        {
            firstLineNumber = line.getLineNumber();
        }
        lines.add(line.getText());
    }

    public LineReader.Line line()
    {
        if (lines.isEmpty())
        {
            throw new IllegalStateException("No lines have been recorded");
        }

        return new SimpleLine(firstLineNumber, String.join("\n", lines));
    }

    boolean containsKey(String key)
    {
        return keyValues.containsKey(key);
    }

    public String get(String key)
    {
        return keyValues.get(key);
    }

    String[] keys()
    {
        return keyValues.keySet().toArray(new String[0]);
    }

    String[] values()
    {
        return keyValues.values().toArray(new String[0]);
    }

    public List<IDefect> getDefects()
    {
        return defects;
    }
}
