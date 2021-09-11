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

public class Document extends ReadHandler
{
    private final ReadHandler content;

    public Document(ReadHandler content)
    {
        this.content = content;
    }

    public void process(DeserializeContext<?> context)
    {
        if (!context.reader.isStartDocument())
        {
            throw new IllegalStateException("Expected start of document");
        }
        context.reader.nextTag();
        content.process(context);
        if (!context.reader.isEndDocument())
        {
            throw new IllegalStateException("Expected end of document");
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return context.reader.isStartDocument();
    }

    @Override
    public boolean mustConsume()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return "document";
    }
}
