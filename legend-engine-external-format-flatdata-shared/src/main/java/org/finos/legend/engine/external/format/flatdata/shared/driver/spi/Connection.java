package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import java.io.IOException;

public interface Connection
{
    void open() throws IOException;

    void close() throws IOException;
}
