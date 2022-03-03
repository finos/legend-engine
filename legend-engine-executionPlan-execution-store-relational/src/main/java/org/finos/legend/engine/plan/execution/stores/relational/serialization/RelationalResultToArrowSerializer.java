package org.finos.legend.engine.plan.execution.stores.relational.serialization;


import org.apache.arrow.adapter.jdbc.JdbcToArrowConfig;
import org.apache.arrow.adapter.jdbc.JdbcToArrowConfigBuilder;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.dictionary.DictionaryProvider;
import org.apache.arrow.vector.ipc.ArrowStreamWriter;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.apache.arrow.adapter.jdbc.ArrowVectorIterator;

import java.io.*;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RelationalResultToArrowSerializer extends Serializer {

    private final RelationalResult relationalResult;


    public RelationalResultToArrowSerializer(RelationalResult relationalResult) {
        this.relationalResult = relationalResult;
    }

    @Override
    public void stream(OutputStream targetStream)
    {
        BufferAllocator allocator = new RootAllocator(Integer.MAX_VALUE);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT);
        JdbcToArrowConfig config = new JdbcToArrowConfigBuilder(allocator, calendar)
                .setReuseVectorSchemaRoot(true)
                .setIncludeMetadata(true)
                .build();
        ArrowStreamWriter writer = null;

        try
        {

            ArrowVectorIterator arrowVectorIterator = ArrowVectorIterator.create(this.relationalResult.resultSet, config);

            VectorSchemaRoot vectorSchemaRoot = arrowVectorIterator.next();
            writer = new ArrowStreamWriter(vectorSchemaRoot, (DictionaryProvider) null, targetStream);
            writer.start();
            writer.writeBatch();

            while (arrowVectorIterator.hasNext()) {
                //will read a new vectorSchemaRoot
                arrowVectorIterator.next();
                writer.writeBatch();
            }

            writer.end();
            arrowVectorIterator.close();
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating Arrow result", e);
        }
        finally
        {
            relationalResult.close();
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
