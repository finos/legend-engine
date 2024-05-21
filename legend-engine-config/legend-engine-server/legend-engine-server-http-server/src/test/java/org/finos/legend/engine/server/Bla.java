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

package org.finos.legend.engine.server;

import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;

import java.util.Map;

public class Bla
{
    public static void main(String[] args)
    {
        Map<String, FunctionDocumentation> f = DocumentationGeneration.buildDocumentation().documentationByName;

        System.out.println(
                PCTReportProviderLoader.gatherReports().collect(c -> c.adapterKey.adapter).makeString("\n") + "\n" +
                PCTReportProviderLoader.gatherFunctions().collect(c -> c.reportScope.module));

        System.out.println(f.keySet());

        print(DocumentationGeneration.buildDocumentation().documentationByName.get("filter"));
    }

    public static void print(FunctionDocumentation x)
    {
        String signature = x.functionDefinition._package + "::" + x.functionDefinition.name;
        String url = x.module + " " + x.functionDefinition.sourceId;
        String tests = Maps.mutable.withMap(x.functionTestResults).keyValuesView().collect(z ->
        {
            int success = ListIterate.select(z.getTwo().tests, t -> t.success).size();
            return z.getOne().adapter.name + success + " / " + z.getTwo().tests.size();
        }).makeString("\n  ");


        String result = signature + "\n" +
                " url: " + url + "\n" +
                " tests: " + tests;


        System.out.println(result);
    }
}
