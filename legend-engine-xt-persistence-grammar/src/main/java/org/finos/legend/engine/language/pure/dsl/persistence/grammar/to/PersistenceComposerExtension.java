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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.to;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.PersistenceParserExtension;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.test.assertion.AllRowsEquivalentToJsonGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.Persistence;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.DefaultPersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.test.assertion.AllRowsEquivalentToJson;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.CronTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

import java.util.Collections;
import java.util.List;

public class PersistenceComposerExtension implements IPersistenceComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.fixedSize.of((elements, context, sectionName) ->
        {
            if (!PersistenceParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof Persistence)
                {
                    return renderPersistence((Persistence) element, context);
                }
                else if (element instanceof PersistenceContext)
                {
                    return renderPersistenceContext((PersistenceContext) element, context);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.of((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Lists.mutable.empty();
            composableElements.addAll(ListIterate.selectInstancesOf(elements, PersistenceContext.class));
            composableElements.addAll(ListIterate.selectInstancesOf(elements, Persistence.class));

            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements
                        .collect(element ->
                        {
                            if (element instanceof Persistence)
                            {
                                return PersistenceComposerExtension.renderPersistence((Persistence) element, context);
                            }
                            else if (element instanceof PersistenceContext)
                            {
                                return PersistenceComposerExtension.renderPersistenceContext((PersistenceContext) element, context);
                            }
                            throw new UnsupportedOperationException("Unsupported type " + element.getClass().getName());
                        })
                        .makeString("###" + PersistenceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function3<PersistencePlatform, Integer, PureGrammarComposerContext, String>> getExtraPersistencePlatformComposers()
    {
        return Collections.singletonList((persistencePlatform, indentLevel, context) ->
            persistencePlatform instanceof DefaultPersistencePlatform
                    ? ""
                    : null);
    }

    @Override
    public List<Function3<Trigger, Integer, PureGrammarComposerContext, String>> getExtraTriggerComposers()
    {
        return Collections.singletonList((trigger, indentLevel, context) ->
                {
                    if (trigger instanceof ManualTrigger)
                    {
                        return "Manual";
                    }
                    else if (trigger instanceof CronTrigger)
                    {
                        //TODO: ledav -- implement cron render
                        return null;
                    }
                    return null;
                });
    }

    @Override
    public List<Function2<TestAssertion, PureGrammarComposerContext, ContentWithType>> getExtraTestAssertionComposers()
    {
        return Lists.mutable.with(PersistenceComposerExtension::composeTestAssertionForPersistence);
    }

    private static ContentWithType composeTestAssertionForPersistence(TestAssertion testAssertion, PureGrammarComposerContext context)
    {
        String indentedString = context.getIndentationString() + PureGrammarComposerUtility.getTabString(1);
        PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentedString).build();

        if (testAssertion instanceof AllRowsEquivalentToJson)
        {
            AllRowsEquivalentToJson allRowsEquivalentToJson = (AllRowsEquivalentToJson) testAssertion;
            String content = context.getIndentationString() + "expected : \n"
                + HelperEmbeddedDataGrammarComposer.composeEmbeddedData(allRowsEquivalentToJson.expected, updatedContext) + ";";

            return new ContentWithType(AllRowsEquivalentToJsonGrammarParser.TYPE, content);
        }
        else
        {
            return null;
        }
    }

    private static String renderPersistence(Persistence persistence, PureGrammarComposerContext context)
    {
        return HelperPersistenceComposer.renderPersistence(persistence, 1, context);
    }

    private static String renderPersistenceContext(PersistenceContext persistenceContext, PureGrammarComposerContext context)
    {
        return HelperPersistenceContextComposer.renderPersistenceContext(persistenceContext, 1, context);
    }
}
