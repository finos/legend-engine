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

package org.finos.legend.engine.server.core.pct;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.functions.model.Signature;
import org.finos.legend.pure.m3.pct.reports.model.AdapterKey;
import org.finos.legend.pure.m3.pct.reports.model.FunctionTestResults;
import org.finos.legend.pure.m3.pct.reports.model.TestInfo;
import org.finos.legend.pure.m3.pct.shared.generation.Shared;
import org.finos.legend.shared.stuctures.TreeNode;

public class PCT_to_SimpleHTML
{
    static MutableMap<String, String> moduleURLs = Maps.mutable.empty();

    public static void main(String[] args) throws Exception
    {
        String html = buildHTML();
        Shared.writeStringToTarget("./target", "ok.html", html);
    }

    public static String buildHTML()
    {
        moduleURLs.put("grammar", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("essential", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("standard", "https://github.com/finos/legend-engine/tree/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-base/legend-engine-pure-functions-standard/src/main/resources/");
        moduleURLs.put("relation", "https://github.com/finos/legend-engine/tree/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources");

        Documentation doc = DocumentationGeneration.buildDocumentation();

        ListMultimap<String, AdapterKey> grouped = Lists.mutable.withAll(doc.adapters).groupBy(x -> x.adapter.group);
        MutableList<AdapterKey> orderedAdapters = grouped.keysView().toSortedList().flatCollect(x -> grouped.get(x).toSortedListBy(z -> z.adapter.name));

        // Organize by source
        MutableListMultimap<String, FunctionDocumentation> ordered = Lists.mutable.withAll(doc.functionsDocumentation)
                .groupBy(x ->
                {
                    System.out.println(x.functionDefinition.name);
                    String id = x.functionDefinition.sourceId;
                    return id.substring(x.reportScope.filePath.length(), id.lastIndexOf("/"));
                });


        // Build Tree
        TreeNode root = new TreeNode("root");
        ordered.keyMultiValuePairsView()
                .toSortedListBy(Pair::getOne)
                .forEach(x ->
                        {
                            TreeNode node = root;
                            for (String z : x.getOne().split("/"))
                            {
                                node = node.createOrReturnChild(z);
                            }

                            for (FunctionDocumentation d : x.getTwo().toSortedListBy(v -> v.functionDefinition.name))
                            {
                                MutableList<String> row = Lists.mutable.empty();
                                row.add("<div style='color:#AAAAAA'>" + d.reportScope.module + "</div>");
                                row.add(printFuncName(d));
                                if (!d.functionDefinition.signatures.isEmpty() && d.functionDefinition.signatures.get(0).platformOnly)
                                {
                                    row.add("          <div style='color:#00C72B' class='hover-text'>" + d.functionDefinition.testCount + "<div class='tooltip-text' id='top'>Executed outside of PCT</div></div>");
                                    for (int i = 0; i < orderedAdapters.size() - 1; i++)
                                    {
                                        // Should not have any tests...
                                        row.add("<div style='color:#AAAAAA'>-</div>");
                                    }
                                }
                                else
                                {
                                    row.addAll(orderedAdapters.collect(a -> writeTest(d, a)));
                                }
                                node.addChild(new TreeNode(row));
                            }
                        }
                );

        return top +
                "<BR/><BR/>\n" +
                "    <table style=\"border-spacing:0px;width:900;text-align: center\">\n" +
                "        <tr>\n" +
                "           <th colspan='3' style='width:100'></th>\n" +
                addGroups(orderedAdapters) +
                "        </tr>\n" +
                "        <tr>\n" +
                "           <th style='width:100'></th>\n" +
                "           <th style='width:10'>Group</th>\n" +
                "           <th style='width:200'>Function</th>\n" +
                addHeaders(orderedAdapters) + "\n" +
                "        </tr>\n" +
                root.getChildren().collectWithIndex((n, i) -> addTableRow(n, "", String.valueOf(i), orderedAdapters)).makeString("\n") +
                "\n    </table>\n<BR/><BR/><BR/>\n"
                + bottom;
    }

    private static String addTableRow(TreeNode node, String tab, String id, MutableList<AdapterKey> adapters)
    {
        if (node.getValue() instanceof String)
        {
            String pos = tab.isEmpty() ? "top" : "bottom";
            String borderLine = "border-" + pos + "-style:solid!important;border-" + pos + "-width:1px!important;" + (tab.equals("") ? "border-" + pos + "-color:#000000;" : "border-" + pos + "-color:#EEEEEE;");
            String emptyCell = "<td style='" + borderLine + "'>&nbsp;</td>";
            return "<tr id ='" + id + "'>" +
                    "<td style='" + (tab.isEmpty() ? borderLine : "") + "text-align:left!important'>" + tab + "<a onclick=\"flip('" + id + "', [" + node.getChildren().collectWithIndex((n, i) -> printAllChildrenIds(n, id + "_" + node.getValue(), i)).makeString(", ") + "])\">" + node.getValue() + "</a></td>" +
                    emptyCell +
                    emptyCell +
                    adapters.collect(c -> emptyCell).makeString("") +
                    "</tr>" +
                    node.getChildren().collectWithIndex((n, i) -> addTableRow(n, tab + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;", id + "_" + node.getValue() + "_" + i, adapters)).makeString("\n");
        }
        else if (node.getValue() instanceof MutableList)
        {
            MutableList<String> vals = (MutableList<String>) node.getValue();
            return "<tr id ='" + id + "'><td>&nbsp;</td>" + vals.collect(v -> "<td>" + v + "</td>").makeString("") + "</tr>";
        }
        else
        {
            throw new RuntimeException(node.getValue().getClass() + " is not supported!");
        }
    }

    private static String printAllChildrenIds(TreeNode node, String parentId, int index)
    {
        String id = parentId + "_" + index;
        return "'" + id + "', " + node.getChildren().collectWithIndex((c, i) -> printAllChildrenIds(c, id + "_" + node.getValue(), i)).makeString(", ");
    }


    private static String writeTest(FunctionDocumentation z, AdapterKey a)
    {
        FunctionTestResults results = z.functionTestResults.get(a);
        if (results != null)
        {
            MutableList<TestInfo> tests = Lists.mutable.withAll(results.tests);
            int success = tests.select(t -> t.success).size();
            String color = success == 0 ? "#C70039" : success != tests.size() ? "#FFA500" : "#00C72B";
            return "          <div style='color:" + color + "' class='hover-text'>" + success + "/" + tests.size() + "<div class='tooltip-text' id='top'>" + testDetail(tests) + "</div></div>";
        }
        else
        {
            return "          <div style='color:#AAAAAA'>&empty;</div>";
        }
    }

    private static String testDetail(MutableList<TestInfo> tests)
    {
        return tests.sortThisBy(x -> x.testName).collect(x -> "<span style='color:" + (x.success ? "00FF00" : "#FF0000") + "'>" + x.testName + "</span><span>" + (x.errorMessage == null ? "" : x.errorMessage) + "</span>").makeString("<BR>");

    }

    private static String printFuncName(FunctionDocumentation functionDocumentation)
    {
        String module = functionDocumentation.reportScope.module;
        FunctionDefinition f = functionDocumentation.functionDefinition;
        String color = isPlatformOnly(f) ? "color:#DDDDDD;" : f.name == null ? "color:#79d6db" : "color:#000000;";
        String character = getGrammarCharacter(f);
        return (character != null ? "<span style='color:#34eb92'>" + character + "&nbsp;&nbsp;</span>" : "") + "<a href='" + moduleURLs.get(module) + f.sourceId + "' style='" + color + "'>" + (f.name == null ? "composition-tests" : f.name) + "</a>";
    }

    private static String addGroups(MutableList<AdapterKey> adapterKeys)
    {
        ListMultimap<String, AdapterKey> grouped = adapterKeys.groupBy(x -> x.adapter.group);
        return grouped.keysView().toSortedList().collect(x -> "<th colspan='" + grouped.get(x).size() + "'>" + x + "</th>").makeString("");
    }

    private static String addHeaders(MutableList<AdapterKey> adapterKeys)
    {
        return adapterKeys.collect(c -> c.adapter.name).collect(a -> "          <TH style='width:10'>" + a + "</TH>").makeString("\n");
    }

    public static boolean isPlatformOnly(FunctionDefinition def)
    {
        return ListIterate.detect(def.signatures, x -> x.platformOnly) != null;
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
            "        </style>" +
            "        <script>" +
            "           function flip(stateId, ids)" +
            "           {" +
            "               var state = document.getElementById(stateId).data_openState;" +
            "               state = state === undefined ? true : state;" +
            "               ids.forEach((ele)=> flipOne(!state, ele));" +
            "               document.getElementById(stateId).data_openState = !state;" +
            "           }" +
            "           function flipOne(open, id)" +
            "           {" +
            "               document.getElementById(id).style = open ? 'visibility:true' : 'visibility:collapse';" +
            "           }" +
            "        </script>\n" +
            "    </head>\n" +
            "<body>\n";
    private static String bottom =
            "</body>\n" +
                    "</html>";
}
