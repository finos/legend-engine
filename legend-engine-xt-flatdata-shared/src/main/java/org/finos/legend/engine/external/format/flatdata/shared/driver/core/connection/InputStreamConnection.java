//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

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
