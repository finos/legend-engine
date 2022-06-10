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

public class Sequence extends Particle
{
    private final long minOccurs;
    private final long maxOccurs;
    private final MutableList<ReadHandler> sequence = Lists.mutable.empty();

    public Sequence(long minOccurs, long maxOccurs)
    {
        this.minOccurs = requireValidOccurs(minOccurs);
        this.maxOccurs = requireValidOccurs(maxOccurs);
        checkOccursRange(minOccurs, maxOccurs);
    }

    public Sequence add(ReadHandler option)
    {
        sequence.add(option);
        return this;
    }

    @Override
    public void process(DeserializeContext<?> context)
    {
        long occurs = 0;
        while (occurs < maxOccurs && !sequence.isEmpty() && sequence.get(0).canConsume(context))
        {
            sequence.forEach(h -> h.process(context));
            occurs++;
        }
        if (occurs < minOccurs)
        {
            context.getInsufficientOccurrencesHandling().handle(context, "Insufficient occurrences of choice at " + context.getPath());
        }
    }

    @Override
    public boolean canConsume(DeserializeContext<?> context)
    {
        return maxOccurs > 0 && !sequence.isEmpty() && sequence.get(0).canConsume(context);
    }

    @Override
    public boolean mustConsume()
    {
        return minOccurs > 0;
    }

    @Override
    public String toString()
    {
        return "sequence{" +
                "minOccurs=" + minOccurs +
                ", maxOccurs=" + maxOccurs +
                '}';
    }
}
