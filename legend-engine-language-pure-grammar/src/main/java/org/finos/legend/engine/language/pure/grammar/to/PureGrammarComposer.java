// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionParser;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingParser;
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PureGrammarComposer
{
    private static final String DEFAULT_SECTION_NAME = "Pure";
    private final PureGrammarComposerContext context;

    private PureGrammarComposer(PureGrammarComposerContext context)
    {
        this.context = context;
    }

    public static PureGrammarComposer newInstance(PureGrammarComposerContext context)
    {
        return new PureGrammarComposer(context);
    }

    public String renderPureModelContextData(PureModelContextData pureModelContextData)
    {
        List<PackageableElement> elements = pureModelContextData.getAllElements();
        Set<PackageableElement> elementsToCompose = Sets.mutable.withAll(elements);
        FastList<String> composedSections = FastList.newList();
        if (!pureModelContextData.sectionIndices.isEmpty())
        {
            Map<String, PackageableElement> elementByPath = new HashMap<>();
            // NOTE: here we handle duplication, first element with the duplicated path wins
            elements.forEach(element -> elementByPath.putIfAbsent(element.getPath(), element));
            ListIterate.forEach(pureModelContextData.sectionIndices, sectionIndex -> this.renderSectionIndex((SectionIndex) sectionIndex, elementByPath, elementsToCompose, composedSections));
        }

        for (Function3<Set<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult> composer : this.context.extraFreeSectionComposers)
        {
            PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult result = composer.value(elementsToCompose, this.context, composedSections);
            if (result != null)
            {
                composedSections.add(result.value + "\n");
                // mark that the elements already been rendered by one of the extensions
                result.composedElements.forEach(elementsToCompose::remove);
            }
        }

        // FIXME: the following logic should be removed completely when we move to use extensions
        if (pureModelContextData.domain != null)
        {
            this.DEPRECATED_renderSection(DomainParser.name, Stream.of(
                    pureModelContextData.domain.classes,
                    pureModelContextData.domain.associations,
                    pureModelContextData.domain.enums,
                    pureModelContextData.domain.functions,
                    pureModelContextData.domain.profiles,
                    pureModelContextData.domain.measures
            ).filter(Objects::nonNull).flatMap(java.util.Collection::stream).collect(Collectors.toList()), elementsToCompose, composedSections);
        }
        this.DEPRECATED_renderSection(MappingParser.name, pureModelContextData.mappings, elementsToCompose, composedSections);
        this.DEPRECATED_renderSection(ConnectionParser.name, pureModelContextData.connections, elementsToCompose, composedSections);
        this.DEPRECATED_renderSection(RuntimeParser.name, pureModelContextData.runtimes, elementsToCompose, composedSections);
        return composedSections.select(section -> !section.isEmpty()).makeString("\n\n");
    }

    private void DEPRECATED_renderSection(String parserName, List<? extends PackageableElement> elements, Set<PackageableElement> elementsToCompose, List<String> composedSections)
    {
        List<? extends PackageableElement> els = ListIterate.select(elements, elementsToCompose::contains);
        els.forEach(elementsToCompose::remove);
        if (!els.isEmpty())
        {
            StringBuilder builder = new StringBuilder();
            builder.append(composedSections.size() > 0 || !parserName.equals(DEFAULT_SECTION_NAME) ? ("###" + parserName + "\n") : "");
            builder.append(LazyIterate.collect(els, this::DEPRECATED_renderElement).makeString("\n\n"));
            builder.append("\n");
            composedSections.add(builder.toString());
        }
    }

    private void renderSectionIndex(SectionIndex sectionIndex, Map<String, PackageableElement> elementByPath, Set<PackageableElement> elementsToCompose, List<String> composedSections)
    {
        List<Section> sections = sectionIndex.sections;
        ListIterate.forEach(sections, section ->
        {
            StringBuilder builder = new StringBuilder();
            builder.append(composedSections.size() > 0 || !section.parserName.equals(DEFAULT_SECTION_NAME) ? ("###" + section.parserName + "\n") : "");
            // NOTE: here we remove duplicates in both the imports and the content
            List<String> imports = section instanceof ImportAwareCodeSection ? ListIterate.distinct(((ImportAwareCodeSection) section).imports) : new ArrayList<>();
            if (!imports.isEmpty())
            {
                builder.append(LazyIterate.collect(imports, _import -> ("import " + PureGrammarComposerUtility.convertPath(_import) + "::*;")).makeString("\n"));
                builder.append("\n");
            }
            List<PackageableElement> elements = ListIterate.distinct(section.elements).collect(path ->
            {
                PackageableElement element = elementByPath.get(path);
                // mark that the elements already been rendered in one of the section
                elementsToCompose.remove(element);
                return element;
            }).select(Objects::nonNull);
            if (!elements.isEmpty())
            {
                builder.append(this.context.extraSectionComposers.stream().map(composer -> composer.value(elements, this.context, section.parserName)).filter(Objects::nonNull).findFirst()
                        // NOTE: this is the old way (no-plugin) way to render section elements, this approach is not great since it does not enforce
                        // the types of elements a section can have, the newer approach does the check and compose unsupported message when such violations occur
                        // TO BE REMOVED when we moved everything to extensions
                        .orElseGet(() -> LazyIterate.collect(elements, this::DEPRECATED_renderElement).makeString("\n\n")));
                builder.append("\n");
            }
            composedSections.add(builder.toString());
        });
    }

    private String DEPRECATED_renderElement(PackageableElement element)
    {
        return element.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(this.context).build());
    }
}
