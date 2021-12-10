package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.BooleanFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.DateFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.DateTimeFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.DecimalFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.IntegerFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.StringFieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.BooleanParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateTimeParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DecimalParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.IntegerParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.ValueParser;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Cursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public abstract class StreamingReadDriver<T> implements FlatDataReadDriver<T>
{
    protected static final Consumer<LineReader.Line> NO_OP = x ->
    {
    };

    private static AtomicInteger nextRawThreadReaderId = new AtomicInteger(1);

    protected final StreamingDriverHelper helper;
    private final InputStreamConnection connection;

    private RawLines rawLines;

    protected StreamingReadDriver(StreamingDriverHelper helper)
    {
        this.helper = helper;
        this.connection = (InputStreamConnection) helper.context.getConnection();
    }

    protected abstract LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier);

    @Override
    public void start()
    {
        IntegerVariable lineNumber = helper.lineNumber();
        LineReader lineReader = createLineReader(connection.getCursor(), lineNumber::increment);
        rawLines = new RawLines(lineReader);
        rawLines.start();
    }

    @Override
    public void stop()
    {
        // No Op
    }

    @Override
    public boolean isFinished()
    {
        return rawLines != null && !rawLines.hasMore();
    }

    @Override
    public boolean canStartAt(Cursor cursor)
    {
        if (FlatDataUtils.getBoolean(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_EOF))
        {
            return true;
        }
        else if (FlatDataUtils.getBoolean(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.DEFAULT))
        {
            return false;
        }
        else
        {
            CharCursor csr = (CharCursor) cursor.copy();
            try
            {
                long untilLines = FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).orElse(-1l);
                return canReadExpectedLines(csr, untilLines) && helper.context.isNextSectionReadyToStartAt(csr);
            }
            finally
            {
                csr.destroy();
            }
        }
    }

    private boolean canReadExpectedLines(CharCursor cursor, long lines)
    {
        LineReader reader = createLineReader(cursor, () -> 0);
        for (int i = 0; i < lines; i++)
        {
            if (cursor.isEndOfData())
            {
                return false;
            }
            reader.readLine();
        }
        return true;
    }

    protected LineReader.Line nextLine()
    {
        LineReader.Line line = rawLines.next();
        while (helper.skipBlankLines && rawLines.hasMore() && line.isEmpty())
        {
            line = rawLines.next();
        }
        return line;
    }

    protected LineReader.Line untilLine(Predicate<LineReader.Line> test, Consumer<LineReader.Line> processor)
    {
        LineReader.Line line = nextLine();
        while (!test.test(line))
        {
            processor.accept(line);
            line = nextLine();
        }
        return line;
    }

    protected List<FieldHandler> computeFieldHandlers(FlatDataRecordType recordType, Function<FlatDataRecordField, Function<RawFlatData, String>> rawDataAccessorFactory)
    {
        List<ValueParser> parsers = helper.computeValueParsers(recordType);
        List<FieldHandler> fieldHandlers = Lists.mutable.empty();
        for (int i = 0; i < recordType.getFields().size(); i++)
        {
            FlatDataRecordField field = recordType.getFields().get(i);
            FlatDataDataType type = field.getType();
            if (type instanceof FlatDataString)
            {
                fieldHandlers.add(new StringFieldHandler(field, i, rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataBoolean)
            {
                fieldHandlers.add(new BooleanFieldHandler(field, i, (BooleanParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataInteger)
            {
                fieldHandlers.add(new IntegerFieldHandler(field, i, (IntegerParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDecimal)
            {
                fieldHandlers.add(new DecimalFieldHandler(field, i, (DecimalParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDate)
            {
                fieldHandlers.add(new DateFieldHandler(field, i, (DateParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDateTime)
            {
                fieldHandlers.add(new DateTimeFieldHandler(field, i, (DateTimeParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else
            {
                throw new IllegalArgumentException("Unknown datatype: " + type.getClass().getSimpleName());
            }
        }
        return fieldHandlers;
    }

    private class RawLines
    {
        private final LineReader.Line endMarker = new LineReader.Line()
        {
            @Override
            public long getLineNumber()
            {
                throw new UnsupportedOperationException("Marker should not be used");
            }

            @Override
            public String getText()
            {
                throw new UnsupportedOperationException("Marker should not be used");
            }

            @Override
            public boolean isEmpty()
            {
                throw new UnsupportedOperationException("Marker should not be used");
            }
        };

        private final LineReader lineReader;

        private long lineCount = 0;
        private LineReader.Line lastLine = null;

        private BlockingQueue<LineReader.Line> queue = new ArrayBlockingQueue<>(512, true);
        private AtomicBoolean readFailed = new AtomicBoolean(false);
        private AtomicBoolean interrupted = new AtomicBoolean(false);
        private AtomicReference<Exception> readException = new AtomicReference<>();

        private AtomicReference<LineReader.Line> pending = new AtomicReference<>();

        RawLines(LineReader lineReader)
        {
            this.lineReader = lineReader;
        }

        void start()
        {
            new Thread(this::readLines, "Raw Lines Reader " + nextRawThreadReaderId.getAndIncrement()).start();
        }

        // Called on Producer (Raw Lines) Thread
        private void readLines()
        {
            try
            {
                BooleanSupplier sectionHasConsumedAllItsRawLines;
                if (FlatDataUtils.getBoolean(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_EOF))
                {
                    sectionHasConsumedAllItsRawLines = () -> StreamingReadDriver.this.connection.isConsumedToEof();
                }
                else if (FlatDataUtils.getString(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).isPresent())
                {
                    String untilText = FlatDataUtils.getString(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).orElse(null);
                    sectionHasConsumedAllItsRawLines = () -> lastLine != null && untilText.equals(lastLine.getText());
                }
                else if (FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).isPresent())
                {
                    long untilLines = FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).orElse(-1l);
                    sectionHasConsumedAllItsRawLines = () -> lineCount >= untilLines;
                }
                else
                {
                    // Scope default
                    sectionHasConsumedAllItsRawLines = () -> helper.context.isNextSectionReadyToStartAt(connection.getCursor());
                }

                while (!sectionHasConsumedAllItsRawLines.getAsBoolean())
                {
                    lastLine = lineReader.readLine();
                    queue.put(lastLine);
                    lineCount++;
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                interrupted.set(true);
                readFailed.set(true);
            }
            catch (Exception e)
            {
                readException.set(e);
                readFailed.set(true);
            }

            try
            {
                queue.put(endMarker);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                interrupted.set(true);
            }
            catch (Exception e)
            {
                readException.set(e);
                readFailed.set(true);
            }
        }

        // Called on Consumer Thread
        LineReader.Line next()
        {
            checkIfReaderHasFailed();

            if (!hasMore())
            {
                throw new IllegalStateException("No raw data available");
            }

            return Objects.requireNonNull(pending.getAndSet(null));
        }

        // Called on Consumer Thread
        boolean hasMore()
        {
            checkIfReaderHasFailed();

            if (pending.get() != null)
            {
                return pending.get() != endMarker;
            }

            for (int attempt = 0; attempt < 100; attempt++)
            {
                try
                {
                    LineReader.Line next = queue.take();
                    checkIfReaderHasFailed();
                    if (!pending.compareAndSet(null, next))
                    {
                        throw new IllegalStateException("Pending should be empty");
                    }
                    return pending.get() != endMarker;
                }
                catch (InterruptedException e)
                {
                    // Clear interrupt - Will retry unless limit reached
                    Thread.interrupted();
                }
            }
            throw new IllegalStateException("Interrupted whilst accessing raw lines");
        }

        private void checkIfReaderHasFailed()
        {
            if (readFailed.get())
            {
                if (interrupted.get())
                {
                    throw new IllegalStateException("Raw data reading thread was interrupted");
                }
                else if (readException.get() != null)
                {
                    throw readException.get() instanceof RuntimeException
                          ? (RuntimeException) readException.get()
                          : new RuntimeException(readException.get());
                }
                else
                {
                    throw new IllegalStateException("Unknown failure while reading");
                }
            }
        }
    }
}
