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

import org.finos.legend.engine.external.format.xml.read.DeserializeContext;
import org.finos.legend.engine.external.format.xml.shared.XmlReader;
import org.finos.legend.engine.external.shared.runtime.dependencies.ExternalDataObjectAdder;
import org.finos.legend.engine.external.shared.runtime.dependencies.IExternalDataFactory;

import javax.xml.namespace.QName;
import java.util.function.Predicate;

/*
 * Used when no schema exists to allow propoerties of multiplicity [*] to be mapped from either a repeated
 * element or container element with a repeated element.  Assumes the document will be consistent so establishes
 * the approach on the first record.
 */
public class FlexCollectionElement extends Element
{
    private enum Type
    {
        IN_CONTAINER,
        REPEATED_ELEMENT
    }

    private Type type = null;

    private FlexCollectionElement(long minOccurs, long maxOccurs, Predicate<XmlReader> matcher, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent, String description)
    {
        super(minOccurs, maxOccurs, matcher, dataFactory, addToParent, description);
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        if (type == null)
        {
            // Assume container and see if we can consume
            boolean maybeContainer = true;
            int subElementsCount = 0;
            int consumableSubElementsCount = 0;
            // Uncommitted txn to look ahead
            try (XmlReader.Transaction txn = context.reader.newTransaction())
            {
                QName containerName = context.reader.getName();
                QName containedName = null;
                context.reader.nextTag();
                while (!context.reader.isEndElement(containerName))
                {
                    subElementsCount++;
                    if (containedName == null)
                    {
                        containedName = context.reader.getName();
                    }
                    else if (!containedName.equals(context.reader.getName()))
                    {
                        maybeContainer = false;
                    }

                    if (context.reader.isXsiNil())
                    {
                        context.reader.skipElement();
                        consumableSubElementsCount++;
                    }
                    else if (canConsumeElement(context))
                    {
                        consumableSubElementsCount++;
                    }
                }
            }

            if (maybeContainer && subElementsCount > 0 && consumableSubElementsCount == subElementsCount)
            {
                type = Type.IN_CONTAINER;
            }
            else
            {
                // Cannot assume container so see if we can consume as an element
                // Uncommitted txn to look ahead
                try (XmlReader.Transaction txn = context.reader.newTransaction())
                {
                    if (canConsumeElement(context))
                    {
                        type = Type.REPEATED_ELEMENT;
                    }
                }
            }
        }

        if (type == null)
        {
            // Type hasn't been established yet so we skip assuming it's an empty list
            context.reader.skipElement();
            if (minOccurs > 0)
            {
                context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
            }
        }
        else if (type == Type.REPEATED_ELEMENT)
        {
            super.process(context);
        }
        else
        {
            QName containerName = context.reader.getName();
            context.reader.nextTag();
            if (!context.reader.isEndElement(containerName))
            {
                processForName(context, context.reader.getName());
            }
            else if (minOccurs > 0)
            {
                context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
            }
            context.reader.nextTag();
        }
    }

    private boolean canConsumeElement(DeserializeContext<?> context)
    {
        if (attributes.anySatisfy(a -> a.canConsume(context)))
        {
            context.reader.skipElement();
            return true;
        }
        else
        {
            QName currentName = context.reader.getName();
            context.reader.nextTag();
            boolean canConsume = false;
            while (!context.reader.isEndElement(currentName))
            {
                canConsume |= particle.canConsume(context);
                context.reader.skipElement();
            }
            context.reader.nextTag();
            return canConsume;
        }
    }

    @Override
    public String toString()
    {
        return "FlexCollectionElement{" +
                "minOccurs=" + minOccurs +
                ", maxOccurs=" + maxOccurs +
                ", description='" + description + '\'' +
                '}';
    }

    public static FlexCollectionElement ofStrict(long minOccurs, long maxOccurs, QName name)
    {
        return ofStrict(minOccurs, maxOccurs, name, null, null);
    }

    public static FlexCollectionElement ofStrict(long minOccurs, long maxOccurs, QName name, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        return new FlexCollectionElement(minOccurs, maxOccurs, r -> r.isStartElement(name), dataFactory, addToParent, "Strict: " + name);
    }

    public static FlexCollectionElement ofLenient(long minOccurs, long maxOccurs, String name)
    {
        return ofLenient(minOccurs, maxOccurs, name, null, null);
    }

    public static FlexCollectionElement ofLenient(long minOccurs, long maxOccurs, String name, IExternalDataFactory dataFactory, ExternalDataObjectAdder addToParent)
    {
        return new FlexCollectionElement(minOccurs, maxOccurs, r -> r.isStartElementLenient(name), dataFactory, addToParent, "Lenient: " + name);
    }
}
