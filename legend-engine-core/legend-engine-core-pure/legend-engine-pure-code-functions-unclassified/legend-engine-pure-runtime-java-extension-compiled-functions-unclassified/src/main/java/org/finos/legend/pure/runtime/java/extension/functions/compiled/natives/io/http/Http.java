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

package org.finos.legend.pure.runtime.java.extension.functions.compiled.natives.io.http;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class Http extends AbstractNative
{
    public Http()
    {
        super("executeHTTPRaw_URL_1__HTTPMethod_1__String_$0_1$__String_$0_1$__HTTPResponse_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "FunctionsGen.executeHttpRaw(" + transformedParams.get(0) + "," + transformedParams.get(1) + "," + transformedParams.get(2) + "," + transformedParams.get(3) + ",es)";
    }

    @Override
    public String buildBody()
    {

        return "new SharedPureFunction<Object>()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public Object execute(ListIterable<?> vars, final ExecutionSupport es)\n" +
                "            {\n" +
                "                return FunctionsGen.executeHttpRaw((Root_meta_pure_functions_io_http_URL) vars.get(0), vars.get(1), (String) vars.get(2), (String) vars.get(3), es);" +
                "            }\n" +
                "        }";
    }
}
