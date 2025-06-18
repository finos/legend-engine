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

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.m3.pct.aggregate.generation.DocumentationGeneration;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.functions.model.Signature;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
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
        String html = buildHTML(Sets.mutable.of(args), Sets.mutable.of(), false);
        Shared.writeStringToTarget("./target", "ok.html", html);
    }

    public static String buildHTML(Set<String> adapterNames, Set<String> adapterQualifiers, boolean skipFunctionsWithoutTest)
    {
        String commitId = getCommitId();
        moduleURLs.put("grammar", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("essential", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        moduleURLs.put("standard", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-standard/legend-engine-pure-functions-standard-pure/src/main/resources", commitId));
        moduleURLs.put("relation", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources", commitId));
        moduleURLs.put("unclassified", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-unclassified/legend-engine-pure-functions-unclassified-pure/src/main/resources", commitId));

        Documentation doc = DocumentationGeneration.buildDocumentation();

        ListMultimap<String, AdapterKey> grouped = Lists.mutable.withAll(doc.adapters)
                .select(x -> adapterNames.isEmpty() || adapterNames.contains(x.adapter.name))
                .groupBy(x -> x.adapter.group);
        MutableList<AdapterKey> orderedAdapters = grouped.keysView().toSortedList().flatCollect(x -> grouped.get(x).toSortedListBy(z -> z.adapter.name));

        // Organize by source
        MutableListMultimap<String, FunctionDocumentation> ordered = Lists.mutable.withAll(doc.functionsDocumentation)
                .select(x -> !skipFunctionsWithoutTest || orderedAdapters.stream()
                                 .map(x.functionTestResults::get)
                                 .filter(Objects::nonNull)
                                 .flatMap(result -> result.tests.stream())
                                 .anyMatch(t -> hasAdapterQualifier(t, adapterQualifiers))
                )
                .groupBy(x ->
                {
                    String id = x.functionDefinition.sourceId;
                    return id.substring(x.reportScope.filePath.length(), id.lastIndexOf("/"));
                });
        MutableMap<AdapterKey, TestResultCount> testResultCountByAdapter = Maps.mutable.empty();

        // Build Tree
        TreeNode root = new TreeNode("root");
        ordered.keyMultiValuePairsView()
                .toSortedListBy(Pair::getOne)
                .forEach(x ->
                        {
                            TreeNode node = root;
                            //System.out.println(x.getOne());
                           // System.out.println(x.getTwo().collect(e -> e.functionDefinition.name));
                            for (String z : x.getOne().split("/"))
                            {
                                node = node.createOrReturnChild(z);
                            }

                            for (FunctionDocumentation d : x.getTwo().toSortedListBy(v -> v.functionDefinition.name == null ? "" : v.functionDefinition.name))
                            {
                                MutableList<String> row = Lists.mutable.empty();
                                row.add("<div style='color:#AAAAAA'>" + d.reportScope.module + "</div>");
                                row.add(printFuncName(d));
                                row.add(printFuncSignatures(d));
                                if (!d.functionDefinition.signatures.isEmpty() && d.functionDefinition.signatures.get(0).platformOnly && adapterQualifiers.isEmpty())
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
                                    row.addAll(orderedAdapters.collect(a -> writeTest(d, a, testResultCountByAdapter, adapterQualifiers)));
                                }
                                node.addChild(new TreeNode(row));
                            }
                        }
                );
        addSuccessfulTestRate(orderedAdapters, testResultCountByAdapter, root);

        return top +
                "<BR/><BR/>\n" +
                "    <table style=\"border-spacing:20px 8px;width:900;text-align: center;max-height:70vh;overflow-y:auto\">\n" +
                "        <tr>\n" +
                "           <TH colspan='5' style='width:100'></TH>\n" +
                addGroups(orderedAdapters) +
                "        </tr>\n" +
                "        <tr>\n" +
                "           <th style='width:100'></th>\n" +
                "           <th style='width:10'>Group</th>\n" +
                "           <th style='width:200'>Function</th>\n" +
                "           <th style='width:200'>Signatures & Documentation</th>\n" +
                addHeaders(orderedAdapters) + "\n" +
                "        </tr>\n" +
                root.getChildren().collectWithIndex((n, i) -> addTableRow(n, "", String.valueOf(i), orderedAdapters)).makeString("\n") +
                "\n    </table>\n<BR/><BR/><BR/>\n"
                + bottom;
    }

    private static void addSuccessfulTestRate(MutableList<AdapterKey> orderedAdapters, MutableMap<AdapterKey, TestResultCount> testResultCountByAdapter, TreeNode root)
    {
        MutableList<String> row = Lists.mutable.empty();
        row.add("<div style='color:#AAAAAA'></div>");
        row.add("<div style='color:#AAAAAA'></div>");
        row.add("<div style='color:#AAAAAA'>" + "Successful Test Rate" + "</div>");
        row.addAll(orderedAdapters.collect(adapterKey ->
        {
            TestResultCount testResultCount = testResultCountByAdapter.get(adapterKey);
            int successfulTestCount = testResultCount == null ? 0 : testResultCount.successfulTestCount;
            int totalTestCount = testResultCount == null ? 0 : testResultCount.totalTestCount;
            if (totalTestCount > 0)
            {
                String tooltipElement = "<span style='color:#FFFFFF'>" + successfulTestCount + "/" + totalTestCount + "</span>";
                return "          <div style='color:#000000' class='hover-text'>" + (int) Math.floor((double) successfulTestCount / totalTestCount * 100) + "%<div class='tooltip-text' id='top'>" + tooltipElement + "</div>";
            }
            else
            {
                return "          <div style='color:#AAAAAA'>&empty;</div>";
            }
        }).toList());
        root.addChild(new TreeNode(row));
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

    private static class TestResultCount
    {
        protected int successfulTestCount;
        protected int totalTestCount;

        protected TestResultCount()
        {
            this.successfulTestCount = 0;
            this.totalTestCount = 0;
        }
    }

    private static String writeTest(FunctionDocumentation z, AdapterKey a, MutableMap<AdapterKey, TestResultCount> testResultCountByAdapter, Set<String> adapterQualifiers)
    {
        FunctionTestResults results = z.functionTestResults.get(a);
        if (results != null)
        {
            MutableList<TestInfo> tests = Lists.mutable.withAll(results.tests).select(t -> hasAdapterQualifier(t, adapterQualifiers));
            if (tests.isEmpty())
            {
                return "          <div style='color:#AAAAAA'>&empty;</div>";
            }
            int successfulTestCount = tests.select(t -> t.success).size();
            int totalTestCount = tests.size();
            TestResultCount testResultCount = (testResultCountByAdapter.containsKey(a)) ? testResultCountByAdapter.get(a) : new TestResultCount();
            testResultCount.successfulTestCount += successfulTestCount;
            testResultCount.totalTestCount += totalTestCount;
            testResultCountByAdapter.put(a, testResultCount);
            String color = successfulTestCount == 0 ? "#C70039" : successfulTestCount != totalTestCount ? "#FFA500" : "#00C72B";
            return "          <div style='color:" + color + "' class='hover-text'>" + successfulTestCount + "/" + totalTestCount + "<div class='tooltip-text' id='top'>" + testDetail(tests) + "</div></div>";
        }
        return "          <div style='color:#AAAAAA'>&empty;</div>";
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

    private static String printFuncSignatures(FunctionDocumentation functionDocumentation)
    {
        FunctionDefinition f = functionDocumentation.functionDefinition;

        if (f.signatures.isEmpty())
        {
            return "";
        }
        else
        {
            return f.signatures.stream().map(x -> "<td style='text-align: left;'>" + StringEscapeUtils.escapeHtml4(x.simple.substring(x.simple.indexOf(f.name + "("))) + "</td>" + "<td style='text-align: left;'>" + StringEscapeUtils.escapeHtml4(x.documentation != null ? x.documentation : "") + "</td>" ).collect(Collectors.joining("<tr></tr>", "<table style='width:1000; table-layout: fixed;'><tr>\n", "</tr></table>"));
        }
    }

    private static String addGroups(MutableList<AdapterKey> adapterKeys)
    {
        ListMultimap<String, AdapterKey> grouped = adapterKeys.groupBy(x -> x.adapter.group);
        return grouped.keysView().toSortedList().collect(x -> "<TH colspan='" + grouped.get(x).size() + "'>" + x + "</TH>").makeString("");
    }

    private static String addHeaders(MutableList<AdapterKey> adapterKeys)
    {
        return adapterKeys.collect(c -> c.adapter.name).collect(a -> "          <th style='width:10'>" + a + "</th>").makeString("\n");
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

    private static boolean hasAdapterQualifier(TestInfo test, Set<String> adapterQualifiers)
    {
        if (adapterQualifiers.isEmpty())
        {
            return !test.qualifiers.contains(AdapterQualifier.unsupportedFeature.name()) && !test.qualifiers.contains(AdapterQualifier.assertErrorMismatch.name());
        }

        return adapterQualifiers.contains("all") || !Collections.disjoint(test.qualifiers, adapterQualifiers);

    }

    private static String top = "<html>\n" +
            "    <head>\n" +
            "        <style>\n" +
            "            td {\n" +
            "               text-align: center;\n" +
            "               padding-top: 8px;\n" +
            "            }\n" +
            "\n" +
            "            th {\n" +
            "               position: sticky;\n" +
            "               top: 0;\n" +
            "               z-index: 1;\n" +
            "               background: white;\n" +
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
            "<footer style='font-size: 15px;'>\n" +
            String.format("PCT results as of %s using commit <a href='https://github.com/finos/legend-engine/tree/%s'>%s</a>.\n", DeploymentStateAndVersions.sdlc.commitTime, getCommitId(), getCommitId()) +
            "</footer>\n" +
                    "</html>";

    private static String getCommitId()
    {
        String commitId = DeploymentStateAndVersions.sdlc.commitId;
        return (commitId != null && !commitId.isEmpty()) ? commitId : "master";
    }
}
