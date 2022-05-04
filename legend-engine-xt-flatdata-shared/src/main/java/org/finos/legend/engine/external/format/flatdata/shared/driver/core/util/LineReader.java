package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

public interface LineReader
{
    Line readLine();

    interface Line
    {
        long getLineNumber();

        String getText();

        boolean isEmpty();
    }
}
