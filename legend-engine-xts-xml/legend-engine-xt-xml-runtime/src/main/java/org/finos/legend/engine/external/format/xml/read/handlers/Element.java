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

package org.finos.legend.engine.external.format.xml.read.handlers;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.read.ReadHandler;
import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.finos.legend.engine.external.format.xml.shared.XmlUtils;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;

import javax.xml.namespace.QName;
import java.util.function.Predicate;

public class Element extends ReadHandler
{
    final long minOccurs;
    final long maxOccurs;
    final String description;
    final ExternalDataObjectAdder addToParent;
    final IExternalDataFactory dataFactory;
    final Predicate<XmlReader> matcher;
    final MutableList<Attribute> attributes = Lists.mutable.empty();
    TextContent textContent = null;
    Particle particle = null;

    Element(long minOccurs, long maxOccurs, Predicate<XmlReader> matcher, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent, String description)
    {
        this.minOccurs = requireValidOccurs(minOccurs);
        this.maxOccurs = requireValidOccurs(maxOccurs);
        checkOccursRange(minOccurs, maxOccurs);
        this.matcher = matcher;
        this.dataFactory = dataFactory;
        this.addToParent = addToParent;
        this.description = description;
    }

    public Element add(Attribute attribute)
    {
        attributes.add(attribute);
        return this;
    }

    public Element add(TextContent textContent)
    {
        if (this.particle != null)
        {
            throw new IllegalStateException("Cannot add text content handler and particle handler");
        }
        if (this.textContent != null)
        {
            throw new IllegalStateException("Can only add one text content handler");
        }
        this.textContent = textContent;
        return this;
    }

    public Element add(Particle particle)
    {
        if (this.textContent != null)
        {
            throw new IllegalStateException("Cannot add text content handler and particle handler");
        }
        if (this.particle != null)
        {
            throw new IllegalStateException("Can only add one particle handler");
        }
        this.particle = particle;
        return this;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        if (matcher.test(context.reader))
        {
            processForName(context, context.reader.getName());
        }
        else if (minOccurs > 0)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
        }
    }

    void processForName(DeserializeContext<?> context, QName name)
    {
        long occurs = 0;
        while (occurs < maxOccurs && context.reader.isStartElement(name))
        {
            processOne(context);
            occurs++;
        }
        if (occurs < minOccurs)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
        }
    }

    private void processOne(DeserializeContext<?> context)
    {
        if (context.reader.isXsiNil())
        {
            context.reader.skipElement();
        }
        else
        {
            context.pushPathElement(context.reader.getName());
            if (dataFactory != null)
            {
                context.startDataObject(dataFactory, addToParent);
            }
            attributes.forEach(a -> a.process(context));

            if (textContent != null)
            {
                textContent.process(context);  // consumes start tag
            }
            else
            {
                context.reader.nextTag(); // start tag
                if (particle != null)
                {
                    particle.process(context);
                }
                while (context.reader.isStartElement())
                {
                    QName name = context.reader.getName();
                    String msg = "Unexpected element '" + XmlUtils.toShortString(name) + "'" + context.getPath();
                    context.getUnexpectedElementHandling().handle(context, msg);
                    context.reader.skipElement();
                }
            }

            context.reader.nextTag(); // end tag
            if (dataFactory != null)
            {
                context.finishDataObject();
            }
            context.popPathElement();
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return maxOccurs > 0 && matcher.test(context.reader);
    }

    @Override
    public boolean mustConsume()
    {
        return minOccurs > 0;
    }

    @Override
    public String toString()
    {
        return "element{" +
                "minOccurs=" + minOccurs +
                ", maxOccurs=" + maxOccurs +
                ", description='" + description + '\'' +
                '}';
    }

    public static Element ofStrict(long minOccurs, long maxOccurs, QName name)
    {
        return ofStrict(minOccurs, maxOccurs, name, null, null);
    }

    public static Element ofStrict(long minOccurs, long maxOccurs, QName name, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        return new Element(minOccurs, maxOccurs, r -> r.isStartElement(name), dataFactory, addToParent, "Strict: " + name);
    }

    public static Element ofLenient(long minOccurs, long maxOccurs, String name)
    {
        return ofLenient(minOccurs, maxOccurs, name, null, null);
    }

    public static Element ofLenient(long minOccurs, long maxOccurs, String name, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        return new Element(minOccurs, maxOccurs, r -> r.isStartElementLenient(name), dataFactory, addToParent, "Lenient: " + name);
    }

    public static Element ofWildcard(long minOccurs, long maxOccurs, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        return new Element(minOccurs, maxOccurs, r -> r.isStartElement(), dataFactory, addToParent, "Wildcard");
    }
}
