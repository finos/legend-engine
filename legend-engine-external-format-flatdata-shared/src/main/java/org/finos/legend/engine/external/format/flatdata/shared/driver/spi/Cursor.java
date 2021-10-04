package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

public interface Cursor
{
    boolean isEndOfData();
    Cursor copy();
    void destroy();
}
