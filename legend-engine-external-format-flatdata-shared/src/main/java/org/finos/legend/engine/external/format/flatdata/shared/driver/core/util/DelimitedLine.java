package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLine;
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
