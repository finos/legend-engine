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

package org.finos.legend.pure.runtime.java.extension.external.json.interpreted.natives;

import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativePredicate;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Stack;

public class JsonStringsEqual extends NativePredicate
{
    public JsonStringsEqual(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        super(repository);
    }

    @Override
    protected boolean executeBoolean(Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, ListIterable<? extends CoreInstance> params, VariableContext variableContext, CoreInstance functionExpressionToUseInStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, ProcessorSupport processorSupport) throws PureExecutionException
    {
        CoreInstance left = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);
        CoreInstance right = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        JSONParser jsonParser = new JSONParser();
        try
        {
            Object rightDeserialized = jsonParser.parse(PrimitiveUtilities.getStringValue(right));
            Object leftDeserialized = jsonParser.parse(PrimitiveUtilities.getStringValue(left));
            return leftDeserialized.equals(rightDeserialized);
        }
        catch (ParseException e)
        {
            //ParseException implementation returns human readable error message from toString() method and getMessage() returns null
            throw new PureExecutionException(functionExpressionToUseInStack.getSourceInformation(), "Failed to parse JSON string. Invalid JSON string. " + e.toString(), e);
        }
    }
}
