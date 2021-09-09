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
