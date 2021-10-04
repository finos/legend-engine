package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.BasicRawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BloombergKeyValues
{
    private final FlatDataProcessingContext context;
    private final List<IDefect> defects = new ArrayList<>();
    private final Map<String, String> keyValues = new LinkedHashMap<>();
    private final List<String> lines = new ArrayList<>();
    private long firstLineNumber;

    BloombergKeyValues(FlatDataProcessingContext context) {
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

    boolean containsKey(String key)
    {
        return keyValues.containsKey(key);
    }

    public String get(String key)
    {
        return keyValues.get(key);
    }

    RawFlatData toFlatDataRecord()
    {
        return  BasicRawFlatData.newRecord(1, firstLineNumber, String.join("\n", lines), Arrays.asList(keys()), Arrays.asList(values()));
    }

    IChecked<RawFlatData> toCheckedFlatDataRecord()
    {
        return  BasicChecked.newChecked(toFlatDataRecord(), null, getDefects());
    }

    String[] keys()
    {
        return keyValues.keySet().toArray(new String[keyValues.size()]);
    }

    String[] values()
    {
        return keyValues.values().toArray(new String[keyValues.size()]);
    }

    public List<IDefect> getDefects()
    {
        return defects;
    }
}
