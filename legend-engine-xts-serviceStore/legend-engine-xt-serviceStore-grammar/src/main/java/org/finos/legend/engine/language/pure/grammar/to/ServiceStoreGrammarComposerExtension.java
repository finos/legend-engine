// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ServiceStoreGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.data.ServiceStoreEmbeddedDataComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.ClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.RootServiceStoreClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.ServiceStore;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class ServiceStoreGrammarComposerExtension implements IServiceStoreGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!ServiceStoreGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof ServiceStore)
                {
                    return HelperServiceStoreGrammarComposer.renderServiceStore((ServiceStore) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<ServiceStore> composableElements = ListIterate.selectInstancesOf(elements, ServiceStore.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, HelperServiceStoreGrammarComposer::renderServiceStore).makeString("###" + ServiceStoreGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<ClassMapping, PureGrammarComposerContext, String>> getExtraClassMappingComposers()
    {
        return Lists.mutable.with((classMapping, context) ->
        {
            if (classMapping instanceof RootServiceStoreClassMapping)
            {
                RootServiceStoreClassMapping rootServiceStoreClassMapping = (RootServiceStoreClassMapping) classMapping;
                StringBuilder builder = new StringBuilder();
                builder.append(": ").append(ServiceStoreGrammarParserExtension.SERVICE_STORE_MAPPING_ELEMENT_TYPE).append("\n");
                builder.append(getTabString()).append("{\n");
                HelperServiceStoreGrammarComposer.visitRootServiceClassMappingContents(rootServiceStoreClassMapping, builder);
                builder.append(getTabString()).append("}");
                return builder.toString();
            }
            return null;
        });
    }

    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof ServiceStoreConnection)
            {
                ServiceStoreConnection serviceStoreConnection = (ServiceStoreConnection) connectionValue;

                return Tuples.pair(ServiceStoreGrammarParserExtension.SERVICE_STORE_CONNECTION_TYPE,
                        context.getIndentationString() + "{\n" +
                                context.getIndentationString() + getTabString() + "store: " + serviceStoreConnection.element + ";\n" +
                                context.getIndentationString() + getTabString() + "baseUrl: " + PureGrammarComposerUtility.convertString(serviceStoreConnection.baseUrl, true) + ";\n" +
                                context.getIndentationString() + "}");
            }
            return null;
        });
    }

    @Override
    public List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> getExtraEmbeddedDataComposers()
    {
        return Collections.singletonList(ServiceStoreEmbeddedDataComposer::composeServiceStoreEmbeddedData);
    }
}
