package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLineReader;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.function.LongSupplier;

public class ImmaterialLinesReadDriver extends StreamingReadDriver<Object>
{
    public static final String ID = "ImmaterialLines";

    protected ImmaterialLinesReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new StreamingDriverHelper(section, context));
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new SimpleLineReader(cursor, helper.eol, lineNumberSupplier);
    }

    @Override
    public String getId()
    {
        return ImmaterialLinesReadDriver.ID;
    }

    @Override
    public Collection<IChecked<Object>> readCheckedObjects()
    {
        untilLine(l -> isFinished(), NO_OP);
        return Collections.emptyList();
    }
}
