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

import org.finos.legend.engine.external.format.xml.read.ReadHandler;
import org.finos.legend.engine.external.format.xml.read.DeserializeContext;

public class AnySurroundingElements extends ReadHandler
{
    private final Element element;

    public AnySurroundingElements(Element element)
    {
        this.element = element;
    }

    public void process(DeserializeContext<?> context)
    {
        int depth = skip(context);
        while (depth > 0 || element.canConsume(context))
        {
            depth += skip(context);
            if (element.canConsume(context))
            {
                element.process(context);
            }
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return context.reader.hasNext();
    }

    @Override
    public boolean mustConsume()
    {
        return false;
    }

    private int skip(DeserializeContext<?> context)
    {
        int depth = 0;
        while (canConsume(context) && !element.canConsume(context))
        {
            if (context.reader.isStartElement())
            {
                depth++;
                context.pushPathElement(context.reader.getName());
            }
            else if (context.reader.isEndElement())
            {
                context.popPathElement();
                depth--;
            }
            context.reader.nextTag();
        }
        return depth;
    }
}
