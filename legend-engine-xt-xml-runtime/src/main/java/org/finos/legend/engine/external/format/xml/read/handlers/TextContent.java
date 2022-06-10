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
import org.finos.legend.engine.external.format.xml.read.ValueProcessor;
import org.finos.legend.engine.external.format.xml.read.DeserializeContext;

public class TextContent<C> extends ReadHandler
{
    private final ValueProcessor<C> valueProcessor;

    public TextContent(ValueProcessor<C> valueProcessor)
    {
        this.valueProcessor = valueProcessor;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        try
        {
            valueProcessor.process(context, context.reader.getElementText());
        }
        catch (Exception e)
        {
            context.addErrorDefect(e.getMessage() + " at " + context.getPath());
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return context.reader.isStartElement() ;
    }

    @Override
    public boolean mustConsume()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return "textContent";
    }
}
