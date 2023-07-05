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

package org.finos.legend.engine.external.format.xml.read;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.finos.legend.engine.external.format.xml.shared.XmlUtils;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BooleanSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.BuiltInDataTypes;
import org.finos.legend.engine.external.format.xml.shared.datatypes.DoubleSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.LongSimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypeHandler;
import org.finos.legend.engine.external.format.xml.shared.datatypes.SimpleTypesContext;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataBooleanAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataDoubleAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataLongAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalData;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicRelativePathNode;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.RelativePathNode;

import javax.xml.namespace.QName;
import java.io.Closeable;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DeserializeContext<CLS>
{
    public final XmlReader reader;
    public final SimpleTypesContext simpleTypesContext;
    private final String schemaElement;
    private final Consumer<IChecked<CLS>> consumer;
    private Frame currentFrame = new DocumentFrame();
    private long recordNumber = 0;

    private ErrorHandlingType unexpectedElementHandling = ErrorHandlingType.WARNING;
    private ErrorHandlingType insufficientOccurrencesHandling = ErrorHandlingType.FAIL;

    public DeserializeContext(XmlReader reader, Consumer<IChecked<CLS>> consumer)
    {
        this(reader, consumer, null);
    }

    public DeserializeContext(XmlReader reader, Consumer<IChecked<CLS>> consumer, String schemaElement)
    {
        this.reader = reader;
        this.consumer = consumer;
        this.simpleTypesContext = new SimpleTypesContext(reader.getNamespaceContextSupplier());
        this.schemaElement = schemaElement;
    }

    public void setUnexpectedElementHandling(ErrorHandlingType handlingType)
    {
        this.unexpectedElementHandling = handlingType;
    }

    public ErrorHandlingType getUnexpectedElementHandling()
    {
        return unexpectedElementHandling;
    }

    public ErrorHandlingType getInsufficientOccurrencesHandling()
    {
        return insufficientOccurrencesHandling;
    }

    public void setInsufficientOccurrencesHandling(ErrorHandlingType insufficientOccurrencesHandling)
    {
        this.insufficientOccurrencesHandling = insufficientOccurrencesHandling;
    }

    public void pushPathElement(QName name)
    {
        currentFrame.pushPathElement(name);
    }

    public void popPathElement()
    {
        currentFrame.popPathElement();
    }

    public String getPath()
    {
        return currentFrame.pathElement.getPath();
    }

    public void startDataObject(IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        currentFrame = (addToParent == null)
                ? new RecordFrame(currentFrame, dataFactory)
                : new ChildObjectFrame(currentFrame, dataFactory, addToParent);
        currentFrame.start();
    }

    public void finishDataObject()
    {
        currentFrame = currentFrame.finish();
    }

    public void addValue(ExternalDataObjectAdder adder, Object value)
    {
        currentFrame.addValue(adder, value);
    }

    public void addValue(ExternalDataBooleanAdder adder, boolean value)
    {
        currentFrame.addValue(adder, value);
    }

    public void addValue(ExternalDataLongAdder adder, long value)
    {
        currentFrame.addValue(adder, value);
    }

    public void addValue(ExternalDataDoubleAdder adder, double value)
    {
        currentFrame.addValue(adder, value);
    }

    public void addWarningDefect(String msg)
    {
        currentFrame.addWarningDefect(msg);
    }

    public void addErrorDefect(String msg)
    {
        currentFrame.addErrorDefect(msg);
    }

    public void addCriticalDefect(String msg)
    {
        currentFrame.addCriticalDefect(msg);
    }

    public BooleanSimpleTypeHandler booleanSimpleTypeHandler()
    {
        return (BooleanSimpleTypeHandler) simpleTypesContext.<Boolean>handler(BuiltInDataTypes.XS_BOOLEAN);
    }

    public LongSimpleTypeHandler longSimpleTypeHandler()
    {
        return (LongSimpleTypeHandler) simpleTypesContext.<Long>handler(BuiltInDataTypes.XS_LONG);
    }

    public DoubleSimpleTypeHandler doubleSimpleTypeHandler()
    {
        return (DoubleSimpleTypeHandler) simpleTypesContext.<Long>handler(BuiltInDataTypes.XS_DOUBLE);
    }

    public SimpleTypeHandler<String> stringSimpleTypeHandler()
    {
        return simpleTypesContext.<String>handler(BuiltInDataTypes.XS_STRING);
    }

    public SimpleTypeHandler<Temporal> dateSimpleTypeHandler()
    {
        return simpleTypesContext.<Temporal>handler(BuiltInDataTypes.XS_DATE);
    }

    public SimpleTypeHandler<Temporal> dateTimeSimpleTypeHandler()
    {
        return simpleTypesContext.<Temporal>handler(BuiltInDataTypes.XS_DATE_TIME);
    }

    public SimpleTypeHandler<BigDecimal> decimalSimpleTypeHandler()
    {
        return simpleTypesContext.<BigDecimal>handler(BuiltInDataTypes.XS_DECIMAL);
    }

    public Transaction newTransaction()
    {
        currentFrame = new TransactionFrame(currentFrame);
        return new Transaction((TransactionFrame) currentFrame);
    }

    public class Transaction implements Closeable
    {

        private final DeserializeContext.TransactionFrame transactionFrame;
        private final XmlReader.Transaction readerTransaction;

        public Transaction(DeserializeContext.TransactionFrame transactionFrame)
        {
            this.transactionFrame = transactionFrame;
            this.readerTransaction = reader.newTransaction();
        }

        public void commit()
        {
            transactionFrame.commit();
            readerTransaction.commit();
        }

        @Override
        public void close()
        {
            readerTransaction.close();
            currentFrame = transactionFrame.finish();
        }
    }

    private static class PathElement
    {
        private final long index;
        private final QName name;
        private final PathElement parent;
        private final Map<QName, Long> indexes = new HashMap<>();

        PathElement()
        {
            this.parent = null;
            this.index = -1;
            this.name = null;
        }

        PathElement(PathElement proto)
        {
            this.parent = proto.parent;
            this.index = proto.index;
            this.name = proto.name;
            this.indexes.putAll(proto.indexes);
        }

        private PathElement(PathElement parent, long index, QName name)
        {
            this.parent = parent;
            this.index = index;
            this.name = name;
        }

        PathElement resolve(QName name)
        {
            long index = indexes.getOrDefault(name, 0L) + 1;
            indexes.put(name, index);
            PathElement result = new PathElement(this, index, name);
            return result;
        }

        String getPath()
        {
            return (parent == null)
                    ? ""
                    : parent.getPath() + "/" + XmlUtils.toShortString(name) + "[" + index + "]";
        }

        PathElement getParent()
        {
            return parent;
        }
    }

    abstract class Frame
    {
        PathElement pathElement;
        private final List<IDefect> defects = Lists.mutable.empty();

        abstract Frame start();

        abstract Frame finish();

        abstract Object get();

        abstract String getPureClassName();

        abstract long propertySize(String propertyName);

        List<IDefect> getDefects()
        {
            return defects;
        }

        abstract void addValue(ExternalDataObjectAdder adder, Object value);

        abstract void addValue(ExternalDataBooleanAdder adder, boolean value);

        abstract void addValue(ExternalDataLongAdder adder, long value);

        abstract void addValue(ExternalDataDoubleAdder adder, double value);


        void addWarningDefect(String msg)
        {
            addDefect(BasicDefect.newInvalidInputWarningDefect(msg, schemaElement == null ? getPureClassName() : schemaElement));
        }

        void addErrorDefect(String msg)
        {
            addDefect(BasicDefect.newInvalidInputErrorDefect(msg, schemaElement == null ? getPureClassName() : schemaElement));
        }

        void addCriticalDefect(String msg)
        {
            addDefect(BasicDefect.newInvalidInputCriticalDefect(msg, schemaElement == null ? getPureClassName() : schemaElement));
        }

        void addDefect(IDefect defect)
        {
            defects.add(defect);
        }

        void pushPathElement(QName name)
        {
            pathElement = pathElement.resolve(name);
        }

        void popPathElement()
        {
            pathElement = pathElement.getParent();
        }
    }

    class DocumentFrame extends Frame
    {
        DocumentFrame()
        {
            pathElement = new PathElement();
        }

        @Override
        Frame start()
        {
            return this;
        }

        @Override
        Frame finish()
        {
            return null;
        }

        @Override
        Object get()
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        String getPureClassName()
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        long propertySize(String propertyName)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addValue(ExternalDataObjectAdder adder, Object value)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addValue(ExternalDataBooleanAdder adder, boolean value)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addValue(ExternalDataLongAdder adder, long value)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addValue(ExternalDataDoubleAdder adder, double value)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addWarningDefect(String msg)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addErrorDefect(String msg)
        {
            throw new IllegalStateException("No data object in progress");
        }

        @Override
        void addCriticalDefect(String msg)
        {
            throw new IllegalStateException("No data object in progress");
        }
    }

    abstract class DataObjectFrame extends Frame
    {
        private final IExternalData object;
        private final String pureClassName;
        private final Map<String, AtomicLong> propertyCounts = Maps.mutable.empty();

        DataObjectFrame(IExternalDataFactory dataFactory)
        {
            this.object = dataFactory.newInstance();
            this.pureClassName = dataFactory.getPureClassName();
        }

        IExternalData get()
        {
            return object;
        }

        String getPureClassName()
        {
            return pureClassName;
        }

        long propertySize(String propertyName)
        {
            return propertyCounts.containsKey(propertyName) ? propertyCounts.get(propertyName).get() : 0L;
        }

        void addValue(ExternalDataObjectAdder adder, Object value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            adder.addTo(get(), value);
        }

        void addValue(ExternalDataBooleanAdder adder, boolean value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            adder.addTo(get(), value);
        }

        void addValue(ExternalDataLongAdder adder, long value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            adder.addTo(get(), value);
        }

        void addValue(ExternalDataDoubleAdder adder, double value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            adder.addTo(get(), value);
        }
    }

    class RecordFrame extends DataObjectFrame
    {
        private final Frame parent;

        RecordFrame(Frame parent, IExternalDataFactory dataFactory)
        {
            super(dataFactory);
            this.parent = parent;
            this.pathElement = parent.pathElement;
        }

        @Override
        Frame start()
        {
            recordNumber++;
            reader.startCapture();
            return this;
        }

        @Override
        Frame finish()
        {
            XmlDataRecord source = new XmlDataRecord(recordNumber, reader.endCapture());
            IChecked<CLS> checked = getDefects().stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical)
                    ? (IChecked<CLS>) BasicChecked.newChecked(null, source, getDefects())
                    : (IChecked<CLS>) BasicChecked.newChecked(get(), source, getDefects());
            consumer.accept(checked);
            return parent;
        }
    }

    class ChildObjectFrame extends DataObjectFrame
    {
        private final Frame parent;
        private final ExternalDataObjectAdder addToParent;
        private long index;

        ChildObjectFrame(Frame parent, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
        {
            super(dataFactory);
            this.parent = parent;
            this.addToParent = addToParent;
            this.pathElement = parent.pathElement;
        }

        @Override
        Frame start()
        {
            index = parent.propertySize(addToParent.getPropertyName());
            return this;
        }

        @Override
        Frame finish()
        {
            long index = parent.propertySize(addToParent.getPropertyName());
            parent.addValue(addToParent, get());

            get().checkMultiplicities().forEach(this::addDefect);
            for (IDefect defect : getDefects())
            {
                RelativePathNode path = BasicRelativePathNode.newRelativePathNode(addToParent.getPropertyName(), index);
                IDefect newDefect = BasicDefect.prefixPath(defect, path);
                parent.addDefect(newDefect);
            }
            return parent;
        }
    }

    class TransactionFrame extends Frame
    {
        private final Frame parent;
        private final List<IDefect> defects = Lists.mutable.empty();
        private final List<Runnable> deferredActions = Lists.mutable.empty();
        private final Map<String, AtomicLong> propertyCounts = Maps.mutable.empty();

        TransactionFrame(Frame parent)
        {
            this.parent = parent;
            this.pathElement = new PathElement(parent.pathElement);
        }

        @Override
        Frame start()
        {
            return this;
        }

        @Override
        Frame finish()
        {
            return parent;
        }

        void commit()
        {
            deferredActions.forEach(Runnable::run);
            parent.defects.addAll(this.defects);
            parent.pathElement = pathElement;
        }

        @Override
        Object get()
        {
            return parent.get();
        }

        @Override
        String getPureClassName()
        {
            return parent.getPureClassName();
        }

        @Override
        long propertySize(String propertyName)
        {
            return parent.propertySize(propertyName) + (propertyCounts.containsKey(propertyName) ? propertyCounts.get(propertyName).get() : 0L);
        }

        @Override
        void addValue(ExternalDataObjectAdder adder, Object value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            deferredActions.add(() -> parent.addValue(adder, value));
        }

        @Override
        void addValue(ExternalDataBooleanAdder adder, boolean value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            deferredActions.add(() -> parent.addValue(adder, value));
        }

        @Override
        void addValue(ExternalDataLongAdder adder, long value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            deferredActions.add(() -> parent.addValue(adder, value));
        }

        @Override
        void addValue(ExternalDataDoubleAdder adder, double value)
        {
            propertyCounts.computeIfAbsent(adder.getPropertyName(), name -> new AtomicLong(0)).getAndIncrement();
            deferredActions.add(() -> parent.addValue(adder, value));
        }
    }
}
