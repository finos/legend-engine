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

package org.finos.legend.pure.runtime.java.extension.functions.interpreted.natives.io.http;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.stack.MutableStack;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.interpreted.ExecutionSupport;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;
import org.finos.legend.pure.runtime.java.interpreted.VariableContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.InstantiationContext;
import org.finos.legend.pure.runtime.java.interpreted.natives.NativeFunction;
import org.finos.legend.pure.runtime.java.interpreted.profiler.Profiler;
import org.finos.legend.pure.runtime.java.shared.http.HttpMethod;
import org.finos.legend.pure.runtime.java.shared.http.URLScheme;
import org.finos.legend.pure.runtime.java.shared.http.HttpRawHelper;
import org.finos.legend.pure.runtime.java.shared.http.SimpleHttpResponse;

import java.util.Stack;

public class Http extends NativeFunction
{
    private ModelRepository repository;

    public Http(FunctionExecutionInterpreted functionExecution, ModelRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport)
    {
        // URL: param 0
        CoreInstance urlInstance = Instance.getValueForMetaPropertyToOneResolved(params.get(0), M3Properties.values, processorSupport);

        URLScheme urlScheme = URLScheme.http;
        CoreInstance scheme = Instance.getValueForMetaPropertyToOneResolved(urlInstance, "scheme", processorSupport);
        if (scheme != null)
        {
            urlScheme = URLScheme.valueOf(scheme.getName());
        }
        String host = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(urlInstance, M3Properties.host, processorSupport));
        Integer port = (Integer)PrimitiveUtilities.getIntegerValue(Instance.getValueForMetaPropertyToOneResolved(urlInstance, M3Properties.port, processorSupport));
        String path = PrimitiveUtilities.getStringValue(Instance.getValueForMetaPropertyToOneResolved(urlInstance, M3Properties.path, processorSupport));

        // HTTPMethod: param 1
        CoreInstance enumeration = Instance.getValueForMetaPropertyToOneResolved(params.get(1), M3Properties.values, processorSupport);
        String enumName = enumeration.getName();
        HttpMethod httpMethod = HttpMethod.valueOf(enumName);

        // String (mimeType): param 2
        CoreInstance mimeTypeInstance = Instance.getValueForMetaPropertyToOneResolved(params.get(2), M3Properties.values, processorSupport);
        String mimeType = mimeTypeInstance == null ? null : PrimitiveUtilities.getStringValue(mimeTypeInstance);

        // String (body): param 3
        CoreInstance bodyInstance = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        String body = bodyInstance == null ? null : PrimitiveUtilities.getStringValue(bodyInstance);

        SimpleHttpResponse response = HttpRawHelper.executeHttpService(urlScheme, host, port, path, httpMethod, mimeType, body);

        return ValueSpecificationBootstrap.wrapValueSpecification(HttpRawHelper.toHttpResponseInstance(response, processorSupport), true, processorSupport);
    }

}

