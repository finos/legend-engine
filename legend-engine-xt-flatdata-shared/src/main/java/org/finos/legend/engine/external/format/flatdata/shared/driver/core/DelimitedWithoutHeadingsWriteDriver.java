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
