package org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Cursor;

public interface CharCursor extends Cursor
{
    int END_OF_DATA = -1;

    int advance();
    char[] advance(int howMany);

    int peek(int ahead);

    @Override
    CharCursor copy();
}
