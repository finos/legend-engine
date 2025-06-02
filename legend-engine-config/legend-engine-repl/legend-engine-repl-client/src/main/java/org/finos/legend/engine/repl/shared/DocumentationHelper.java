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

package org.finos.legend.engine.repl.shared;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.eclipse.collections.impl.utility.MapIterate;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.shared.core.deployment.DeploymentStateAndVersions;
import org.finos.legend.pure.m3.pct.aggregate.model.Documentation;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.FunctionDefinition;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.reports.model.AdapterKey;
import org.finos.legend.pure.m3.pct.reports.model.TestInfo;

import java.util.Objects;

import static org.finos.legend.engine.repl.shared.REPLHelper.*;

public class DocumentationHelper
{
    private static final MutableMap<String, String> MODULE_URLS = org.eclipse.collections.impl.factory.Maps.mutable.empty();

    static
    {
        String commitId = DeploymentStateAndVersions.sdlc.commitId;
        String commitIdOrMaster = (commitId != null && !commitId.isEmpty()) ? commitId : "master";
        MODULE_URLS.put("grammar", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        MODULE_URLS.put("essential", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        MODULE_URLS.put("standard", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-standard/legend-engine-pure-functions-standard-pure/src/main/resources", commitIdOrMaster));
        MODULE_URLS.put("relation", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources", commitIdOrMaster));
        MODULE_URLS.put("unclassified", String.format("https://github.com/finos/legend-engine/tree/%s/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-unclassified/legend-engine-pure-functions-unclassified-pure/src/main/resources", commitIdOrMaster));
    }

    private static final int ANSI_ATTR_WIDTH = 8;

    public static String generateANSIFunctionDocumentation(FunctionDocumentation functionDocumentation, MutableList<AdapterKey> adapterKeys)
    {
        StringBuilder builder = new StringBuilder();
        FunctionDefinition definition = functionDocumentation.functionDefinition;
        String name = definition.name;
        String path = definition._package + "::" + name;
        String src = MODULE_URLS.get(functionDocumentation.reportScope.module) + definition.sourceId;
        String grouping = definition.sourceId.substring(functionDocumentation.reportScope.filePath.length(), definition.sourceId.lastIndexOf("/"));
        // NOTE: make assumption that each function has doc/usage on exactly one of the signatures
        String syntax = ListIterate.detectOptional(definition.signatures, signature -> signature.grammarCharacter != null).map(s -> s.grammarCharacter).orElse(null);
        String doc = ListIterate.detectOptional(definition.signatures, signature -> signature.documentation != null).map(s -> s.documentation).orElse(null);
        String usage = ListIterate.detectOptional(definition.signatures, signature -> signature.grammarDoc != null).map(s -> s.grammarDoc).orElse(null);

        Lists.mutable.with(syntax != null ? ansiGreen(syntax) : null)
                .with(name)
                .withAll(ListIterate.collect(definition.signatures, s -> s.simple.substring(definition._package.length() + 2)))
                .select(Objects::nonNull)
                .forEachWithIndex((value, idx) -> builder.append(ansiAttr(idx == 0 ? "function" : null)).append(value).append("\n"));
        builder.append(ansiAttr("path")).append(path).append("\n");
        builder.append(ansiAttr("grouping")).append("(" + functionDocumentation.reportScope.module + ") " + grouping).append("\n");
        builder.append(ansiAttr("src")).append(src).append("\n");
        if (doc != null)
        {
            builder.append(ansiAttr("doc")).append(ArrayIterate.makeString(wrap(doc).split("\n"), "\n" + ansiAttr(null))).append("\n");
        }
        if (usage != null)
        {
            builder.append(ansiAttr("usage")).append(usage).append("\n");
        }

        // compatibility
        MutableMap<String, String> matrix = MapIterate.collect(functionDocumentation.functionTestResults, (adapterKey, testResults) ->
        {
            String key = adapterKey.adapter.group + (adapterKey.adapter.group.isEmpty() ? "" : "/") + adapterKey.adapter.name;
            MutableList<TestInfo> tests = Lists.mutable.withAll(testResults.tests);
            String value;
            if (tests.isEmpty())
            {
                value = ansiDim("∅");
            }
            else
            {
                int passedCount = tests.select(t -> t.success).size();
                value = passedCount + "/" + tests.size();
                value = passedCount == 0 ? ansiRed(value) : passedCount == tests.size() ? ansiGreen(value) : ansiYellow(value);
            }
            return Tuples.pair(key, value);
        });
        // NOTE: here we sort the adapter naively, and it achieves the desired order anyway,
        // but we should consider a more methodical/intentional sort: e.g. native goes first, followed by platforms and stores
        MutableList<String> adapters = adapterKeys.collect(adapterKey -> adapterKey.adapter.group + (adapterKey.adapter.group.isEmpty() ? "" : "/") + adapterKey.adapter.name).toSortedList();
        int maxKeyLength = adapters.collect(String::length).max();

        builder.append("\n").append(StringUtils.rightPad("compatibility", maxKeyLength + 2)).append(" :").append("\n");
        builder.append(adapters.collect(adapter -> StringUtils.rightPad("  " + adapter, maxKeyLength + 2) + " : " + matrix.getOrDefault(adapter, "∅")).makeString("\n"));
        return builder.toString();
    }

    private static String ansiAttr(String attr)
    {
        if (attr == null)
        {
            return StringUtils.rightPad("", ANSI_ATTR_WIDTH + 3);
        }
        return StringUtils.rightPad(attr, ANSI_ATTR_WIDTH) + " : ";
    }

    public abstract static class Walkthrough
    {
        protected final Client client;
        private int currentStep = 0;

        public Walkthrough(Client client)
        {
            this.client = client;
        }

        protected abstract void beforeStep();

        protected abstract void afterStep();

        protected abstract MutableList<Function0<Void>> getSteps();

        public int getCurrentStep()
        {
            return currentStep;
        }

        public int getStepCount()
        {
            return getSteps().size();
        }

        private void run(int step)
        {
            getSteps().get(step).value();
        }

        public void current()
        {
            try
            {
                this.beforeStep();
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }
            this.client.clearScreen();
            this.run(currentStep);

            this.afterStep();
        }

        public void next()
        {
            currentStep++;
            if (currentStep >= this.getStepCount())
            {
                currentStep = 0;
            }
            this.current();
        }

        public void prev()
        {
            currentStep--;
            if (currentStep < 0)
            {
                currentStep = 0;
            }
            this.current();
        }

        public void restart()
        {
            currentStep = 0;
            this.current();
        }
    }
}
