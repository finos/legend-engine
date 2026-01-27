// Copyright 2026 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.deephaven.test.shared;

import io.deephaven.client.impl.BarrageSession;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.csv.CsvTools;
import io.deephaven.csv.util.CsvReaderException;
import io.deephaven.engine.primitive.iterator.CloseableIterator;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfByte;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfChar;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfDouble;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfFloat;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfInt;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfLong;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfShort;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.Table;
import io.deephaven.qst.array.Array;
import io.deephaven.qst.array.ByteArray;
import io.deephaven.qst.array.CharArray;
import io.deephaven.qst.array.DoubleArray;
import io.deephaven.qst.array.FloatArray;
import io.deephaven.qst.array.IntArray;
import io.deephaven.qst.array.LongArray;
import io.deephaven.qst.array.ShortArray;
import io.deephaven.qst.column.Column;
import io.deephaven.qst.table.NewTable;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import io.deephaven.qst.type.StringType;
import org.apache.arrow.memory.BufferAllocator;
import org.eclipse.collections.api.factory.Maps;

public class CsvToNewTable
{
    private static final Map<Class<?>, BiFunction<CsvToNewTable, String, Array<?>>> HANDLERS;

    static
    {
        Map<Class<?>, BiFunction<CsvToNewTable, String, Array<?>>> handlers = Maps.mutable.empty();
        handlers.put(int.class, CsvToNewTable::intHandler);
        handlers.put(Integer.class, CsvToNewTable::intHandler);
        handlers.put(byte.class, CsvToNewTable::byteHandler);
        handlers.put(Byte.class, CsvToNewTable::byteHandler);
        handlers.put(long.class, CsvToNewTable::longHandler);
        handlers.put(Long.class, CsvToNewTable::longHandler);
        handlers.put(short.class, CsvToNewTable::shortHandler);
        handlers.put(Short.class, CsvToNewTable::shortHandler);
        handlers.put(double.class, CsvToNewTable::doubleHandler);
        handlers.put(Double.class, CsvToNewTable::doubleHandler);
        handlers.put(float.class, CsvToNewTable::floatHandler);
        handlers.put(Float.class, CsvToNewTable::floatHandler);
        handlers.put(char.class, CsvToNewTable::charHandler);
        handlers.put(Character.class, CsvToNewTable::charHandler);
        handlers.put(String.class, CsvToNewTable::stringHandler);
        HANDLERS = handlers;
    }

    private final Table table;

    public CsvToNewTable(String csv) throws CsvReaderException
    {
        this.table = CsvTools.readCsv(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    public NewTable toNewTable()
    {
        Map<String, ? extends ColumnSource<?>> columnSourceMap = table.getColumnSourceMap();

        List<Column<?>> columns = new ArrayList<>(columnSourceMap.size());

        for (Map.Entry<String, ? extends ColumnSource<?>> e : columnSourceMap.entrySet())
        {
            String key = e.getKey();
            ColumnSource<?> value = e.getValue();

            Array<?> values = Objects.requireNonNull(HANDLERS.get(value.getType()), () -> value.getType() + " not supported").apply(this, key);
            columns.add(Column.of(key, values));
        }

        return NewTable.of(columns);
    }

    public void publish(String tableName, BufferAllocator bufferAllocator, BarrageSession session) throws Exception
    {
        NewTable newTable = this.toNewTable();
        TableHandle handle = session.putExport(newTable, bufferAllocator);
        session.session().publish(tableName, handle).get();
    }

    private Array<?> intHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfInt iterator = this.table.integerColumnIterator(key))
        {
            return IntArray.of(iterator.intStream().toArray());
        }
    }

    private Array<?> byteHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfByte iterator = this.table.byteColumnIterator(key))
        {
            List<Byte> bytes = new ArrayList<>();
            while (iterator.hasNext())
            {
                bytes.add(iterator.nextByte());
            }
            byte[] byteArray = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++)
            {
                byteArray[i] = bytes.get(i);
            }
            return ByteArray.of(byteArray);
        }
    }

    private Array<?> longHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfLong iterator = this.table.longColumnIterator(key))
        {
            return LongArray.of(iterator.longStream().toArray());
        }
    }

    private Array<?> shortHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfShort iterator = this.table.shortColumnIterator(key))
        {
            List<Short> shorts = new ArrayList<>();
            while (iterator.hasNext())
            {
                shorts.add(iterator.nextShort());
            }
            short[] shortArray = new short[shorts.size()];
            for (int i = 0; i < shorts.size(); i++)
            {
                shortArray[i] = shorts.get(i);
            }
            return ShortArray.of(shortArray);
        }
    }

    private Array<?> doubleHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfDouble iterator = this.table.doubleColumnIterator(key))
        {
            return DoubleArray.of(iterator.doubleStream().toArray());
        }
    }

    private Array<?> floatHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfFloat iterator = this.table.floatColumnIterator(key))
        {
            List<Float> floats = new ArrayList<>();
            while (iterator.hasNext())
            {
                floats.add(iterator.nextFloat());
            }
            float[] floatArray = new float[floats.size()];
            for (int i = 0; i < floats.size(); i++)
            {
                floatArray[i] = floats.get(i);
            }
            return FloatArray.of(floatArray);
        }
    }

    private Array<?> charHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfChar iterator = this.table.characterColumnIterator(key))
        {
            List<Character> chars = new ArrayList<>();
            while (iterator.hasNext())
            {
                chars.add(iterator.nextChar());
            }
            char[] charArray = new char[chars.size()];
            for (int i = 0; i < chars.size(); i++)
            {
                charArray[i] = chars.get(i);
            }
            return CharArray.of(charArray);
        }
    }

    private Array<?> stringHandler(String key)
    {
        try (CloseableIterator<String> iterator = this.table.objectColumnIterator(key, String.class))
        {
            List<String> strings = new ArrayList<>();
            while (iterator.hasNext())
            {
                strings.add(iterator.next());
            }
            return io.deephaven.qst.array.GenericArray.of(StringType.of(), strings.toArray(new String[0]));
        }
    }
}
