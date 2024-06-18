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

package org.finos.legend.pure.runtime.java.extension.external.json.compiled.natives;

import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.natives.AbstractNative;

public class ParseJSON extends AbstractNative
{
    public ParseJSON()
    {
        super("parseJSON_String_1__JSONElement_1_");
    }

    @Override
    public String build(CoreInstance topLevelElement, CoreInstance functionExpression, ListIterable<String> transformedParams, ProcessorContext processorContext)
    {
        return "(("  + JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("meta::json::JSONElement") + ")new org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser(((CompiledExecutionSupport)es).getProcessorSupport()).toPureJson(" + transformedParams.get(0) + "))";
    }

    @Override
    public String buildBody()
    {
        final String jsonElement = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath("meta::json::JSONElement");
        return "new DefendedPureFunction1<String, " + jsonElement + ">()\n" +
                "        {\n" +
                "            @Override\n" +
                "            public " + jsonElement + " value(String url, ExecutionSupport es)\n" +
                "            {\n" +
                "               return (" + jsonElement + ")new org.finos.legend.pure.runtime.java.extension.external.json.shared.JsonParser(((CompiledExecutionSupport)es).getProcessorSupport()).toPureJson(url);" +
                "            }\n" +
                "        }";
    }

}

