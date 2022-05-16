// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.inMemory.plugin;

import org.finos.legend.engine.plan.dependencies.domain.dataQuality.Constrained;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.store.inMemory.IStoreStreamReader;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public abstract class StoreStreamReadingObjectsIterator<T> implements Iterator<T>
{
    protected final IStoreStreamReader storeStreamReader;
    protected final Queue<IChecked<?>> queue = new LinkedList<>();
    protected final Integer recordsToBeRead;
    protected Integer recordsRead = 0;

    public StoreStreamReadingObjectsIterator(IStoreStreamReader storeStreamReader, Integer recordsToBeRead)
    {
        this.storeStreamReader = storeStreamReader;
        this.storeStreamReader.initReading();
        this.recordsToBeRead = recordsToBeRead;
    }

    public boolean hasNext()
    {
        if (recordsToBeRead != null && recordsRead >= recordsToBeRead)
        {
            this.close();
            return false;
        }

        if (this.queue.peek() == null && !this.storeStreamReader.isFinished())
        {
            this.queue.addAll(this.storeStreamReader.readCheckedObjects());
        }
        if (this.queue.peek() == null)
        {
            this.close();
        }

        return this.queue.peek() != null;
    }

    public void close()
    {
        this.storeStreamReader.destroyReading();
    }

    public static StoreStreamReadingObjectsIterator<?> newObjectsIterator(IStoreStreamReader storeStreamReader, boolean enableConstraints, boolean checked, Integer recordsToBeRead)
    {
        return checked ?
                enableConstraints ?
                        new CheckedObjectsIteratorWithConstraintsEnabled<>(storeStreamReader, recordsToBeRead) :
                        new CheckedObjectsIteratorWithConstraintsDisabled<>(storeStreamReader, recordsToBeRead) :
                enableConstraints ?
                        new ObjectsIteratorWithConstraintsEnabled<>(storeStreamReader, recordsToBeRead) :
                        new ObjectsIteratorWithConstraintsDisabled<>(storeStreamReader, recordsToBeRead);
    }

    private static class CheckedObjectsIteratorWithConstraintsEnabled<T> extends StoreStreamReadingObjectsIterator<IChecked<T>>
    {
        private CheckedObjectsIteratorWithConstraintsEnabled(IStoreStreamReader storeStreamReader, Integer recordsToBeRead)
        {
            super(storeStreamReader, recordsToBeRead);
        }

        @Override
        public IChecked<T> next()
        {
            if (!super.hasNext())
            {
                throw new NoSuchElementException("End of stream has passed");
            }

            IChecked<?> next = super.queue.remove();
            recordsRead++;
            if (next.getValue() != null && next.getValue() instanceof Constrained)
            {
                List<IDefect> defects = new ArrayList<>(next.getDefects());
                defects.addAll(((Constrained<?>) next.getValue()).allConstraints());

                return new IChecked<T>()
                {
                    @Override
                    public List<IDefect> getDefects()
                    {
                        return defects;
                    }

                    @Override
                    public Object getSource()
                    {
                        return next.getSource();
                    }

                    @Override
                    public T getValue()
                    {
                        return (T) next.getValue();
                    }
                };
            }
            return (IChecked<T>) next;
        }
    }

    private static class CheckedObjectsIteratorWithConstraintsDisabled<T> extends StoreStreamReadingObjectsIterator<IChecked<T>>
    {
        private CheckedObjectsIteratorWithConstraintsDisabled(IStoreStreamReader storeStreamReader, Integer recordsToBeRead)
        {
            super(storeStreamReader, recordsToBeRead);
        }

        @Override
        public IChecked<T> next()
        {
            if (!super.hasNext())
            {
                throw new NoSuchElementException("End of stream has passed");
            }
            IChecked<T> next = (IChecked<T>) super.queue.remove();
            recordsRead++;
            return next;
        }
    }

    private static class ObjectsIteratorWithConstraintsEnabled<T> extends StoreStreamReadingObjectsIterator<T>
    {
        private ObjectsIteratorWithConstraintsEnabled(IStoreStreamReader storeStreamReader, Integer recordsToBeRead)
        {
            super(storeStreamReader, recordsToBeRead);
        }

        @Override
        public T next()
        {
            if (!super.hasNext())
            {
                throw new NoSuchElementException("End of stream has passed");
            }

            IChecked<?> next = super.queue.remove();
            recordsRead++;
            StoreStreamReadingObjectsIterator.throwIfCheckedObjectIsNullOrDefective(next);

            if (next.getValue() instanceof Constrained)
            {
                return (T) ((Constrained<?>) next.getValue()).withConstraintsApplied();
            }
            return (T) next.getValue();
        }
    }

    private static class ObjectsIteratorWithConstraintsDisabled<T> extends StoreStreamReadingObjectsIterator<T>
    {
        private ObjectsIteratorWithConstraintsDisabled(IStoreStreamReader storeStreamReader, Integer recordsToBeRead)
        {
            super(storeStreamReader, recordsToBeRead);
        }

        @Override
        public T next()
        {
            if (!super.hasNext())
            {
                throw new NoSuchElementException("End of stream has passed");
            }

            IChecked<?> next = super.queue.remove();
            recordsRead++;
            StoreStreamReadingObjectsIterator.throwIfCheckedObjectIsNullOrDefective(next);
            return (T) next.getValue();
        }
    }

    private static void throwIfCheckedObjectIsNullOrDefective(IChecked<?> next)
    {
        if (!next.getDefects().isEmpty())
        {
            throw new IllegalStateException(next.getDefects().stream().map(IDefect::getMessage).filter(Objects::nonNull).collect(Collectors.joining("\n")));
        }
        else if (next.getValue() == null)
        {
            throw new IllegalStateException("Unexpected error: no object and no defects");
        }
    }
}
