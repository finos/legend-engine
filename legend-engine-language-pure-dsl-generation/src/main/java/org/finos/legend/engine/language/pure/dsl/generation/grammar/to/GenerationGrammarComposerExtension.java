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

package org.finos.legend.engine.language.pure.dsl.generation.grammar.to;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.generation.grammar.from.GenerationParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.ConfigurationProperty;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.generationSpecification.GenerationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.schemaGeneration.SchemaGenerationSpecification;

public class GenerationGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(
                (elements, context, sectionName) ->
                {
                    if (!GenerationParserExtension.FILE_GENERATION_SECTION_NAME.equals(sectionName))
                    {
                        return null;
                    }
                    return ListIterate.collect(elements, element ->
                    {
                        if (element instanceof SchemaGenerationSpecification)
                        {
                            return renderSchemaGenerationSpecification((SchemaGenerationSpecification) element);
                        }
                        else if (element instanceof FileGenerationSpecification)
                        {
                            return renderFileGenerationSpecification((FileGenerationSpecification) element);
                        }
                        return "/* Can't transform element '" + element.getPath() + "' in this section */";
                    }).makeString("\n\n");
                },
                (elements, context, sectionName) ->
                {
                    if (!GenerationParserExtension.GENERATION_SPECIFICATION_SECTION_NAME.equals(sectionName))
                    {
                        return null;
                    }
                    return ListIterate.collect(elements, element ->
                    {
                        if (element instanceof GenerationSpecification)
                        {
                            return renderGenerationSpecification((GenerationSpecification) element);
                        }
                        return "/* Can't transform element '" + element.getPath() + "' in this section */";
                    }).makeString("\n\n");
                }
        );
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with(
            (elements, context, composedSections) ->
            {
                {
                    List<SchemaGenerationSpecification> composableElements = ListIterate.selectInstancesOf(elements, SchemaGenerationSpecification.class);
                    return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, GenerationGrammarComposerExtension::renderSchemaGenerationSpecification).makeString("###" + GenerationParserExtension.FILE_GENERATION_SECTION_NAME + "\n", "\n\n", ""), composableElements);
                }
            },
                (elements, context, composedSections) ->
                {
                    List<FileGenerationSpecification> composableElements = ListIterate.selectInstancesOf(elements, FileGenerationSpecification.class);
                    return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, GenerationGrammarComposerExtension::renderFileGenerationSpecification).makeString("###" + GenerationParserExtension.FILE_GENERATION_SECTION_NAME + "\n", "\n\n", ""), composableElements);
                },
                (elements, context, composedSections) ->
                {
                    List<GenerationSpecification> composableElements = ListIterate.selectInstancesOf(elements, GenerationSpecification.class);
                    return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, GenerationGrammarComposerExtension::renderGenerationSpecification).makeString("###" + GenerationParserExtension.GENERATION_SPECIFICATION_SECTION_NAME + "\n", "\n\n", ""), composableElements);
                }
        );
    }

    private static String renderGenerationSpecification(GenerationSpecification generationSpecification)
    {
        return "GenerationSpecification " + PureGrammarComposerUtility.convertPath(generationSpecification.getPath()) +
                "\n{\n" +
                (generationSpecification.generationNodes.isEmpty() ? "" :
                        "  generationNodes: [\n"
                                + LazyIterate.collect(generationSpecification.generationNodes, HelperGenerationSpecificationGrammarComposer::renderGenerationNode).makeString(",\n") + (generationSpecification.generationNodes.isEmpty() ? "" : "\n") +
                                "  ];\n"
                ) +
                HelperGenerationSpecificationGrammarComposer.renderFileGenerationNode(generationSpecification.fileGenerations) + (generationSpecification.fileGenerations.isEmpty() ? "" : "\n") +
                "}";
    }


    private static String renderSchemaGenerationSpecification(SchemaGenerationSpecification schemaGenerationSpecification)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SchemaGeneration " + PureGrammarComposerUtility.convertPath(schemaGenerationSpecification.getPath()));
        stringBuilder.append("\n{\n");
        if (schemaGenerationSpecification.format != null)
        {
            stringBuilder.append(getTabString()).append("format: '" + schemaGenerationSpecification.format + "';\n");
        }
        ModelUnit modelUnit = schemaGenerationSpecification.modelUnit;
        if (modelUnit != null)
        {
            if (modelUnit.packageableElementIncludes != null && !modelUnit.packageableElementIncludes.isEmpty())
            {
                PureGrammarComposerUtility.appendTabString(stringBuilder).append("modelIncludes: [\n");
                stringBuilder.append(modelUnit.packageableElementIncludes.stream().map(pe -> PureGrammarComposerUtility.getTabString(2) + PureGrammarComposerUtility.convertPath(pe)).collect(Collectors.joining(",\n"))).append("\n");
                PureGrammarComposerUtility.appendTabString(stringBuilder).append("];\n");
            }

            if (modelUnit.packageableElementExcludes != null &&  !modelUnit.packageableElementExcludes.isEmpty())
            {
                PureGrammarComposerUtility.appendTabString(stringBuilder).append("modelExcludes: [\n");
                stringBuilder.append(modelUnit.packageableElementExcludes.stream().map(pe -> PureGrammarComposerUtility.getTabString(2) + PureGrammarComposerUtility.convertPath(pe)).collect(Collectors.joining(",\n"))).append("\n");
                PureGrammarComposerUtility.appendTabString(stringBuilder).append("];\n");
            }
        }
        stringBuilder.append(renderSchemaGenerationConfig(schemaGenerationSpecification));
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    private static String renderSchemaGenerationConfig(SchemaGenerationSpecification schemaGenerationSpecification)
    {
        List<ConfigurationProperty> configuration = schemaGenerationSpecification.config;
        if (configuration == null || configuration.isEmpty())
        {
            return "";
        }
        return "config: {\n" + LazyIterate.collect(configuration, c -> getTabString() + HelperFileGenerationGrammarComposer.renderFileGenerationPropertyValue(c)).makeString("\n") + "\n" + "};\n";
    }

    private static String renderFileGenerationSpecification(FileGenerationSpecification fileGeneration)
    {
        StringBuilder builder = new StringBuilder();
        if (fileGeneration.type != null)
        {
            String driverType = fileGeneration.type.substring(0, 1).toUpperCase() + fileGeneration.type.substring(1);
            builder.append(driverType);
        }
        builder.append(" ").append(PureGrammarComposerUtility.convertPath(fileGeneration.getPath()));
        builder.append("\n{\n");
        if (!fileGeneration.scopeElements.isEmpty())
        {
            builder.append(getTabString()).append("scopeElements: [");
            builder.append(String.join(", ", fileGeneration.scopeElements));
            builder.append("];\n");
        }
        if (fileGeneration.generationOutputPath != null)
        {
            builder.append(getTabString()).append("generationOutputPath: ").append(convertString(fileGeneration.generationOutputPath, true)).append(";\n");
        }
        if (!fileGeneration.configurationProperties.isEmpty())
        {
            builder.append(LazyIterate.collect(fileGeneration.configurationProperties, c -> getTabString() + HelperFileGenerationGrammarComposer.renderFileGenerationPropertyValue(c)).makeString("\n")).append("\n");
        }
        builder.append("}");
        return builder.toString();
    }
}
