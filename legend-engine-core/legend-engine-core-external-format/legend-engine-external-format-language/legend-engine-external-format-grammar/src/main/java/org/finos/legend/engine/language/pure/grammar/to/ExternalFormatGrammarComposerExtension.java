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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.language.pure.grammar.from.ExternalFormatGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.Binding;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatSchemaSet;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class ExternalFormatGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("External_Format", "-Core");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) -> renderElement(element));

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer(ExternalFormatGrammarParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) ->
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof ExternalFormatSchemaSet) || (e instanceof Binding), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(ExternalFormatGrammarComposerExtension::renderElement).makeString("###" + ExternalFormatGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderElement(PackageableElement element)
    {
        if (element instanceof ExternalFormatSchemaSet)
        {
            return renderSchemaSet((ExternalFormatSchemaSet) element);
        }
        if (element instanceof Binding)
        {
            return renderSchemaBinding((Binding) element);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderSchemaSet(ExternalFormatSchemaSet schemaSet)
    {
        StringBuilder builder = new StringBuilder()
                .append("SchemaSet ").append(PureGrammarComposerUtility.convertPath(schemaSet.getPath()))
                .append("\n{\n");

        PureGrammarComposerUtility.appendTabString(builder).append("format: ").append(schemaSet.format).append(";\n");
        PureGrammarComposerUtility.appendTabString(builder).append("schemas: [\n");
        for (int i = 0; i < schemaSet.schemas.size(); i++)
        {
            ExternalFormatSchema schema = schemaSet.schemas.get(i);
            PureGrammarComposerUtility.appendTabString(builder, 2).append("{\n");
            if (schema.id != null)
            {
                PureGrammarComposerUtility.appendTabString(builder, 3).append("id: ").append(PureGrammarComposerUtility.convertIdentifier(schema.id)).append(";\n");
            }
            if (schema.location != null)
            {
                PureGrammarComposerUtility.appendTabString(builder, 3).append("location: ").append(PureGrammarComposerUtility.convertString(schema.location, true)).append(";\n");
            }
            PureGrammarComposerUtility.appendTabString(builder, 3).append("content: ").append(PureGrammarComposerUtility.convertString(schema.content, true)).append(";\n");
            PureGrammarComposerUtility.appendTabString(builder, 2).append("}").append(i < schemaSet.schemas.size() - 1 ? ",\n" : "\n");
        }
        PureGrammarComposerUtility.appendTabString(builder).append("];\n}");

        return builder.toString();
    }

    private static String renderSchemaBinding(Binding schemaBinding)
    {
        StringBuilder builder = new StringBuilder()
                .append("Binding ").append(PureGrammarComposerUtility.convertPath(schemaBinding.getPath()))
                .append("\n{\n");

        if (schemaBinding.schemaSet != null)
        {
            PureGrammarComposerUtility.appendTabString(builder).append("schemaSet: ").append(PureGrammarComposerUtility.convertPath(schemaBinding.schemaSet)).append(";\n");
            if (schemaBinding.schemaId != null)
            {
                PureGrammarComposerUtility.appendTabString(builder).append("schemaId: ").append(PureGrammarComposerUtility.convertIdentifier(schemaBinding.schemaId)).append(";\n");
            }
        }
        PureGrammarComposerUtility.appendTabString(builder).append("contentType: ").append(PureGrammarComposerUtility.convertString(schemaBinding.contentType, true)).append(";\n");

        ModelUnit modelUnit = schemaBinding.modelUnit;
        PureGrammarComposerUtility.appendTabString(builder).append("modelIncludes: [\n");
        builder.append(modelUnit.packageableElementIncludes.stream().map(pe -> PureGrammarComposerUtility.getTabString(2) + PureGrammarComposerUtility.convertPath(pe)).collect(Collectors.joining(",\n"))).append("\n");
        PureGrammarComposerUtility.appendTabString(builder).append("];\n");
        if (!modelUnit.packageableElementExcludes.isEmpty())
        {
            PureGrammarComposerUtility.appendTabString(builder).append("modelExcludes: [\n");
            builder.append(modelUnit.packageableElementExcludes.stream().map(pe -> PureGrammarComposerUtility.getTabString(2) + PureGrammarComposerUtility.convertPath(pe)).collect(Collectors.joining(",\n"))).append("\n");
            PureGrammarComposerUtility.appendTabString(builder).append("];\n");
        }

        return builder.append("}").toString();
    }
}
