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
import org.finos.legend.engine.external.format.xml.read.ReadHandler;
import org.finos.legend.engine.external.format.xml.read.ValueProcessor;
import org.finos.legend.engine.external.format.xml.shared.XmlReader;

import javax.xml.namespace.QName;
import java.util.function.Function;
import java.util.function.Predicate;

public class Attribute<C> extends ReadHandler
{
    private final long minOccurs;
    private final long maxOccurs;
    private final Predicate<XmlReader> matcher;
    private final ValueProcessor<C> valueProcessor;
    private final Function<XmlReader, String> getValue;
    private final Function<XmlReader, String> resolveName;

    private Attribute(long minOccurs, long maxOccurs, Predicate<XmlReader> matcher, ValueProcessor<C> valueProcessor, Function<XmlReader, String> getValue, Function<XmlReader, String> resolveName)
    {
        this.minOccurs = requireValidAttributeOccurs(minOccurs);
        this.maxOccurs = requireValidAttributeOccurs(maxOccurs);
        checkOccursRange(minOccurs, maxOccurs);

        this.matcher = matcher;
        this.valueProcessor = valueProcessor;
        this.getValue = getValue;
        this.resolveName = resolveName;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        if (matcher.test(context.reader))
        {
            if (maxOccurs == 0)
            {
                context.getUnexpectedElementHandling().handle(context, "Prohibited attribute present '" + resolveName.apply(context.reader) + "' at " + context.getPath());
            }
            else
            {
                try
                {
                    valueProcessor.process(context, getValue.apply(context.reader));
                }
                catch (Exception e)
                {
                    context.addErrorDefect(e.getMessage() + " at " + path(context));
                }
            }
        }
        else if (minOccurs == 1)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Missing mandatory attribute '" + resolveName.apply(context.reader) + "' at " + context.getPath());
        }
    }

    private String path(DeserializeContext<?> context)
    {
        return context.getPath() + "[@" + resolveName.apply(context.reader) + "]";
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return matcher.test(context.reader);
    }

    @Override
    public boolean mustConsume()
    {
        return minOccurs > 0;
    }

    public static <C> Attribute<C> ofStrict(long minOccurs, long maxOccurs, QName name, ValueProcessor<C> valueProcessor)
    {
        return new Attribute<>(minOccurs, maxOccurs, r -> r.hasAttribute(name), valueProcessor, r -> r.getAttributeValue(name), r -> name.getLocalPart());
    }

    public static <C> Attribute<C> ofLenient(long minOccurs, long maxOccurs, String name, ValueProcessor<C> valueProcessor)
    {
        Function<XmlReader, String> resolveName = r ->
        {
            String resolved = r.resolveLenientAttributeName(name);
            return  resolved == null ? name : resolved;
        };
        return new Attribute<>(minOccurs, maxOccurs, r -> r.hasAttributeLenient(name), valueProcessor, r -> r.getAttributeValueLenient(name), resolveName);
    }
}
