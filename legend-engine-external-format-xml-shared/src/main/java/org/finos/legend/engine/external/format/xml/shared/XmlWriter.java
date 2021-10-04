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

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.Arrays;

public class XmlWriter
{
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newFactory();
    private static final int NO_INDENTING = -1;
    private static final char[] SPACES = new char[1000];
    static
    {
        Arrays.fill(SPACES, ' ');
    }

    private final XMLStreamWriter writer;
    private final int indent;
    private int depth;
    private boolean lastWasIndent;

    private XmlWriter(XMLStreamWriter writer, int indent)
    {
        this.writer = writer;
        this.indent = indent;
    }

    public void writeStartElement(String localName)
    {
        indent();
        try
        {
            writer.writeStartElement(localName);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeStartElement(String namespaceURI, String localName)
    {
        indent();
        try
        {
            writer.writeStartElement(namespaceURI, localName);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeStartElement(String prefix, String localName, String namespaceURI)
    {
        indent();
        try
        {
            writer.writeStartElement(prefix, localName, namespaceURI);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEmptyElement(String namespaceURI, String localName)
    {
        try
        {
            indent();
            writer.writeEmptyElement(namespaceURI, localName);
            outdent();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEmptyElement(String prefix, String localName, String namespaceURI)
    {
        try
        {
            indent();
            writer.writeEmptyElement(prefix, localName, namespaceURI);
            outdent();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEmptyElement(String localName)
    {
        try
        {
            indent();
            writer.writeEmptyElement(localName);
            outdent();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEndElement()
    {
        try
        {
            outdent();
            writer.writeEndElement();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEndDocument()
    {
        try
        {
            writer.writeEndDocument();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void close()
    {
        try
        {
            writer.close();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void flush()
    {
        try
        {
            writer.flush();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeAttribute(String localName, String value)
    {
        try
        {
            writer.writeAttribute(localName, value);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeAttribute(String prefix, String namespaceURI, String localName, String value)
    {
        try
        {
            writer.writeAttribute(prefix, namespaceURI, localName, value);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeAttribute(String namespaceURI, String localName, String value)
    {
        try
        {
            writer.writeAttribute(namespaceURI, localName, value);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeNamespace(String prefix, String namespaceURI)
    {
        try
        {
            writer.writeNamespace(prefix, namespaceURI);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeDefaultNamespace(String namespaceURI)
    {
        try
        {
            writer.writeDefaultNamespace(namespaceURI);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeComment(String data)
    {
        try
        {
            writer.writeComment(data);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeProcessingInstruction(String target)
    {
        try
        {
            writer.writeProcessingInstruction(target);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeProcessingInstruction(String target, String data)
    {
        try
        {
            writer.writeProcessingInstruction(target, data);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeCData(String data)
    {
        try
        {
            writer.writeCData(data);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeDTD(String dtd)
    {
        try
        {
            writer.writeDTD(dtd);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeEntityRef(String name)
    {
        try
        {
            writer.writeEntityRef(name);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeStartDocument()
    {
        try
        {
            writer.writeStartDocument();
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeStartDocument(String version)
    {
        try
        {
            writer.writeStartDocument(version);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeStartDocument(String encoding, String version)
    {
        try
        {
            writer.writeStartDocument(encoding, version);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeCharacters(String text)
    {
        try
        {
            writer.writeCharacters(text);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void writeCharacters(char[] text, int start, int len)
    {
        try
        {
            writer.writeCharacters(text, start, len);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getPrefix(String uri)
    {
        try
        {
            return writer.getPrefix(uri);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setPrefix(String prefix, String uri)
    {
        try
        {
            writer.setPrefix(prefix, uri);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setDefaultNamespace(String uri)
    {
        try
        {
            writer.writeDefaultNamespace(uri);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setNamespaceContext(NamespaceContext context)
    {
        try
        {
            writer.setNamespaceContext(context);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public NamespaceContext getNamespaceContext()
    {
        return writer.getNamespaceContext();
    }

    public Object getProperty(String name)
    {
        return writer.getProperty(name);
    }

    private void indent()
    {
        if (indent != NO_INDENTING)
        {
            writeCharacters("\n");
            writeCharacters(SPACES, 0, depth*indent);
        }
        lastWasIndent = true;
        depth++;
    }

    private void outdent()
    {
        depth--;
        if (indent != NO_INDENTING)
        {
            if (!lastWasIndent)
            {
                writeCharacters("\n");
                writeCharacters(SPACES, 0, depth*indent);
            }
        }
        lastWasIndent = false;
    }

    public static XmlWriter newWriter(OutputStream stream)
    {
        try
        {
            return new XmlWriter(XML_OUTPUT_FACTORY.createXMLStreamWriter(stream), NO_INDENTING);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static XmlWriter newIndentingWriter(OutputStream stream)
    {
        return newIndentingWriter(stream, 2);
    }

    public static XmlWriter newIndentingWriter(OutputStream stream, int indentSize)
    {
        if (indentSize < 0)
        {
            throw new IllegalArgumentException("Indent size cannot be negative");
        }
        try
        {
            return new XmlWriter(XML_OUTPUT_FACTORY.createXMLStreamWriter(stream), indentSize);
        }
        catch (XMLStreamException e)
        {
            throw new RuntimeException(e);
        }
    }
}
