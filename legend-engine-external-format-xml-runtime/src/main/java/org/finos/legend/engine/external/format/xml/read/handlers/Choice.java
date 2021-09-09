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

public class Choice extends Particle
{
    private final long minOccurs;
    private final long maxOccurs;
    private final MutableList<ReadHandler> options = Lists.mutable.empty();

    private Choice(long minOccurs, long maxOccurs)
    {
        this.minOccurs = requireValidOccurs(minOccurs);
        this.maxOccurs = requireValidOccurs(maxOccurs);
        checkOccursRange(minOccurs, maxOccurs);
    }

    public Choice add(ReadHandler option)
    {
        options.add(option);
        return this;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        long occurs = 0;
        long lastOccurs = 0;
        do
        {
            lastOccurs = occurs;

            MutableList<ReadHandler> matching = options.select(h -> h.canConsume(context));
            if (matching.size() == 1)
            {
                matching.get(0).process(context);
                occurs++;
            }
            else if (matching.size() > 1)
            {
                while (!matching.isEmpty() && occurs == lastOccurs)
                {
                    if (tryOption(matching.remove(0), context))
                    {
                        occurs++;
                    }
                }
            }
        }
        while (occurs < maxOccurs && occurs != lastOccurs);

        if (occurs < minOccurs)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
        }
    }

    private boolean tryOption(ReadHandler handler, DeserializeContext<?> context)
    {
        try (DeserializeContext.Transaction txn = context.newTransaction())
        {
            handler.process(context);
            txn.commit();
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return maxOccurs > 0 && options.anySatisfy(h -> h.canConsume(context));
    }

    @Override
    public boolean mustConsume()
    {
        return minOccurs > 0;
    }

    public static Choice of(long minOccurs, long maxOccurs)
    {
        return new Choice(minOccurs, maxOccurs);
    }

    @Override
    public String toString()
    {
        return "choice{" +
                "minOccurs=" + minOccurs +
                ", maxOccurs=" + maxOccurs +
                '}';
    }

}
