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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Frame
{
    FrameType type;
    boolean fromUnbounded;
    int offsetFrom;
    boolean toUnbounded;
    int offsetTo;

    public Frame(FrameType type, boolean fromUnbounded, int offsetTo)
    {
        this.type = type;
        this.fromUnbounded = fromUnbounded;
        this.offsetTo = offsetTo;
    }

    public Frame(FrameType type, boolean fromUnbounded, boolean toUnbounded)
    {
        this.type = type;
        this.fromUnbounded = fromUnbounded;
        this.toUnbounded = toUnbounded;
    }

    public Frame(FrameType type, int offsetFrom, boolean toUnbounded)
    {
        this.type = type;
        this.offsetFrom = offsetFrom;
        this.toUnbounded = toUnbounded;

    }

    public Frame(FrameType type, int offsetFrom, int offsetTo)
    {
        this.type = type;
        this.offsetFrom = offsetFrom;
        this.offsetTo = offsetTo;
    }

    public Frame(FrameType frameType, boolean b, int i, boolean b1, int i1)
    {
        this.type = frameType;
        this.fromUnbounded = b;
        this.offsetFrom = i;
        this.toUnbounded = b1;
        this.offsetTo = i1;
    }

    public static Frame build(CoreInstance frameCore, ProcessorSupport processorSupport)
    {
        if (frameCore == null)
        {
            return null;
        }
        FrameType type = processorSupport.instance_instanceOf(frameCore, "meta::pure::functions::relation::Rows") ? FrameType.rows : FrameType.range;
        CoreInstance from = frameCore.getValueForMetaPropertyToOne("offsetFrom");
        CoreInstance to = frameCore.getValueForMetaPropertyToOne("offsetTo");
        boolean fromUn = processorSupport.instance_instanceOf(from, "meta::pure::functions::relation::UnboundedFrameValue");
        boolean toUn = processorSupport.instance_instanceOf(to, "meta::pure::functions::relation::UnboundedFrameValue");
        Frame result;
        if (fromUn)
        {
            result = toUn ? new Frame(type, fromUn, toUn) : new Frame(type, fromUn, (int) PrimitiveUtilities.getIntegerValue(to.getValueForMetaPropertyToOne("value")));
        }
        else
        {
            result = toUn ? new Frame(type, (int) PrimitiveUtilities.getIntegerValue(from.getValueForMetaPropertyToOne("value")), toUn) : new Frame(type, (int) PrimitiveUtilities.getIntegerValue(from.getValueForMetaPropertyToOne("value")), (int) PrimitiveUtilities.getIntegerValue(to.getValueForMetaPropertyToOne("value")));
        }
        return result;
    }

    public int getOffsetFrom()
    {
        return fromUnbounded ? 0 : this.offsetFrom;
    }

    public int getOffsetTo(int maxSize)
    {
        return toUnbounded ? maxSize - 1 : this.offsetTo;
    }

    public int getLow(int currentRow)
    {
        return fromUnbounded ? 0 : Math.max(0, currentRow + offsetFrom);
    }

    public int getHigh(int currentRow, int maxSize)
    {
        return toUnbounded ? maxSize - 1 : Math.min(maxSize - 1, currentRow + offsetTo);
    }

    public CoreInstance convert(ProcessorSupport ps, PrimitiveBuilder primitiveBuilder)
    {
        CoreInstance result = ps.newCoreInstance("", this.type == FrameType.rows ? "meta::pure::functions::relation::Rows" : "meta::pure::functions::relation::Range", null);
        CoreInstance from = ps.newCoreInstance("", this.fromUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.fromUnbounded)
        {
            from.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveBuilder.build(this.offsetFrom)));
        }
        CoreInstance to = ps.newCoreInstance("", this.toUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.toUnbounded)
        {
            to.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveBuilder.build(this.offsetTo)));
        }
        result.setKeyValues(Lists.mutable.with("offsetFrom"), Lists.mutable.with(from));
        result.setKeyValues(Lists.mutable.with("offsetTo"), Lists.mutable.with(to));
        return result;
    }

    public static interface PrimitiveBuilder
    {
        CoreInstance build(String val);

        CoreInstance build(int val);
    }
}

