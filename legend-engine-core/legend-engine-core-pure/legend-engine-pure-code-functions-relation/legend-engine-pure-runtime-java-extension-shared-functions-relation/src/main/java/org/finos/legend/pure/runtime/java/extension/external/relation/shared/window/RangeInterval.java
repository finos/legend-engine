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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class RangeInterval extends Frame
{
    Enum offsetFromDurationUnit;
    Enum offsetToDurationUnit;

    public RangeInterval(boolean fromUnbounded, Number offsetTo, Enum offsetToDurationUnit)
    {
        super(fromUnbounded, offsetTo);
        this.offsetToDurationUnit = offsetToDurationUnit;
    }

    public RangeInterval(boolean fromUnbounded, boolean toUnbounded)
    {
        super(fromUnbounded, toUnbounded);
    }

    public RangeInterval(Number offsetFrom, Enum offsetFromDurationUnit, boolean toUnbounded)
    {
        super(offsetFrom, toUnbounded);
        this.offsetFromDurationUnit = offsetFromDurationUnit;
    }

    public RangeInterval(Number offsetFrom, Enum offsetFromDurationUnit, Number offsetTo, Enum offsetToDurationUnit)
    {
        super(offsetFrom, offsetTo);
        this.offsetFromDurationUnit = offsetFromDurationUnit;
        this.offsetToDurationUnit = offsetToDurationUnit;
    }

    public RangeInterval(boolean b, Number i, Enum offsetFromDurationUnit, boolean b1, Number i1, Enum offsetToDurationUnit)
    {
        super(b, i, b1, i1);
        this.offsetFromDurationUnit = offsetFromDurationUnit;
        this.offsetToDurationUnit = offsetToDurationUnit;
    }

    protected static RangeInterval buildRangeInterval(CoreInstance frameCore, ProcessorSupport processorSupport, PrimitiveHandler primitiveHandler)
    {
        CoreInstance from = frameCore.getValueForMetaPropertyToOne("offsetFrom");
        CoreInstance to = frameCore.getValueForMetaPropertyToOne("offsetTo");
        boolean fromUn = processorSupport.instance_instanceOf(from, "meta::pure::functions::relation::UnboundedFrameValue");
        boolean toUn = processorSupport.instance_instanceOf(to, "meta::pure::functions::relation::UnboundedFrameValue");
        RangeInterval result;
        if (fromUn)
        {
            result = toUn ? new RangeInterval(fromUn, toUn)
                          : new RangeInterval(fromUn, primitiveHandler.toJavaNumber(to.getValueForMetaPropertyToOne("value"), processorSupport),
                                              (Enum) to.getValueForMetaPropertyToOne("durationUnit"));
        }
        else
        {
            result = toUn ? new RangeInterval(primitiveHandler.toJavaNumber(from.getValueForMetaPropertyToOne("value"), processorSupport),
                                              (Enum) from.getValueForMetaPropertyToOne("durationUnit"), toUn)
                          : new RangeInterval(primitiveHandler.toJavaNumber(from.getValueForMetaPropertyToOne("value"), processorSupport),
                                              (Enum) from.getValueForMetaPropertyToOne("durationUnit"),
                                              primitiveHandler.toJavaNumber(to.getValueForMetaPropertyToOne("value"), processorSupport),
                                              (Enum) to.getValueForMetaPropertyToOne("durationUnit"));
        }
        return result;
    }

    @Override
    public Number getOffsetFrom()
    {
        return fromUnbounded ? null : this.offsetFrom;
    }

    @Override
    public Number getOffsetTo(int maxSize)
    {
        return toUnbounded ? null : this.offsetTo;
    }

    public Enum getOffsetFromDurationUnit()
    {
        return this.fromUnbounded ? null : this.offsetFromDurationUnit;
    }

    public Enum getOffsetToDurationUnit()
    {
        return this.toUnbounded ? null : this.offsetToDurationUnit;
    }

    @Override
    public int getLow(int currentRow)
    {
        return 0;
    }

    @Override
    public int getHigh(int currentRow, int maxSize)
    {
        return maxSize - 1;
    }

    @Override
    public CoreInstance convert(ProcessorSupport ps, PrimitiveHandler primitiveHandler)
    {
        CoreInstance result = ps.newCoreInstance("", "meta::pure::functions::relation::_RangeInterval", null);
        CoreInstance from = ps.newCoreInstance("", this.fromUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntervalValue", null);
        if (!this.fromUnbounded)
        {
            from.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveHandler.build(this.offsetFrom)));
            from.setKeyValues(Lists.mutable.with("durationUnit"), Lists.mutable.with(this.offsetFromDurationUnit));
        }
        CoreInstance to = ps.newCoreInstance("", this.toUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntervalValue", null);
        if (!this.toUnbounded)
        {
            to.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(primitiveHandler.build(this.offsetTo)));
            to.setKeyValues(Lists.mutable.with("durationUnit"), Lists.mutable.with(this.offsetToDurationUnit));
        }
        result.setKeyValues(Lists.mutable.with("offsetFrom"), Lists.mutable.with(from));
        result.setKeyValues(Lists.mutable.with("offsetTo"), Lists.mutable.with(to));
        return result;
    }
}
