package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

public class DelimitedWithHeadingsWriteDriver<T> extends DelimitedWriteDriver<T>
{
    DelimitedWithHeadingsWriteDriver(FlatDataSection section, FlatDataProcessingContext<T> context)
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
        writeHeadings(writer);
        writeDataLines(writer);
        writer.flush();
    }

    public void writeHeadings(PrintWriter writer)
    {
        IntegerVariable lineNumber = helper.lineNumber();

        // Headings
        final List<FlatDataRecordField> fields = recordType.getFields();
        String[] headings = new String[fields.size()];
        for (int i = 0; i < headings.length; i++)
        {
            headings[i] = fields.get(i).getLabel();
        }
        writeLine(writer, headings, lineNumber);
    }
}
