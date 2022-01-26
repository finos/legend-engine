// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

public abstract class StreamingReadDriver<T> implements FlatDataReadDriver<T>
{
    protected static final Consumer<LineReader.Line> NO_OP = x ->
    {
    };

    private static AtomicInteger nextRawThreadReaderId = new AtomicInteger(1);

    protected final StreamingDriverHelper helper;
    protected final CommonDataHandler commonDataHandler;
    private final InputStreamConnection connection;

    private RawLines rawLines;

    protected StreamingReadDriver(StreamingDriverHelper helper)
    {
        this.helper = helper;
        this.commonDataHandler = helper.section.getRecordType() == null ? null : new CommonDataHandler(helper.section, helper.context);
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

    public boolean canStartAt(CharCursor cursor)
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
            CharCursor csr = cursor.copy();
            try
            {
                long untilLines = FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).orElseThrow(() -> new IllegalStateException("Expected number of lines"));
                return canReadExpectedLines(csr, untilLines) && ((StreamingSequentialSectionsProcessingContext) helper.context).isNextSectionReadyToStartAt(csr);
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
                    sectionHasConsumedAllItsRawLines = StreamingReadDriver.this.connection::isConsumedToEof;
                }
                else if (FlatDataUtils.getString(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).isPresent())
                {
                    String untilText = FlatDataUtils.getString(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).get();
                    sectionHasConsumedAllItsRawLines = () -> lastLine != null && untilText.equals(lastLine.getText());
                }
                else if (FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).isPresent())
                {
                    long untilLines = FlatDataUtils.getInteger(helper.section.getSectionProperties(), StreamingDriverHelper.SCOPE, StreamingDriverHelper.FOR_NUMBER_OF_LINES).get();
                    sectionHasConsumedAllItsRawLines = () -> lineCount >= untilLines;
                }
                else
                {
                    // Scope default
                    sectionHasConsumedAllItsRawLines = () -> ((StreamingSequentialSectionsProcessingContext) helper.context).isNextSectionReadyToStartAt(connection.getCursor());
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
