// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

import java.util.ListIterator;
import java.util.Stack;

public class ProcessingContext
{
    public MutableList<MutableMap<String, ValueSpecification>> inferredVariableList = FastList.newList();
    public MilestoningDatePropagationContext milestoningDatePropagationContext = new MilestoningDatePropagationContext();
    private final Stack<String> tags = new Stack<>();

    public ProcessingContext(String firstTag)
    {
        this.tags.push(firstTag);
    }

    public ProcessingContext push(String s)
    {
        this.tags.push(s);
        return this;
    }

    public ProcessingContext pop()
    {
        this.tags.pop();
        return this;
    }

    public String peek()
    {
        return this.tags.peek();
    }

    public Stack<String> getStack()
    {
        return tags;
    }

    public boolean hasVariableLevel()
    {
        return !this.inferredVariableList.isEmpty();
    }

    public void addInferredVariables(String name, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification variable)
    {
        if (inferredVariableList.isEmpty())
        {
            this.addVariableToNewLevel(name, variable);
        }
        else
        {
            this.inferredVariableList.getLast().put(name, variable);
        }
    }

    public void flushVariable(String name)
    {
        this.inferredVariableList.getLast().remove(name);
    }

    public void removeLastVariableLevel()
    {
        this.inferredVariableList.remove(this.inferredVariableList.size() - 1);
    }

    public void addVariableLevel()
    {
        MutableMap<String, ValueSpecification> map = UnifiedMap.newMap();
        this.inferredVariableList.add(map);
    }

    public org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification getInferredVariable(String name)
    {
        ListIterator<MutableMap<String, ValueSpecification>> reversedIt = this.inferredVariableList.toReversed().listIterator();
        org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification vs = null;
        while (reversedIt.hasNext())
        {
            try
            {
                vs = reversedIt.next().get(name);
            }
            catch (NullPointerException e)
            {
                vs = null;
            }
            if (vs != null)
            {
                break;
            }
        }
        return vs;
    }

    private void addVariableToNewLevel(String name, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification variable)
    {
        MutableMap<String, ValueSpecification> map = UnifiedMap.newMap();
        map.put(name, variable);
        this.inferredVariableList.add(map);
    }
}