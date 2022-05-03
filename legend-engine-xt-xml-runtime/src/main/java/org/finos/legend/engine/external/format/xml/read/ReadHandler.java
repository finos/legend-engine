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

package org.finos.legend.engine.external.format.xml.read;

public abstract class ReadHandler
{
    public abstract void process(DeserializeContext<?> context);

    public abstract boolean canConsume(DeserializeContext<?> context);
    public abstract boolean mustConsume();

    protected long requireValidAttributeOccurs(long occurs)
    {
        if (occurs != 0 && occurs != 1)
        {
            throw new IllegalArgumentException("Invalid occurs value for attribute: " + occurs);
        }
        return occurs;
    }

    protected long requireValidOccurs(long occurs)
    {
        if (occurs < 0)
        {
            throw new IllegalArgumentException("Invalid occurs value: " + occurs);
        }
        return occurs;
    }

    protected void checkOccursRange(long minOccurs, long maxOccurs)
    {
        if (minOccurs > maxOccurs)
        {
            throw new IllegalArgumentException("Invalid occurs range: " + minOccurs + " to " + maxOccurs);
        }
    }
}
