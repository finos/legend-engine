// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to.data;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.data.contentPattern.HelperContentPatternGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceRequestPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceResponseDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStoreEmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ServiceStubMapping;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.StringValuePattern;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperServiceStoreEmbeddedDataComposer
{
    private PureGrammarComposerContext context;

    public HelperServiceStoreEmbeddedDataComposer(PureGrammarComposerContext context)
    {
        this.context = context;
    }

    public String visitServiceStoreEmbeddedData(ServiceStoreEmbeddedData serviceStoreEmbeddedData)
    {
        String indentString = context.getIndentationString();
        return ListIterate.collect(serviceStoreEmbeddedData.serviceStubMappings, stub -> this.visitServiceStubMapping(stub, indentString + getTabString())).makeString(indentString + "[\n", ",\n", "\n" + indentString + "]");
    }

    private String visitServiceStubMapping(ServiceStubMapping serviceStubMapping, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(baseIndentation).append("{\n");
        str.append(visitServiceRequestPattern(serviceStubMapping.requestPattern, baseIndentation + getTabString())).append("\n");
        str.append(visitServiceResponseDefinition(serviceStubMapping.responseDefinition, baseIndentation + getTabString())).append("\n");
        str.append(baseIndentation).append("}");

        return str.toString();
    }

    private String visitServiceRequestPattern(ServiceRequestPattern serviceStubMapping, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(baseIndentation).append("request:\n");
        str.append(baseIndentation).append("{\n");
        str.append(baseIndentation).append(getTabString()).append("method: ").append(serviceStubMapping.method).append(";\n");
        if (serviceStubMapping.url != null)
        {
            str.append(baseIndentation).append(getTabString()).append("url: ").append(PureGrammarComposerUtility.convertString(serviceStubMapping.url, true)).append(";\n");
        }
        if (serviceStubMapping.urlPath != null)
        {
            str.append(baseIndentation).append(getTabString()).append("urlPath: ").append(PureGrammarComposerUtility.convertString(serviceStubMapping.urlPath, true)).append(";\n");
        }
        if (serviceStubMapping.headerParams != null && serviceStubMapping.headerParams.size() > 0)
        {
            MutableList<String> parametersContent = Lists.mutable.empty();
            serviceStubMapping.headerParams.forEach((key, value) -> parametersContent.add(visitRequestParameter(key, value, baseIndentation + getTabString(2))));

            str.append(baseIndentation).append(getTabString()).append("headerParameters:\n");
            str.append(baseIndentation).append(getTabString()).append("{\n");
            str.append(parametersContent.makeString(",\n")).append("\n");
            str.append(baseIndentation).append(getTabString()).append("};\n");
        }
        if (serviceStubMapping.queryParams != null && serviceStubMapping.queryParams.size() > 0)
        {
            MutableList<String> parametersContent = Lists.mutable.empty();
            serviceStubMapping.queryParams.forEach((key, value) -> parametersContent.add(visitRequestParameter(key, value, baseIndentation + getTabString(2))));

            str.append(baseIndentation).append(getTabString()).append("queryParameters:\n");
            str.append(baseIndentation).append(getTabString()).append("{\n");
            str.append(parametersContent.makeString(",\n")).append("\n");
            str.append(baseIndentation).append(getTabString()).append("};\n");
        }
        if (serviceStubMapping.bodyPatterns != null && serviceStubMapping.bodyPatterns.size() > 0)
        {
            str.append(baseIndentation).append(getTabString()).append("bodyPatterns:\n");
            str.append(baseIndentation).append(getTabString()).append("[\n");
            str.append(ListIterate.collect(serviceStubMapping.bodyPatterns, pattern -> visitStringValuePattern(pattern, baseIndentation + getTabString(2))).makeString(",\n")).append("\n");
            str.append(baseIndentation).append(getTabString()).append("];\n");
        }
        str.append(baseIndentation).append("};");
        return str.toString();
    }

    private String visitRequestParameter(String paramName, StringValuePattern stringValuePattern, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();
        str.append(baseIndentation).append(paramName).append(":\n");
        str.append(visitStringValuePattern(stringValuePattern, baseIndentation + getTabString()));

        return str.toString();
    }

    private String visitStringValuePattern(StringValuePattern stringValuePattern, String baseIndentation)
    {
        return HelperContentPatternGrammarComposer.composeContentPattern(stringValuePattern, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(baseIndentation).build());
    }

    private String visitServiceResponseDefinition(ServiceResponseDefinition serviceStubMapping, String baseIndentation)
    {
        StringBuilder str = new StringBuilder();

        str.append(baseIndentation).append("response:\n");
        str.append(baseIndentation).append("{\n");
        str.append(baseIndentation).append(getTabString()).append("body:\n");
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(serviceStubMapping.body, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(baseIndentation + getTabString(2)).build())).append(";\n");
        str.append(baseIndentation).append("};");

        return str.toString();
    }
}
