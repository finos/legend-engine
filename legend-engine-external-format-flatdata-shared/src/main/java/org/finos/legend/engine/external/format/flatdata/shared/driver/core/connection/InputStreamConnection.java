package org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InputStreamConnection implements Connection
{
    private static final int BLOCK_SIZE = 4 * 1024;
    private static final int BUFFER_SIZE = 64 * 1024;

    private final InputStream inputStream;

    private BufferedReader buffer;
    private CharCursor cursor;

    public InputStreamConnection(InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    @Override
    public void open() throws IOException
    {
        buffer = new BufferedReader(BLOCK_SIZE, BUFFER_SIZE, new InputStreamReader(inputStream));
        cursor = buffer.openCursor();
    }

    public CharCursor getCursor()
    {
        return cursor;
    }

    @Override
    public void close()
    {
        this.buffer.close();
    }

    public boolean isConsumedToEof()
    {
        return cursor.isEndOfData();
    }
}
