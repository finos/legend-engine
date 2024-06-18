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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.functions.model.Signature;
import org.finos.legend.pure.m3.pct.reports.model.AdapterKey;
import org.finos.legend.pure.m3.pct.reports.model.FunctionTestResults;
import org.finos.legend.pure.m3.pct.reports.model.TestInfo;
import org.finos.legend.pure.m3.pct.shared.generation.Shared;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderLoader;

import java.util.Map;

public class GeneratePCTReport
{
    static MutableMap<String, String> moduleURLs = Maps.mutable.empty();

    public static void main(String[] args) throws Exception
    {
        moduleURLs.put("basic", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("grammar", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("base", "https://github.com/finos/legend-pure/tree/master/legend-pure-functions/legend-pure-functions-base/legend-engine-pure-functions-unclassified-pure/src/main/resources/");
        moduleURLs.put("relation", "https://github.com/finos/legend-pure/tree/master/legend-pure-functions/legend-engine-pure-runtime-java-extension-compiled-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources");

        Map<String, FunctionDocumentation> f = DocumentationGeneration.buildDocumentation().documentationByName;

        System.out.println("Functions: (" + f.keySet().size() + ") " + f.keySet());

        System.out.println("");

        MutableListMultimap<String, FunctionDocumentation> ordered = Lists.mutable.withAll(DocumentationGeneration.buildDocumentation().documentationByName.values()).groupBy(x -> x.functionDefinition._package);

        MutableList<AdapterKey> adapterKeys = PCTReportProviderLoader.gatherReports().collect(c -> c.adapterKey).distinct();

        String html = top + ordered.keyMultiValuePairsView()
                .collect(packageAndFunctionDocListPair -> "\n" +
                        "<b>" + packageAndFunctionDocListPair.getOne() + "</b>\n" +
                        "<BR/><BR/>\n" +
                        "    <table style=\"width:800;text-align: center\">\n" +
                        "        <tr>\n" +
                        "           <th style='width:300'>Name</th>\n" +
                        addHeaders(adapterKeys) + "\n" +
                        "        </tr>\n" +
                        sortFunctionDef(packageAndFunctionDocListPair.getTwo()).collect(z ->
                                "        <TR>\n" +
                                        printFuncName(z.module, z.functionDefinition) +
                                        adapterKeys.collect(a -> writeTest(z, a)).makeString("\n") + "\n" +
                                        "        </TR>\n"
                        ).makeString("\n") +
                        "\n    </table>\n<BR/><BR/><BR/>\n"
                )
                .makeString("\n")
                + bottom;

        Shared.writeStringToTarget("./target", "ok.html", html);
    }

    private static String writeTest(FunctionDocumentation z, AdapterKey a)
    {
        FunctionTestResults results = z.functionTestResults.get(a);
        if (results != null)
        {
            MutableList<TestInfo> tests = Lists.mutable.withAll(results.tests);
            int success = tests.select(t -> t.success).size();
            String color = success == 0 ? "#C70039 " : success != tests.size() ? "#FFA500" : "#00C72B";
            return "          <TD style='height:0;color:" + color + "'><div class='hover-text'>" + success + "/" + tests.size() + "<div class='tooltip-text' id='top'>" + testDetail(tests) + "</div></div></TD>";
        }
        else
        {
            return "          <TD> - </TD>";
        }
    }

    private static String testDetail(MutableList<TestInfo> tests)
    {
        return tests.sortThisBy(x -> x.testName).collect(x -> "<span style='color:" + (x.success ? "00FF00" : "#FF0000") + "'>" + x.testName + "</span><span>" + (x.errorMessage == null ? "" : x.errorMessage) + "</span>").makeString("<BR>");

    }

    private static MutableList<FunctionDocumentation> sortFunctionDef(RichIterable<FunctionDocumentation> functionDocumentations)
    {
        return functionDocumentations.toSortedListBy(funcDoc ->
        {
            FunctionDefinition funcDef = funcDoc.functionDefinition;
            return isPlatformOnly(funcDef) ? "|" + funcDef.name : getGrammarCharacter(funcDef) != null ? "." + funcDef.name : funcDef.name;
        });
    }

    private static String printFuncName(String module, FunctionDefinition f)
    {
        String color = isPlatformOnly(f) ? " style='color:#DDDDDD;text-decoration: line-through;'" : "";
        String character = getGrammarCharacter(f);
        return "          <TD" + color + ">" + (character != null ? "<span style='color:#34eb92'>" + character + "&nbsp;&nbsp;</span><span style='color:#DDDDDD'><a href='" + moduleURLs.get(module) + f.sourceId + "' style='color:#DDDDDD'>" + f.name + "</a></span>" : "<a href='" + moduleURLs.get(module) + f.sourceId + "' style='color:#000000'>" + f.name + "</a>") + "</TD>\n";
    }

    private static String addHeaders(MutableList<AdapterKey> adapterKeys)
    {
        return adapterKeys.collect(c -> c.adapter.name).collect(a -> "          <TH>" + a + "</TH>").makeString("\n");
    }

    public static boolean isPlatformOnly(FunctionDefinition def)
    {
        System.out.println(def.name + " " + (ListIterate.detect(def.signatures, x -> x.platformOnly) != null));
        return ListIterate.detect(def.signatures, x -> x.platformOnly) != null;
    }

    public static String getDoc(FunctionDefinition def)
    {
        return ListIterate.detect(def.signatures, x -> x.documentation != null).documentation;
    }

    public static String getGrammarCharacter(FunctionDefinition def)
    {
        Signature signature = ListIterate.detect(def.signatures, x -> x.grammarCharacter != null);
        return signature == null ? null : signature.grammarCharacter;
    }


    private static String top = "<html>\n" +
            "    <head>\n" +
            "        <style>\n" +
            "            td {\n" +
            "               text-align: center;\n" +
            "            }\n" +
            "\n" +
            "            .tooltip-text {\n" +
            "               visibility: hidden;\n" +
            "               position: absolute;\n" +
            "               z-index: 1;\n" +
            "               color: white;\n" +
//            "               width:500;" +
            "               font-size: 12px;\n" +
            "               background-color: #192733;\n" +
            "               border-radius: 10px;\n" +
            "               padding: 10px 15px 10px 15px;\n" +
            "            }" +
            "\n" +
            "            .hover-text:hover .tooltip-text {\n" +
            "               visibility: visible;\n" +
            "            }" +
            "\n" +
            "            .hover-text {\n" +
            "               position: relative;\n" +
            "               display: inline-block;\n" +
            "               font-family: Arial;\n" +
            "               text-align: center;\n" +
            "            }" +
            "        </style>\n" +
            "    </head>\n" +
            "<body>\n";
    private static String bottom =
            "</body>\n" +
                    "</html>";
}
