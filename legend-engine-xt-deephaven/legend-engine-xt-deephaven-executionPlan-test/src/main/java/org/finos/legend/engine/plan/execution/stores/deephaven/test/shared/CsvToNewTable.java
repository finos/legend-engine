import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfByte;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfChar;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfDouble;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfFloat;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfInt;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfLong;
import io.deephaven.engine.primitive.iterator.CloseablePrimitiveIteratorOfShort;
import io.deephaven.engine.table.ColumnSource;
import io.deephaven.engine.table.Table;
import io.deephaven.engine.table.iterators.CloseableIterator;
import io.deephaven.qst.array.Array;
import io.deephaven.qst.array.ByteArray;
import io.deephaven.qst.array.CharArray;
import io.deephaven.qst.array.DoubleArray;
import io.deephaven.qst.array.FloatArray;
import io.deephaven.qst.array.IntArray;
import io.deephaven.qst.array.LongArray;
import io.deephaven.qst.array.ShortArray;
import io.deephaven.qst.array.StringArray;
// ...existing code...

public class CsvToNewTable
{
    private static final Map<Class<?>, BiFunction<CsvToNewTable, String, Array<?>>> HANDLERS = Maps.mutable.of(
            int.class, CsvToNewTable::intHandler,
            Byte.class, CsvToNewTable::byteHandler,
            String.class, CsvToNewTable::stringHandler,
            long.class, CsvToNewTable::longHandler,
            Long.class, CsvToNewTable::longHandler,
            short.class, CsvToNewTable::shortHandler,
            Short.class, CsvToNewTable::shortHandler,
            char.class, CsvToNewTable::charHandler,
            Character.class, CsvToNewTable::charHandler,
            float.class, CsvToNewTable::floatHandler,
            Float.class, CsvToNewTable::floatHandler,
            double.class, CsvToNewTable::doubleHandler,
            Double.class, CsvToNewTable::doubleHandler
    );

    // ...existing code...

    private Array<?> stringHandler(String key)
    {
        try (CloseableIterator<String> iterator = this.table.objectColumnIterator(key))
        {
            List<String> strings = new ArrayList<>();
            while (iterator.hasNext())
            {
                strings.add(iterator.next());
            }
            return StringArray.of(strings.toArray(new String[0]));
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

    private Array<?> doubleHandler(String key)
    {
        try (CloseablePrimitiveIteratorOfDouble iterator = this.table.doubleColumnIterator(key))
        {
            return DoubleArray.of(iterator.doubleStream().toArray());
        }
    }

//    CloseablePrimitiveIteratorOfChar characterColumnIterator(@NotNull String columnName);

