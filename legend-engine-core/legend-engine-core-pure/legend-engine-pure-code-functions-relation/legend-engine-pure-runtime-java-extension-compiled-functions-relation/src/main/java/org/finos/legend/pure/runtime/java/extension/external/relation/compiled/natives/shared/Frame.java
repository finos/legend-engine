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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;

public class Frame
{
    FrameType type;
    boolean fromUnbounded;
    int offsetFrom;
    boolean toUnbounded;
    int offsetTo;

    public Object convert(ExecutionSupport es)
    {
        ProcessorSupport ps = ((CompiledExecutionSupport) es).getProcessorSupport();
        CoreInstance result = ps.newCoreInstance("", this.type == FrameType.rows ? "meta::pure::functions::relation::Rows" : "meta::pure::functions::relation::Range", null);
        CoreInstance from = ps.newCoreInstance("", this.fromUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.fromUnbounded)
        {
            from.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(new ValCoreInstance(String.valueOf(this.offsetFrom), "Integer")));
        }
        CoreInstance to = ps.newCoreInstance("", this.toUnbounded ? "meta::pure::functions::relation::UnboundedFrameValue" : "meta::pure::functions::relation::FrameIntValue", null);
        if (!this.toUnbounded)
        {
            to.setKeyValues(Lists.mutable.with("value"), Lists.mutable.with(new ValCoreInstance(String.valueOf(this.offsetTo), "Integer")));
        }
        result.setKeyValues(Lists.mutable.with("offsetFrom"), Lists.mutable.with(from));
        result.setKeyValues(Lists.mutable.with("offsetTo"), Lists.mutable.with(to));
        return result;
    }

    public static enum FrameType
    {
        rows, range
    }

    public Frame(FrameType type, boolean fromUnbounded, int offsetFrom, boolean toUnbounded, int offsetTo)
    {
        this.type = type;
        this.fromUnbounded = fromUnbounded;
        this.offsetFrom = offsetFrom;
        this.toUnbounded = toUnbounded;
        this.offsetTo = offsetTo;
    }

    public int getLow(int currentRow)
    {
        return fromUnbounded ? 0 : Math.max(0, currentRow - offsetFrom);
    }

    public int getHigh(int currentRow, int maxSize)
    {
        return toUnbounded ? maxSize - 1 : Math.min(maxSize - 1, currentRow + offsetTo);
    }
}
