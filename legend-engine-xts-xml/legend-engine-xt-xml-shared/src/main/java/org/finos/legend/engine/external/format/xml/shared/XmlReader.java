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

package org.finos.legend.engine.external.format.xml.shared;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.shared.core.util.LimitedByteArrayOutputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class XmlReader
{
    private static final int EVENT_BUFFER_SIZE = 4096;
    private static final int DEFAULT_CAPTURE_CAPACITY = 4096;

    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newFactory();
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();

    private final XMLEventReader reader;
    private final Function<XMLStreamException, ? extends RuntimeException> exceptionHandler;
    private final int captureCapacity;

    private EventBuffer eventBuffer;
    private ReadState currentState;

    private XmlReader(XMLEventReader reader, Function<XMLStreamException, ? extends RuntimeException> exceptionHandler)
    {
        this(reader, exceptionHandler, DEFAULT_CAPTURE_CAPACITY);
    }

    private XmlReader(XMLEventReader reader, Function<XMLStreamException, ? extends RuntimeException> exceptionHandler, int captureCapacity)
    {
        this.reader = reader;
        this.exceptionHandler = exceptionHandler;
        this.captureCapacity = captureCapacity;
        this.eventBuffer = new EventBuffer();
        this.currentState = new NoTransaction();
    }

    public boolean isStartDocument()
    {
        return currentEvent().isStartDocument();
    }

    public boolean isEndDocument()
    {
        return currentEvent().isEndDocument();
    }

    public boolean isStartElement()
    {
        return currentEvent().isStartElement();
    }

    public boolean isStartElement(QName name)
    {
        return isStartElement() && name.equals(getName());
    }

    public boolean isStartElement(String namespace, String localPart)
    {
        return isStartElement() && namespace.equals(getName().getNamespaceURI()) && localPart.equals(getName().getLocalPart());
    }

    public boolean isStartElementLenient(String localPart)
    {
        return isStartElement() && XmlUtils.lenientMatch(localPart, getName().getLocalPart());
    }

    public boolean isXsiNil()
    {
        if (isStartElement())
        {
            String nil = getAttributeValue(XmlUtils.XSI_NIL);
            return nil != null && (nil.equals("true") || nil.equals("1"));
        }
        else
        {
            return false;
        }
    }

    public boolean isEndElement()
    {
        return currentEvent().isEndElement();
    }

    public boolean isEndElement(QName name)
    {
        return isEndElement() && name.equals(getName());
    }

    public boolean isEndElement(String namespace, String localPart)
    {
        return isEndElement() && namespace.equals(getName().getNamespaceURI()) && localPart.equals(getName().getLocalPart());
    }

    public QName getName()
    {
        return currentEvent().isStartElement() ? currentEvent().asStartElement().getName() : currentEvent().asEndElement().getName();
    }

    public String getElementText()
    {
        StringBuilder text = new StringBuilder();
        next();

        while (hasNext() && !isEndElement())
        {
            if (isStartElement() || isEndDocument())
            {
                throw exceptionHandler.apply(new XMLStreamException("Unexpected element in text content", currentElement().getLocation()));
            }
            if (currentEvent().isCharacters())
            {
                Characters characters = currentEvent().asCharacters();
                text.append(characters.getData());
            }
            next();
        }

        return text.toString();
    }

    public String getAttributeValue(QName name)
    {
        return getAttributeValue(name.getNamespaceURI(), name.getLocalPart());
    }

    public String getAttributeValue(String localPart)
    {
        return getAttributeValue(null, localPart);
    }

    public String getAttributeValue(String namespace, String localPart)
    {
        for (Iterator<Attribute> attributes = currentElement().getAttributes(); attributes.hasNext(); )
        {
            Attribute attribute = attributes.next();
            if ((namespace == null || namespace.equals(attribute.getName().getNamespaceURI())) && localPart.equals(attribute.getName().getLocalPart()))
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    public String getAttributeValueLenient(String attributeName)
    {
        for (Iterator<Attribute> attributes = currentElement().getAttributes(); attributes.hasNext(); )
        {
            Attribute attribute = attributes.next();
            if (XmlUtils.lenientMatch(attributeName, attribute.getName().getLocalPart()))
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    public String getAttributeValueOrDefault(QName name, String dflt)
    {
        String value = getAttributeValue(name);
        return value == null ? dflt : value;
    }

    public String getAttributeValueOrDefault(String localPart, String dflt)
    {
        String value = getAttributeValue(localPart);
        return value == null ? dflt : value;
    }

    public String getAttributeValueOrDefault(String namespace, String localPart, String dflt)
    {
        String value = getAttributeValue(namespace, localPart);
        return value == null ? dflt : value;
    }

    public String getAttributeValueLenientOrDefault(String attributeName, String dflt)
    {
        String value = getAttributeValueLenient(attributeName);
        return value == null ? dflt : value;
    }

    public boolean hasAttribute(QName name)
    {
        return hasAttribute(name.getNamespaceURI(), name.getNamespaceURI());
    }

    public boolean hasAttribute(String namespace, String localPart)
    {
        for (Iterator<Attribute> attributes = currentElement().getAttributes(); attributes.hasNext(); )
        {
            Attribute attribute = attributes.next();
            if (namespace == null || namespace.equals(attribute.getName().getNamespaceURI()) && localPart.equals(attribute.getName().getLocalPart()))
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasAttributeLenient(String attributeName)
    {
        return resolveLenientAttributeName(attributeName) != null;
    }

    public String resolveLenientAttributeName(String attributeName)
    {
        for (Iterator<Attribute> attributes = currentElement().getAttributes(); attributes.hasNext(); )
        {
            Attribute attribute = attributes.next();
            if (XmlUtils.lenientMatch(attributeName, attribute.getName().getLocalPart()))
            {
                return attribute.getName().toString();
            }
        }
        return null;
    }

    public void skipElement()
    {
        if (!isStartElement())
        {
            throw new IllegalStateException("Cannot goto end of element");
        }
        QName name = getName();
        int depth = 0;
        while (depth > 0 || !isEndElement(name))
        {
            nextTag();
            if (isStartElement(name))
            {
                depth++;
            }
            else if (isEndElement(name))
            {
                depth--;
            }
        }
        nextTag();
    }

    public void nextTag()
    {
        int event = next();
        while (event != XMLStreamConstants.END_DOCUMENT && event != XMLStreamConstants.START_ELEMENT && event != XMLStreamConstants.END_ELEMENT)
        {
            event = next();
        }
    }

    public Supplier<NamespaceContext> getNamespaceContextSupplier()
    {
        return () -> currentState.lastStartElement.asStartElement().getNamespaceContext();
    }

    public void close()
    {
        try
        {
            reader.close();
        }
        catch (XMLStreamException e)
        {
            throw exceptionHandler.apply(e);
        }
    }

    public Location getLocation()
    {
        return currentEvent().getLocation();
    }

    public String describe()
    {
        StringBuilder builder = new StringBuilder();
        switch (currentEvent().getEventType())
        {
            case XMLStreamConstants.START_ELEMENT:
                builder.append("START_ELEMENT ").append(getName());
                break;
            case XMLStreamConstants.END_ELEMENT:
                builder.append("END_ELEMENT ").append(getName());
                break;
            case XMLStreamConstants.CHARACTERS:
                Characters characters = currentEvent().asCharacters();
                if (characters.isWhiteSpace())
                {
                    builder.append("WHITESPACE");
                }
                else if (characters.getData().length() > 50)
                {
                    builder.append("CHARACTERS ").append(characters.getData(), 0, 50).append("...");
                }
                else
                {
                    builder.append("CHARACTERS ").append(characters.getData());
                }
                break;
            case XMLStreamConstants.ATTRIBUTE:
                builder.append("ATTRIBUTE");
                break;
            case XMLStreamConstants.NAMESPACE:
                builder.append("NAMESPACE");
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                builder.append("PROCESSING_INSTRUCTION");
                break;
            case XMLStreamConstants.COMMENT:
                builder.append("COMMENT");
                break;
            case XMLStreamConstants.START_DOCUMENT:
                builder.append("START_DOCUMENT");
                break;
            case XMLStreamConstants.END_DOCUMENT:
                builder.append("END_DOCUMENT");
                break;
            case XMLStreamConstants.DTD:
                builder.append("DTD");
                break;
            default:
                builder.append("UNKNOWN: ").append(currentEvent().getEventType());
        }
        Location location = currentEvent().getLocation();
        builder.append(" @ ").append(location.getLineNumber()).append(":").append(location.getColumnNumber());
        return builder.toString();
    }

    public String getRawElementContents()
    {
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            XMLEventWriter writer = XML_OUTPUT_FACTORY.createXMLEventWriter(bytes);
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            writer.setNamespaceContext(currentElement().getNamespaceContext());
            next();
            int depth = 0;
            writer.add(eventFactory.createStartElement("", "", "WRAPPER"));
            while (hasNext() && !(depth == 0 && isEndElement()))
            {
                switch (currentEvent().getEventType())
                {
                    case XMLStreamConstants.START_ELEMENT:
                        depth++;
                        writer.add(currentEvent());
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        depth--;
                        writer.add(currentEvent());
                        break;
                    case XMLStreamConstants.CHARACTERS:
                    case XMLStreamConstants.COMMENT:
                        writer.add(currentEvent());
                        break;
                }
                next();
            }
            writer.add(eventFactory.createEndElement("", "", "WRAPPER"));
            String s = bytes.toString("UTF-8");

            return s.replace("<WRAPPER>", "").replace("</WRAPPER>", "").replace("<WRAPPER/>", "");
        }
        catch (XMLStreamException e)
        {
            throw exceptionHandler.apply(e);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getRawElementTextContents()
    {
        StringBuilder text = new StringBuilder();

        int depth = 0;
        while (hasNext() && !(depth == 0 && isEndElement()))
        {
            switch (currentEvent().getEventType())
            {
                case XMLStreamConstants.START_ELEMENT:
                    depth++;
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    depth--;
                    break;
                case XMLStreamConstants.CHARACTERS:
                    Characters characters = currentEvent().asCharacters();
                    text.append(characters.getData());
                    break;
            }
            next();
        }

        return text.toString();
    }

    public Transaction newTransaction()
    {
        currentState = new Transaction(currentState);
        return (Transaction) currentState;
    }

    public void startCapture()
    {
        currentState.startCapture();
    }

    public String endCapture()
    {
        return currentState.endCapture();
    }

    public boolean hasNext()
    {
        return currentState.hasNext();
    }

    public int next()
    {
        currentState.advance();
        return currentEvent().getEventType();
    }

    private XMLEvent currentEvent()
    {
        return currentState.currentEvent;
    }

    private StartElement currentElement()
    {
        return currentEvent().asStartElement();
    }

    private class EventBuffer
    {
        private XMLEvent[] events = new XMLEvent[EVENT_BUFFER_SIZE];
        private long low;
        private long high;
        private long max;

        EventBuffer()
        {
            if (reader.hasNext())
            {
                max = Long.MAX_VALUE;
                high = -1;
                moveTo(0);
            }
            else
            {
                max = -1;
            }
        }

        boolean hasEvent(long index)
        {
            fillTo(index);
            return index <= max;
        }

        XMLEvent peek(long index)
        {
            fillTo(index);
            if (index > max)
            {
                throw new IllegalStateException("Attempting to read beyond end of XML events");
            }
            return events[(int) (index % EVENT_BUFFER_SIZE)];
        }

        XMLEvent moveTo(long index)
        {
            low = index;
            return peek(index);
        }

        void fillTo(long index)
        {
            while (index > high && index < max)
            {
                if ((high - low) >= EVENT_BUFFER_SIZE)
                {
                    throw new IllegalStateException("Cannot buffer more than " + EVENT_BUFFER_SIZE + " XML events");
                }
                try
                {
                    high++;
                    events[(int) (high % EVENT_BUFFER_SIZE)] = reader.nextEvent();
                    if (!reader.hasNext())
                    {
                        max = high;
                    }
                }
                catch (XMLStreamException e)
                {
                    throw exceptionHandler.apply(e);
                }
            }
        }
    }

    private static class Capture
    {
        private XMLEventWriter captureWriter;
        private ByteArrayOutputStream captureBytes;
        private List<XMLEvent> captureDeferredWhitespace;
        private boolean capturingText;

        Capture(int capacity)
        {
            try
            {
                captureBytes = new LimitedByteArrayOutputStream(capacity);
                captureWriter = XML_OUTPUT_FACTORY.createXMLEventWriter(captureBytes);
                captureDeferredWhitespace = Lists.mutable.empty();
                capturingText = false;
            }
            catch (XMLStreamException e)
            {
                throw new RuntimeException(e);
            }
        }

        String finish()
        {
            try
            {
                captureWriter.flush();
                return captureBytes.toString("UTF-8");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        void add(XMLEvent event)
        {
            try
            {
                if (event.isStartElement() || event.isEndElement())
                {
                    captureWriter.add(event);
                    captureDeferredWhitespace.clear();
                    capturingText = false;
                }
                else if (event.isCharacters())
                {
                    Characters characters = event.asCharacters();
                    if (!capturingText && characters.isWhiteSpace())
                    {
                        captureDeferredWhitespace.add(characters);
                    }
                    else
                    {
                        for (XMLEvent deferred : captureDeferredWhitespace)
                        {
                            captureWriter.add(deferred);
                        }
                        captureWriter.add(event);
                        captureDeferredWhitespace.clear();
                        capturingText = true;
                    }
                }
            }
            catch (XMLStreamException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private abstract class ReadState
    {
        long index = -1;
        XMLEvent currentEvent;
        Capture capture;
        XMLEvent lastStartElement;

        abstract void advance();

        void setCurrentEvent(XMLEvent event)
        {
            if (capture != null)
            {
                capture.add(currentEvent);
            }
            this.currentEvent = event;
            if (event.isStartElement())
            {
                this.lastStartElement = event;
            }
        }

        void advanceTo(long index)
        {
            while (this.index < index)
            {
                advance();
            }
        }

        boolean hasNext()
        {
            return eventBuffer.hasEvent(index + 1);
        }

        void startCapture()
        {
            if (capture != null)
            {
                throw new IllegalStateException("Nested captures not supported");
            }
            capture = new Capture(captureCapacity);
        }

        String endCapture()
        {
            if (capture == null)
            {
                throw new IllegalStateException("No capture started");
            }
            String result = capture.finish();
            capture = null;
            return result;
        }
    }

    private class NoTransaction extends ReadState
    {
        NoTransaction()
        {
            advanceTo(0);
        }

        @Override
        void advance()
        {
            setCurrentEvent(eventBuffer.moveTo(++index));
        }
    }

    public class Transaction extends ReadState implements Closeable
    {
        private final ReadState parent;

        private Transaction(ReadState parent)
        {
            this.parent = parent;
            this.index = parent.index;
            this.currentEvent = parent.currentEvent;
        }

        @Override
        void advance()
        {
            setCurrentEvent(eventBuffer.peek(++index));
        }

        public void commit()
        {
            parent.advanceTo(index);
        }

        @Override
        public void close()
        {
            currentState = parent;
        }
    }

    public static XmlReader newReader(InputStream stream)
    {
        return newReader(new InputStreamReader(stream), (String) null);
    }

    public static XmlReader newReader(InputStream stream, String id)
    {
        return newReader(new InputStreamReader(stream), id);
    }

    public static XmlReader newReader(Reader reader)
    {
        return newReader(reader, (String) null);
    }

    public static XmlReader newReader(Reader reader, String id)
    {
        return newReader(reader, id, e ->
        {
            throw new RuntimeException(e.getMessage(), e);
        });
    }

    public static XmlReader newReader(Reader reader, Function<XMLStreamException, ? extends RuntimeException> exceptionHandler)
    {
        return newReader(reader, null, exceptionHandler);
    }

    public static XmlReader newReader(Reader reader, String id, Function<XMLStreamException, ? extends RuntimeException> exceptionHandler)
    {
        try
        {
            return new XmlReader(XML_INPUT_FACTORY.createXMLEventReader(id, reader), exceptionHandler);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }
}
