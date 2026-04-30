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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

import java.util.ListIterator;
import java.util.Stack;

public class ProcessingContext
{
    public MutableList<MutableMap<String, ValueSpecification>> inferredVariableList = FastList.newList();
    public Stack<MilestoningDatePropagationContext> milestoningDatePropagationContext = new Stack<>();
    private final Stack<String> tags = new Stack<>();
    public boolean isDatePropagationSupported = true;

    // Pre-compiled lambda parameters for batch processing (e.g., ColSpecArray).
    // Kept separate from inferredVariableList so that same-named variables from
    // outer scopes are never incorrectly reused.
    // The targetVariableLevel tracks the exact variable level depth at which these
    // parameters should be consumed (one level deeper than where they were set),
    // preventing nested lambdas (e.g., c | $c.values->exists(c | true)) from
    // incorrectly matching.
    private MutableMap<String, VariableExpression> preCompiledLambdaParameters = null;
    private int preCompiledLambdaParametersTargetLevel = -1;

    public void setPreCompiledLambdaParameters(MutableMap<String, VariableExpression> params)
    {
        this.preCompiledLambdaParameters = params;
        this.preCompiledLambdaParametersTargetLevel = this.inferredVariableList.size() + 1;
    }

    public void clearPreCompiledLambdaParameters()
    {
        this.preCompiledLambdaParameters = null;
        this.preCompiledLambdaParametersTargetLevel = -1;
    }

    public VariableExpression getPreCompiledLambdaParameter(String name)
    {
        if (this.preCompiledLambdaParameters == null)
        {
            return null;
        }
        // Only return pre-compiled parameters at the target depth.
        // This ensures nested lambdas which add additional variable levels) don't incorrectly reuse them.
        if (this.inferredVariableList.size() != this.preCompiledLambdaParametersTargetLevel)
        {
            return null;
        }
        return this.preCompiledLambdaParameters.get(name);
    }

    public void pushMilestoningDatePropagationContext(MilestoningDatePropagationContext milestoningContext)
    {
        this.milestoningDatePropagationContext.push(milestoningContext);
    }

    public MilestoningDatePropagationContext peekMilestoningDatePropagationContext()
    {
        return this.milestoningDatePropagationContext.peek();
    }

    public void popMilestoningDatePropagationContext()
    {
        this.milestoningDatePropagationContext.pop();
    }

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