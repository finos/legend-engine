package org.finos.legend.engine.external.format.flatdata.shared.driver.spi;

import java.io.OutputStream;

/**
 * Providers should implement this interface to handle the writing of their flat data (if
 * write is supported).  A driver is used to handle the processing of data as a section of a
 * <tt>FlatData</tt>.
 */
public interface FlatDataWriteDriver<T> extends FlatDataDriver
{
    /**
     * Called to write data for this driver.
     */
    void write(OutputStream stream);
}
