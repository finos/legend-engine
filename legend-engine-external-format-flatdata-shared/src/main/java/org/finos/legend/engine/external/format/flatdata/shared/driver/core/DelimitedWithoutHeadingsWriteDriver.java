package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

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
        PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8)));
        writeDataLines(writer);
        writer.flush();
    }
}
