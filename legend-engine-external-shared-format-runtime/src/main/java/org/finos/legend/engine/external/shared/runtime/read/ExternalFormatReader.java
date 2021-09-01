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

package org.finos.legend.engine.external.shared.runtime.read;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.engine.shared.core.url.StreamProviderHolder;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class ExternalFormatReader<T> implements Iterator<IChecked<T>>
{
    private static final long FIVE_MINUTES = 5L * 60L * 1000L;
    private static int threadId = 0;

    private BlockingQueue<IChecked<T>> queue = new ArrayBlockingQueue<>(256, true);
    private AtomicReference<Exception> readException = new AtomicReference<>();
    private AtomicBoolean readingComplete = new AtomicBoolean(false);
    private StreamProvider streamProvider;

    @Override
    public boolean hasNext()
    {
        checkReader();
        long t0 = System.currentTimeMillis();
        while (queue.isEmpty() && !readingComplete.get())
        {
            checkReader();
            if (System.currentTimeMillis() -t0 > FIVE_MINUTES)
            {
                throw new IllegalStateException("Data unavailable for more than 5 minutes");
            }
            // busy wait because there's no time out variant of peek
            sleep(100);
        }
        return !queue.isEmpty();
    }

    @Override
    public IChecked<T> next()
    {
        // Fast path should always work if next() is called first
        if (!queue.isEmpty())
        {
            return queue.remove();
        }

        // Slow path in case next() is not called first
        checkReader();
        long t0 = System.currentTimeMillis();
        IChecked<T> obj = poll(100);
        while (obj == null && !readingComplete.get())
        {
            checkReader();
            if (System.currentTimeMillis() -t0 > FIVE_MINUTES)
            {
                throw new IllegalStateException("Data unavailable for more than 5 minutes");
            }
            obj = poll(100);
        }
        if (obj == null)
        {
            throw new NoSuchElementException();
        }
        return obj;
    }

    public Stream<IChecked<T>>  startStream()
    {
        streamProvider = StreamProviderHolder.streamProviderThreadLocal.get();
        Thread thread = new Thread(this::readData, "ExternalFormatDeserializer-"+(++threadId));
        thread.start();

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, (Spliterator.ORDERED) | Spliterator.IMMUTABLE | Spliterator.NONNULL), false);
    }

    private void checkReader()
    {
        if (readException.get() != null)
        {
            throw readException.get() instanceof RuntimeException
                  ? (RuntimeException) readException.get()
                  : new RuntimeException(readException.get());
        }
    }

    private IChecked<T> poll(int waitMs)
    {
        try
        {
            return queue.poll(waitMs, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            // Ignore -  We should really handle the interruption here, but it caused issues to client code
            // that we haven't been able to track down so leaving this as return null for now.
            return null;
        }
    }

    private void sleep(int ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException e)
        {
            // Ignore - will time out in due course
        }
    }

    private void enqueue(IChecked<T> object)
    {
        try
        {
            queue.put(object);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void readData()
    {
        if (streamProvider != null)
        {
            StreamProviderHolder.streamProviderThreadLocal.set(streamProvider);
        }

        try
        {
            readData(this::enqueue);
            readingComplete.set(true);
        }
        catch (Exception e)
        {
            readException.set(e);
        }
    }

    protected abstract void readData(Consumer<IChecked<T>> consumer);
}

