package org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BufferedReader
{
    private final Reader reader;
    private final int blockSize;
    private final AtomicReference<Block> lowBlock;
    private final AtomicReference<Block> highBlock;

    private final int capacity;
    private final AtomicInteger capacityUsed = new AtomicInteger(0);

    private final Object bufferLock = new Object();

    public BufferedReader(int blockSize, int capacity, Reader reader)
    {
        this.blockSize = blockSize;
        this.capacity = capacity;
        this.reader = reader;

        Block startBlock = new StartBlock();
        lowBlock = new AtomicReference<>(startBlock);
        highBlock = new AtomicReference<>(startBlock);
    }

    public void close()
    {
        try
        {
            reader.close();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    public CharCursor openCursor()
    {
        return highBlock.get().openCursor();
    }

    // Cursor is not multithreaded
    private class BufferCursor implements CharCursor
    {
        // Index of the next character to be consumed
        private long position;
        private Block block;
        private CursorState cursorState;

        private BufferCursor(Block block)
        {
            this.position = 0;
            this.block = block;
            this.cursorState = new ActiveCursorState();
            block.addCursor();
        }

        private BufferCursor(BufferCursor copyOf)
        {
            this.position = copyOf.position;
            this.block = copyOf.block;
            this.cursorState = new ActiveCursorState();
            block.addCursor();
        }

        @Override
        public void destroy()
        {
            cursorState.destroy();
            block.removeCursor();
        }

        @Override
        public int advance()
        {
            return cursorState.advance();
        }

        @Override
        public char[] advance(int howMany)
        {
            return cursorState.advance(howMany);
        }

        public int peek(int ahead)
        {
            return cursorState.peek(ahead);
        }

        @Override
        public boolean isEndOfData()
        {
            return cursorState.isEndOfData();
        }

        @Override
        public BufferCursor copy()
        {
            return new BufferCursor(this);
        }

        private abstract class CursorState
        {
            abstract int advance();

            abstract char[] advance(int howMany);

            abstract public int peek(int ahead);

            abstract boolean isEndOfData();

            abstract void destroy();
        }

        private class ActiveCursorState extends CursorState
        {
            @Override
            public int advance()
            {
                moveToNextBlockIfRequired();
                return block.charAt(position++);
            }

            public char[] advance(int howMany)
            {
                if (howMany < 0)
                {
                    throw new IllegalArgumentException("Cannot advance negatively");
                }

                char[] chars = new char[howMany];
                int copyTo = 0;
                int toAdvance = howMany;
                while (toAdvance > 0)
                {
                    moveToNextBlockIfRequired();
                    int available = (int) (block.endIndex - position);
                    if (available == 0)
                    {
                        return Arrays.copyOfRange(chars, 0, howMany - toAdvance);
                    }

                    int toTake = Math.min(toAdvance, available);
                    block.copyChars(position, position+toTake, chars, copyTo);
                    position += toTake;
                    copyTo += toTake;
                    toAdvance -= toTake;
                }
                return chars;
            }

            public int peek(int ahead)
            {
                if (ahead <= 0)
                {
                    throw new IllegalArgumentException("Cannot peek on characters that have been advanced");
                }

                return block.charAt(position + ahead - 1);
            }

            boolean isEndOfData()
            {
                moveToNextBlockIfRequired();
                return block.isEndOfData();
            }

            private void moveToNextBlockIfRequired()
            {
                if (position >= block.endIndex)
                {
                    Block next = block.nextBlock();
                    next.addCursor();
                    block.removeCursor();
                    block = next;
                }
            }

            @Override
            void destroy()
            {
                cursorState = new InactiveCursorState();
            }
        }

        private class InactiveCursorState extends CursorState
        {
            @Override
            public int advance()
            {
                throw new IllegalStateException("This cursor has been destroyed");
            }

            @Override
            char[] advance(int howMany)
            {
                throw new IllegalStateException("This cursor has been destroyed");
            }

            @Override
            public int peek(int ahead)
            {
                throw new IllegalStateException("This cursor has been destroyed");
            }

            @Override
            boolean isEndOfData()
            {
                throw new IllegalStateException("This cursor has been destroyed");
            }

            @Override
            void destroy()
            {
                throw new IllegalStateException("This cursor has already been destroyed");
            }
        }
    }

    private abstract class Block
    {
        final long startIndex; // Inclusive
        final long endIndex;   // Exclusive
        final AtomicInteger cursorCount = new AtomicInteger(0);
        volatile Block nextBlock = null;

        Block(long startIndex, long endIndex)
        {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        abstract CharCursor openCursor();

        abstract boolean isEndOfData();

        abstract int charAt(long index);

        abstract void copyChars(long startIndex, long endIndex, char[] into, int intoIndex);

        abstract int size();

        void addCursor()
        {
            cursorCount.incrementAndGet();
        }

        void removeCursor()
        {
            if (cursorCount.decrementAndGet() == 0)
            {
                if (lowBlock.compareAndSet(this, nextBlock))
                {
                    capacityUsed.addAndGet(-this.size());
                }
            }
        }

        Block nextBlock()
        {
            if (nextBlock == null)
            {
                createNextBlock();
            }
            return  nextBlock;
        }

        private void createNextBlock()
        {
            synchronized (BufferedReader.this.bufferLock)
            {
                if (capacityUsed.get() >= capacity)
                {
                    throw new IllegalStateException("Insufficient capacity to load more data");
                }

                if (nextBlock != null)
                {
                    return;
                }

                try
                {
                    char[] buffer = new char[blockSize];
                    int read = reader.read(buffer);
                    Block newBlock;
                    if (read == -1)
                    {
                        newBlock = new EndBlock(endIndex);
                    }
                    else if (read == blockSize)
                    {
                        newBlock = new DataBlock(endIndex, buffer);
                    }
                    else
                    {
                        newBlock = new DataBlock(endIndex, Arrays.copyOfRange(buffer, 0, read));
                    }
                    nextBlock = newBlock;
                    if (!BufferedReader.this.highBlock.compareAndSet(this, newBlock))
                    {
                        throw new Error("New block is not added to  end of chain");
                    }
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    private class StartBlock extends Block
    {
        StartBlock()
        {
            super(-1, 0);
        }

        CharCursor openCursor()
        {
            return new BufferCursor(this);
        }

        @Override
        int charAt(long index)
        {
            return nextBlock().charAt(index);
        }

        @Override
        boolean isEndOfData()
        {
            return false;
        }

        @Override
        void copyChars(long start, long end, char[] into, int intoIndex)
        {
            // Do nothing
        }

        @Override
        int size()
        {
            return 0;
        }
    }

    private class DataBlock extends Block
    {
        private final char[] data;

        DataBlock(long startIndex, char[] data)
        {
            super(startIndex, startIndex+data.length);
            this.data = data;
            BufferedReader.this.capacityUsed.addAndGet(data.length);
        }

        CharCursor openCursor()
        {
            throw new IllegalStateException("Cannot open cursor once reading has begun");
        }

        @Override
        int charAt(long index)
        {
            if (index < startIndex)
            {
                throw new IndexOutOfBoundsException("Block starting from " + startIndex + " cannot access " + index);
            }
            else if (index < endIndex)
            {
                return data[(int) (index - startIndex)];
            }
            else
            {
                return nextBlock().charAt(index);
            }
        }

        @Override
        void copyChars(long start, long end, char[] into, int intoIndex)
        {
            System.arraycopy(data, (int) (start - startIndex), into, intoIndex, (int) (end - start));
        }

        @Override
        boolean isEndOfData()
        {
            return false;
        }

        @Override
        int size()
        {
            return data.length;
        }
    }

    private class EndBlock extends Block
    {

        EndBlock(long eodIndex)
        {
            super(eodIndex, eodIndex);
        }

        CharCursor openCursor()
        {
            throw new IllegalStateException("Cannot open cursor once reading has begun");
        }

        @Override
        int charAt(long index)
        {
            return CharCursor.END_OF_DATA;
        }

        @Override
        void copyChars(long start, long end, char[] into, int intoIndex)
        {
            // Do nothing
        }

        @Override
        boolean isEndOfData()
        {
            return true;
        }

        @Override
        int size()
        {
            return 0;
        }
    }
}
