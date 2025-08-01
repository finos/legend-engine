// Copyright 2025 Goldman Sachs
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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class Rows extends Frame
{
    public Rows(boolean fromUnbounded, Number offsetTo)
    {
        super(fromUnbounded, offsetTo);
    }

    public Rows(boolean fromUnbounded, boolean toUnbounded)
    {
        super(fromUnbounded, toUnbounded);
    }

    public Rows(Number offsetFrom, boolean toUnbounded)
    {
        super(offsetFrom, toUnbounded);
    }

    public Rows(Number offsetFrom, Number offsetTo)
    {
        super(offsetFrom, offsetTo);
    }

    public Rows(boolean b, Number i, boolean b1, Number i1)
    {
        super(b, i, b1, i1);
    }

    protected static Rows buildRows(CoreInstance frameCore, ProcessorSupport processorSupport, PrimitiveHandler primitiveHandler)
    {
        CoreInstance from = frameCore.getValueForMetaPropertyToOne("offsetFrom");
        CoreInstance to = frameCore.getValueForMetaPropertyToOne("offsetTo");
        boolean fromUn = processorSupport.instance_instanceOf(from, "meta::pure::functions::relation::UnboundedFrameValue");
        boolean toUn = processorSupport.instance_instanceOf(to, "meta::pure::functions::relation::UnboundedFrameValue");
        Rows result;
        if (fromUn)
        {
            result = toUn ? new Rows(fromUn, toUn) : new Rows(fromUn, primitiveHandler.toJavaNumber(to.getValueForMetaPropertyToOne("value"), processorSupport));
        }
        else
        {
            result = toUn ? new Rows(primitiveHandler.toJavaNumber(from.getValueForMetaPropertyToOne("value"), processorSupport), toUn)
                    : new Rows(primitiveHandler.toJavaNumber(from.getValueForMetaPropertyToOne("value"), processorSupport), primitiveHandler.toJavaNumber(to.getValueForMetaPropertyToOne("value"), processorSupport));
        }
        return result;
    }

    @Override
    public Number getOffsetFrom()
    {
        return fromUnbounded ? 0 : this.offsetFrom;
    }

    @Override
    public Number getOffsetTo(int maxSize)
    {
        return toUnbounded ? maxSize - 1 : this.offsetTo;
    }

    @Override
    public int getLow(int currentRow)
    {
        return fromUnbounded ? 0 : Math.max(0, currentRow + (Integer) offsetFrom);
    }

    @Override
    public int getHigh(int currentRow, int maxSize)
    {
        return toUnbounded ? maxSize - 1 : Math.min(maxSize - 1, currentRow + (Integer) offsetTo);
    }

    @Override
    public CoreInstance convert(ProcessorSupport ps, PrimitiveHandler primitiveHandler)
    {
        CoreInstance result = ps.newCoreInstance("", "meta::pure::functions::relation::Rows", null);
        CoreInstance from = ps.newCoreInstance("", this.fromUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.fromUnbounded)
        {
            from.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveHandler.build(this.offsetFrom)));
        }
        CoreInstance to = ps.newCoreInstance("", this.toUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.toUnbounded)
        {
            to.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveHandler.build(this.offsetTo)));
        }
        result.setKeyValues(Lists.mutable.with("offsetFrom"), Lists.mutable.with(from));
        result.setKeyValues(Lists.mutable.with("offsetTo"), Lists.mutable.with(to));
        return result;
    }
}
