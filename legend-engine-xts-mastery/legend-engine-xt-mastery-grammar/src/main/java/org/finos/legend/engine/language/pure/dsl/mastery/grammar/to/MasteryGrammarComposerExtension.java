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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.MasteryParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.MasterRecordDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.dataProvider.DataProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class MasteryGrammarComposerExtension implements IMasteryComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mastery");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof MasterRecordDefinition)
        {
            return renderMasterRecordDefinition((MasterRecordDefinition) element, context);
        }
        if (element instanceof DataProvider)
        {
            return renderDataProvider((DataProvider) element, context);
        }
        if (element instanceof Connection)
        {
            return renderConnection((Connection) element, context);
        }
        if (element instanceof MasteryRuntime)
        {
            return renderMasteryRuntime((MasteryRuntime) element, context);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(MasteryParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.of((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Lists.mutable.empty();
            composableElements.addAll(ListIterate.selectInstancesOf(elements, MasterRecordDefinition.class));
            composableElements.addAll(ListIterate.selectInstancesOf(elements, Connection.class));
            composableElements.addAll(ListIterate.selectInstancesOf(elements, DataProvider.class));
            composableElements.addAll(ListIterate.selectInstancesOf(elements, MasteryRuntime.class));

            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements
                    .collect(element ->
                    {
                        if (element instanceof MasterRecordDefinition)
                        {
                            return MasteryGrammarComposerExtension.renderMasterRecordDefinition((MasterRecordDefinition) element, context);
                        }
                        else if (element instanceof DataProvider)
                        {
                            return MasteryGrammarComposerExtension.renderDataProvider((DataProvider) element, context);
                        }
                        else if (element instanceof Connection)
                        {
                            return MasteryGrammarComposerExtension.renderConnection((Connection) element, context);
                        }
                        else if (element instanceof MasteryRuntime)
                        {
                            return MasteryGrammarComposerExtension.renderMasteryRuntime((MasteryRuntime) element, context);
                        }
                        throw new UnsupportedOperationException("Unsupported type " + element.getClass().getName());
                    })
                    .makeString("###" + MasteryParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderMasterRecordDefinition(MasterRecordDefinition masterRecordDefinition, PureGrammarComposerContext context)
    {
        return HelperMasteryGrammarComposer.renderMasterRecordDefinition(masterRecordDefinition, 1, context);
    }

    private static String renderDataProvider(DataProvider dataProvider, PureGrammarComposerContext context)
    {
        return HelperMasteryGrammarComposer.renderDataProvider(dataProvider);
    }

    private static String renderConnection(Connection connection, PureGrammarComposerContext context)
    {
        List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
        return IMasteryComposerExtension.process(connection, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraMasteryConnectionComposers), 1, context);
    }

    private static String renderMasteryRuntime(MasteryRuntime masteryRuntime, PureGrammarComposerContext context)
    {
        List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
        return IMasteryComposerExtension.process(masteryRuntime, ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraMasteryRuntimeComposers), 1, context);
    }

    @Override
    public List<Function3<Connection, Integer, PureGrammarComposerContext, String>> getExtraMasteryConnectionComposers()
    {
        return Collections.singletonList(HelperConnectionComposer::renderConnection);
    }

    @Override
    public List<Function3<Trigger, Integer, PureGrammarComposerContext, String>> getExtraTriggerComposers()
    {
        return Collections.singletonList(HelperTriggerComposer::renderTrigger);
    }

    @Override
    public List<Function3<AuthenticationStrategy, Integer, PureGrammarComposerContext, String>> getExtraAuthenticationStrategyComposers()
    {
        return Collections.singletonList(HelperAuthenticationComposer::renderAuthentication);
    }

    @Override
    public List<Function3<AcquisitionProtocol, Integer, PureGrammarComposerContext, String>> getExtraAcquisitionProtocolComposers()
    {
        return Collections.singletonList(HelperAcquisitionComposer::renderAcquisition);
    }
}
