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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ExternalFormatGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.Binding;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchema;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatSchemaSet;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.UrlStreamExternalSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class ExternalFormatGrammarComposerExtension implements IExternalFormatGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!ExternalFormatGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, ExternalFormatGrammarComposerExtension::renderElement).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureGrammarComposerExtension.PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with((elements, context, composedSections) ->
        {
            List<PackageableElement> composableElements = ListIterate.selectInstancesOf(elements, ExternalFormatSchemaSet.class)
                    .collect(PackageableElement.class::cast)
                    .withAll(ListIterate.selectInstancesOf(elements, Binding.class));
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, ExternalFormatGrammarComposerExtension::renderElement).makeString("###" + ExternalFormatGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<Connection, PureGrammarComposerContext, Pair<String, String>>> getExtraConnectionValueComposers()
    {
        return Lists.mutable.with((connectionValue, context) ->
        {
            if (connectionValue instanceof ExternalFormatConnection)
            {
                return renderExternalFormatConnection((ExternalFormatConnection) connectionValue, context);
            }
            return null;
        });
    }

    private Pair<String, String> renderExternalFormatConnection(ExternalFormatConnection connection, PureGrammarComposerContext context)
    {
        List<IExternalFormatGrammarComposerExtension> extensions = IExternalFormatGrammarComposerExtension.getExtensions(context);

        String specification = IExternalFormatGrammarComposerExtension.process(connection.externalSource,
                ListIterate.flatCollect(extensions, IExternalFormatGrammarComposerExtension::getExtraExternalSourceSpecificationComposers),
                context);

        return Tuples.pair(
                ExternalFormatGrammarParserExtension.EXTERNAL_FORMAT_CONNECTION_TYPE,
                context.getIndentationString() + "{\n" +
                        (connection.element == null ? "" : context.getIndentationString() + getTabString(1) + "store: " + connection.element + ";\n") +
                        context.getIndentationString() + getTabString(1) + "source: " + specification + ";\n" +
                        context.getIndentationString() + "}");
    }

    @Override
    public List<Function2<ExternalSource, PureGrammarComposerContext, String>> getExtraExternalSourceSpecificationComposers()
    {
        return Lists.mutable.with((specification, context) ->
        {
            if (specification instanceof UrlStreamExternalSource)
            {
                UrlStreamExternalSource spec = (UrlStreamExternalSource) specification;
                int baseIndentation = 1;
                return "UrlStream\n" +
                        context.getIndentationString() + getTabString(1) + "{\n" +
                        context.getIndentationString() + getTabString(2) + "url: " + convertString(spec.url, true) + ";\n" +
                        context.getIndentationString() + getTabString(1) + "}";
            }

            return null;
        });
    }

    private static String renderElement(PackageableElement element)
    {
        if (element instanceof ExternalFormatSchemaSet)
        {
            return renderSchemaSet((ExternalFormatSchemaSet) element);
        }
        else if (element instanceof Binding)
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
