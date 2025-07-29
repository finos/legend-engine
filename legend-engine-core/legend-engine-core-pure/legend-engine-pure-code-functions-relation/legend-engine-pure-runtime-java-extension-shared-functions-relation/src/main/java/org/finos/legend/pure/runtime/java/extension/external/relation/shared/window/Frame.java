// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.shared.window;

import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public abstract class Frame
{
    boolean fromUnbounded;
    Number offsetFrom;
    boolean toUnbounded;
    Number offsetTo;

    public Frame(boolean fromUnbounded, Number offsetTo)
    {
        this.fromUnbounded = fromUnbounded;
        this.offsetTo = offsetTo;
    }

    public Frame(boolean fromUnbounded, boolean toUnbounded)
    {
        this.fromUnbounded = fromUnbounded;
        this.toUnbounded = toUnbounded;
    }

    public Frame(Number offsetFrom, boolean toUnbounded)
    {
        this.offsetFrom = offsetFrom;
        this.toUnbounded = toUnbounded;

    }

    public Frame(Number offsetFrom, Number offsetTo)
    {
        this.offsetFrom = offsetFrom;
        this.offsetTo = offsetTo;
    }

    public Frame(boolean b, Number i, boolean b1, Number i1)
    {
        this.fromUnbounded = b;
        this.offsetFrom = i;
        this.toUnbounded = b1;
        this.offsetTo = i1;
    }

    public static Frame build(CoreInstance frameCore, ProcessorSupport processorSupport, PrimitiveHandler primitiveHandler)
    {
        if (frameCore == null)
        {
            return null;
        }
        if (processorSupport.instance_instanceOf(frameCore, "meta::pure::functions::relation::Rows"))
        {
            return Rows.buildRows(frameCore, processorSupport, primitiveHandler);
        }
        else if (processorSupport.instance_instanceOf(frameCore, "meta::pure::functions::relation::_Range"))
        {
            return Range.buildRange(frameCore, processorSupport, primitiveHandler);
        }
        else
        {
            return RangeInterval.buildRangeInterval(frameCore, processorSupport, primitiveHandler);
        }
    }

    public abstract Number getOffsetFrom();

    public abstract Number getOffsetTo(int maxSize);

    public abstract int getLow(int currentRow);

    public abstract int getHigh(int currentRow, int maxSize);

    public abstract CoreInstance convert(ProcessorSupport ps, PrimitiveHandler primitiveHandler);

    public static interface PrimitiveHandler
    {
        CoreInstance build(String val);

        CoreInstance build(Number val);

        Number plus(Number left, Number right);

        Number minus(Number left, Number right);

        boolean lessThanEqual(Number left, Number right);

        default Number toJavaNumber(CoreInstance coreInstance, ProcessorSupport processorSupport)
        {
            return null;
        }
    }
}

