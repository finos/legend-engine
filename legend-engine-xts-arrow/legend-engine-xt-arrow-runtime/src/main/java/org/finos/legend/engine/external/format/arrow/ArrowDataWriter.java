// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.external.format.arrow;

import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;
import org.apache.arrow.adapter.jdbc.JdbcToArrow;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ArrowDataWriter extends ExternalFormatWriter implements AutoCloseable
{
    private ArrowVectorIterator iterator;
    private BufferAllocator allocator;

    public ArrowDataWriter(ResultSet resultSet) throws SQLException, IOException
    {
        this.allocator = new RootAllocator();
        this.iterator = JdbcToArrow.sqlToArrowVectorIterator(resultSet, allocator);

    }


    @Override
    public void writeData(OutputStream outputStream) throws IOException
    {

        while (this.iterator.hasNext())
        {
            VectorSchemaRoot vector = iterator.next();
            ArrowStreamWriter writer = new ArrowStreamWriter(vector, null, outputStream);
            writer.start();
            writer.writeBatch();
            writer.close();
            vector.close();
        }
        this.iterator.close();
    }

//    @Override
//    public void writeDataAsString(OutputStream outputStream) throws IOException
//    {
//        while (this.iterator.hasNext())
//        {
//            VectorSchemaRoot vector = iterator.next();
//            outputStream.write(vector.contentToTSVString().getBytes(Charset.forName("UTF-8")));
//            vector.close();
//        }
//        this.iterator.close();
//
//    }


    @Override
    public void close()
    {
        allocator.close();
    }
}

