// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ObjectStreamConnection implements Connection
{
    private static final int CAPACITY = 128;
    private static final int BLOCK_SIZE = 32;

    private final Stream<?> stream;
    private ObjectCursor cursor;

    private AtomicReference<Block> lowBlock;
    private AtomicReference<Block> highBlock;
    private AtomicInteger capacityUsed = new AtomicInteger(0);
    private final Object lock = new Object();
    private Iterator<? extends Object> iterator;

    public ObjectStreamConnection(Stream<?> stream)
    {
        this.stream = stream;
    }

    @Override
    public void open() throws IOException
    {
        iterator = stream.iterator();
        StartBlock startBlock = new StartBlock();
        lowBlock = new AtomicReference<>(startBlock);
        highBlock = new AtomicReference<>(startBlock);
        cursor = startBlock.openCursor();
    }

    @Override
    public void close() throws IOException
    {
        // No Op
    }

    public ObjectCursor getCursor()
    {
        return cursor;
    }

    private class ConnectionCursor implements ObjectCursor
    {
        // Index of the next object to be consumed
        private long position;
        private Block block;
        private CursorState cursorState;

        ConnectionCursor(Block block)
        {
            this.position = 0;
            this.block = block;
            this.cursorState = new ActiveCursorState();
            block.addCursor();
        }

        private ConnectionCursor(ConnectionCursor copyOf)
        {
            this.position = copyOf.position;
            this.block = copyOf.block;
            this.cursorState = new ActiveCursorState();
            block.addCursor();
        }

        @Override
        public <T> T advance()
        {
            return (T) cursorState.advance();
        }

        @Override
        public ObjectCursor copy()
        {
            return new ConnectionCursor(this);
        }

        @Override
        public boolean isEndOfData()
        {
            return cursorState.isEndOfData();
        }

        @Override
        public void destroy()
        {
            cursorState.destroy();
            block.removeCursor();
        }

        private abstract class CursorState
        {
            abstract Object advance();

            abstract boolean isEndOfData();

            abstract void destroy();
        }

        private class ActiveCursorState extends CursorState
        {
            @Override
            public Object advance()
            {
                moveToNextBlockIfRequired();
                return block.get(position++);
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
            public Object advance()
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

        abstract Object get(long index);

        abstract boolean isEndOfData();

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
            return nextBlock;
        }

        private void createNextBlock()
        {
            synchronized (lock)
            {
                if (capacityUsed.get() >= CAPACITY)
                {
                    throw new IllegalStateException("Insufficient capacity to load more data");
                }

                if (nextBlock != null)
                {
                    return;
                }

                Block newBlock;
                if (iterator.hasNext())
                {
                    List<Object> objects = new ArrayList<>(BLOCK_SIZE);
                    while (iterator.hasNext() && objects.size() < BLOCK_SIZE)
                    {
                        objects.add(iterator.next());
                    }
                    newBlock = new DataBlock(endIndex, objects);
                }
                else
                {
                    newBlock = new EndBlock(endIndex);
                }
                nextBlock = newBlock;
                if (!highBlock.compareAndSet(this, newBlock))
                {
                    throw new Error("New block is not added to  end of chain");
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

        ObjectCursor openCursor()
        {
            return new ConnectionCursor(this);
        }

        @Override
        Object get(long index)
        {
            return nextBlock().get(index);
        }

        @Override
        boolean isEndOfData()
        {
            return false;
        }

        @Override
        int size()
        {
            return 0;
        }
    }

    private class DataBlock extends Block
    {
        private final List<Object> data;

        DataBlock(long startIndex, List<Object> data)
        {
            super(startIndex, startIndex + data.size());
            this.data = data;
            capacityUsed.addAndGet(data.size());
        }

        @Override
        boolean isEndOfData()
        {
            return false;
        }

        Object get(long index)
        {
            if (index < startIndex)
            {
                throw new IndexOutOfBoundsException("Block starting from " + startIndex + " cannot access " + index);
            }
            else if (index < endIndex)
            {
                return data.get((int) (index - startIndex));
            }
            else
            {
                return nextBlock().get(index);
            }
        }

        @Override
        int size()
        {
            return data.size();
        }
    }

    private class EndBlock extends Block
    {

        EndBlock(long eodIndex)
        {
            super(eodIndex, eodIndex);
        }

        @Override
        Object get(long index)
        {
            return null;
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
