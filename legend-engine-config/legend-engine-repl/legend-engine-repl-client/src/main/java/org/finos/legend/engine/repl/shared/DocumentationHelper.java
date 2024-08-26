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
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.pure.m3.pct.aggregate.model.FunctionDocumentation;
import org.finos.legend.pure.m3.pct.functions.model.Signature;

import static org.finos.legend.engine.repl.shared.REPLHelper.ansiDim;
import static org.finos.legend.engine.repl.shared.REPLHelper.ansiGreen;

public class DocumentationHelper
{
    private static final MutableMap<String, String> MODULE_URLS = org.eclipse.collections.impl.factory.Maps.mutable.empty();

    static
    {
        MODULE_URLS.put("grammar", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        MODULE_URLS.put("essential", "https://github.com/finos/legend-pure/tree/master/legend-pure-core/legend-pure-m3-core/src/main/resources");
        MODULE_URLS.put("standard", "https://github.com/finos/legend-engine/tree/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-base/legend-engine-pure-functions-standard/src/main/resources/");
        MODULE_URLS.put("relation", "https://github.com/finos/legend-engine/tree/master/legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-relation/legend-engine-pure-functions-relation-pure/src/main/resources");
    }

    private static final int ANSI_ATTR_WIDTH = 20;

    public static String generateANSIFunctionDocumentation(FunctionDocumentation functionDocumentation)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(ansiAttr("function")).append(ansiGreen(functionDocumentation.reportScope._package + "::" + functionDocumentation.functionDefinition.name + "()")).append("\n");
        builder.append(ansiAttr("  [src]")).append(getFunctionSourceUrl(functionDocumentation)).append("\n");
        builder.append("\n");
        for (int i = 0; i < functionDocumentation.functionDefinition.signatures.size(); i++)
        {
            Signature signature = functionDocumentation.functionDefinition.signatures.get(i);
            builder.append(ansiGreen(ansiAttr("#[" + (i + 1) + "]")));
            if (signature.grammarCharacter != null)
            {
                builder.append(ansiGreen(signature.grammarCharacter));
                builder.append("\n").append(ansiAttr(""));
            }
            builder.append(ansiGreen(signature.simple)).append("\n");
            builder.append(ansiAttr("  [id]")).append(ansiDim(signature.id)).append("\n");
            if (signature.documentation != null)
            {
                builder.append(ansiAttr("  [doc]")).append(signature.documentation).append("\n");
            }
            if (signature.grammarDoc != null)
            {
                builder.append(ansiAttr("  [usage]")).append(signature.grammarDoc).append("\n");
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    private static String ansiAttr(String attr)
    {
        return StringUtils.rightPad(attr, ANSI_ATTR_WIDTH);
    }

    private static String getFunctionSourceUrl(FunctionDocumentation functionDocumentation)
    {
        return MODULE_URLS.get(functionDocumentation.reportScope.module) + functionDocumentation.functionDefinition.sourceId;
    }

    public abstract static class Walkthrough
    {
        protected final Client client;
        private int currentStep = 0;

        public Walkthrough(Client client)
        {
            this.client = client;
        }

        protected abstract String getCommand(int step);

        protected abstract void run(int step);

        protected abstract void prepare();

        protected abstract int getStepCount();

        public void current()
        {
            try
            {
                this.prepare();
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }

            this.client.clearScreen();
            this.run(currentStep);
        }

        public void runCommand(String command)
        {
            try
            {
                this.prepare();
            }
            catch (Exception ex)
            {
                throw new RuntimeException(ex);
            }

            if (command != null)
            {
                client.runCommandInBackground(command);
            }
        }

        public void next()
        {
            currentStep++;
            if (currentStep >= getStepCount())
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
