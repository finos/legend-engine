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

public class All extends Particle
{
    private final long minOccurs;
    private final long maxOccurs;
    private final MutableList<ReadHandler> all = Lists.mutable.empty();

    public All(long minOccurs, long maxOccurs)
    {
        this.minOccurs = requireValidOccurs(minOccurs);
        this.maxOccurs = requireValidOccurs(maxOccurs);
        checkOccursRange(minOccurs, maxOccurs);
    }

    public All add(ReadHandler option)
    {
        all.add(option);
        return this;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        long occurs = 0;
        while (occurs < maxOccurs && !all.isEmpty() && all.get(0).canConsume(context))
        {
            MutableList<ReadHandler> remaining = Lists.mutable.ofAll(all);
            MutableList<ReadHandler> consumers = handlersThatCanConsume(remaining, context);
            while (!consumers.isEmpty())
            {
                if (consumers.size() > 1)
                {
                    throw new IllegalStateException("Ambiguous consumption in all not yet supported");
                }
                consumers.get(0).process(context);
                remaining.remove(consumers.get(0));
                consumers = handlersThatCanConsume(remaining, context);
            }
            MutableList<ReadHandler> remainingMandatories = remaining.select(ReadHandler::mustConsume);
            if (!remainingMandatories.isEmpty())
            {
                context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of " + remainingMandatories.get(0) + " at " + context.getPath());
            }
            occurs++;
        }
        if (occurs < minOccurs)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
        }
    }

    private MutableList<ReadHandler> handlersThatCanConsume(MutableList<ReadHandler> handlers, DeserializeContext<?> context)
    {
        return handlers.select(h -> h.canConsume(context));
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return maxOccurs > 0 && !all.isEmpty() && !handlersThatCanConsume(all, context).isEmpty();
    }

    @Override
    public boolean mustConsume()
    {
        return minOccurs > 0;
    }

    @Override
    public String toString()
    {
        return "all{" +
                "minOccurs=" + minOccurs +
                ", maxOccurs=" + maxOccurs +
                '}';
    }
}
