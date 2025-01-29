//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.dataquality.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.dataquality.grammar.from.DataQualityGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQuality;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityPropertyGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataQualityRootGraphFetchTree;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataSpaceDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.DataqualityRelationValidation;
import org.finos.legend.engine.protocol.dataquality.metamodel.MappingAndRuntimeDataQualityExecutionContext;
import org.finos.legend.engine.protocol.dataquality.metamodel.RelationValidation;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.graph.SubTypeGraphFetchTree;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer.renderAnnotations;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabSize;

public class DataQualityGrammarComposerExtension implements PureGrammarComposerExtension
{
    private static final int initialTabSize = 3;
    private static final RenderStyle renderStyle = RenderStyle.PRETTY; // get this from context

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataQualityValidation");
    }

    private static String renderElement(PackageableElement element, PureGrammarComposerContext context)
    {
        if (element instanceof DataQuality)
        {
            return renderDataQuality((DataQuality) element, context);
        }
        if (element instanceof DataqualityRelationValidation)
        {
            return renderDataQualityRelationValidation((DataqualityRelationValidation) element, context);
        }
        return "/* Can't transform element '" + element.getPath() + "' in this section */";
    }

    private static String renderDataQuality(DataQuality dataQuality, PureGrammarComposerContext context)
    {
        String packageName = dataQuality._package == null || dataQuality._package.isEmpty() ? dataQuality.name : dataQuality._package + "::" + dataQuality.name;
        DEPRECATED_PureGrammarComposerCore grammarTransformer = DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).withIndentation(1).withRenderStyle(RenderStyle.PRETTY).build();
        return "DataQualityValidation " + renderAnnotations(dataQuality.stereotypes, dataQuality.taggedValues) + packageName + "\n" +
                "{\n" +
                "   context: " + getContextFunc(dataQuality) + ";\n" +
                "   validationTree: " + processGraphFetchTree(dataQuality.dataQualityRootGraphFetchTree, grammarTransformer) + ";\n" +
                    renderLambda(dataQuality, context) +
                "}";
    }

    private static String renderDataQualityRelationValidation(DataqualityRelationValidation dataqualityRelationValidation, PureGrammarComposerContext context)
    {
        String packageName = dataqualityRelationValidation._package == null || dataqualityRelationValidation._package.isEmpty() ? dataqualityRelationValidation.name : dataqualityRelationValidation._package + "::" + dataqualityRelationValidation.name;
        return "DataQualityRelationValidation " + renderAnnotations(dataqualityRelationValidation.stereotypes, dataqualityRelationValidation.taggedValues) + packageName + "\n" +
                "{\n" +
                "   query: " + renderRelationQuery(dataqualityRelationValidation, context) +
                "   validations: " + renderValidations(dataqualityRelationValidation.validations, context) +
                "}";
    }

    private static String renderLambda(DataQuality dataQuality, PureGrammarComposerContext context)
    {
        if (Objects.isNull(dataQuality.filter))
        {
            return "";
        }
        return "   filter: " + dataQuality.filter.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) + ";\n";
    }

    private static String getContextFunc(DataQuality dataQuality)
    {
        if (dataQuality.context instanceof MappingAndRuntimeDataQualityExecutionContext)
        {
            MappingAndRuntimeDataQualityExecutionContext context = (MappingAndRuntimeDataQualityExecutionContext) dataQuality.context;
            return "fromMappingAndRuntime(" + context.mapping.path + ", " + context.runtime.path + ")";
        }
        else if (dataQuality.context instanceof DataSpaceDataQualityExecutionContext)
        {
            DataSpaceDataQualityExecutionContext context = (DataSpaceDataQualityExecutionContext) dataQuality.context;
            return "fromDataSpace(" + context.dataSpace.path + ", '" + context.context + "')";
        }
        throw new EngineException("Unsupported DataQuality ExecutionContext");
    }

    private static String processGraphFetchTree(DataQualityRootGraphFetchTree rootGraphFetchTree, DEPRECATED_PureGrammarComposerCore grammarTransformer)
    {
        String subTreeString = "";
        String subTypeTreeString = "";
        if (rootGraphFetchTree.subTrees != null && !rootGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = rootGraphFetchTree.subTrees.stream().map(x -> processGraphFetchTree((DataQualityPropertyGraphFetchTree) x, initialTabSize + 1, grammarTransformer)).collect(Collectors.joining("," + (isRenderingPretty() ? returnChar() : "")));
        }
        if (rootGraphFetchTree.subTypeTrees != null && !rootGraphFetchTree.subTypeTrees.isEmpty())
        {
            subTypeTreeString = subTypeTreeString + rootGraphFetchTree.subTypeTrees.stream().map(x -> processGraphFetchTree(x, initialTabSize, grammarTransformer)).collect(Collectors.joining("," + (isRenderingPretty() ? returnChar() : "")));
        }
        return "$[" + (isRenderingPretty() ? returnChar() : "") +
                DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(initialTabSize)) + printFullPath(rootGraphFetchTree._class) + printConstraints(rootGraphFetchTree.constraints) + "{" + (isRenderingPretty() ? returnChar() : "") +
                subTreeString + (!subTreeString.isEmpty() && !subTypeTreeString.isEmpty() ? (isRenderingPretty() ? "," + returnChar() : ",") : "") + subTypeTreeString + (isRenderingPretty() ? returnChar() : "") +
                DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(initialTabSize)) + "}" + (isRenderingPretty() ? returnChar() : "") +
                (isRenderingPretty() ? DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(initialTabSize - 1)) : DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(1))) + "]$";
    }

    private static String printConstraints(List<String> constraints)
    {
        if (Objects.isNull(constraints) || constraints.isEmpty())
        {
            return "";
        }
        return "<" + constraints.stream().map(DataQualityGrammarComposerExtension::renderConstraintName).collect(Collectors.joining(", ")) + ">";
    }

    private static String renderConstraintName(String constraintName)
    {
        return PureGrammarComposerUtility.convertIdentifier(constraintName);
    }

    public static String printFullPath(String fullPath)
    {
        if (isRenderingHTML())
        {
            int index = fullPath.lastIndexOf("::");
            if (index == -1)
            {
                return "<span class='pureGrammar-packageableElement'>" + fullPath + "</span>";
            }
            return "<span class='pureGrammar-package'>" + fullPath.substring(0, index + 2) + "</span><span class='pureGrammar-packageableElement'>" + fullPath.substring(index + 2) + "</span>";
        }
        return fullPath;
    }

    public static String processGraphFetchTree(DataQualityPropertyGraphFetchTree propertyGraphFetchTree, int tabSize, DEPRECATED_PureGrammarComposerCore grammarTransformer)
    {
        String aliasString = "";
        if (propertyGraphFetchTree.alias != null)
        {
            aliasString = convertString(propertyGraphFetchTree.alias, false) + ":"; // we do not need to escape here because the alias need to be a valid string
        }

        String subTreeString = "";
        if (propertyGraphFetchTree.subTrees != null && !propertyGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = "{" + (isRenderingPretty() ? returnChar() : "") +
                    propertyGraphFetchTree.subTrees.stream().map(x -> processGraphFetchTree((DataQualityPropertyGraphFetchTree) x, tabSize + 1, grammarTransformer)).collect(Collectors.joining("," + (isRenderingPretty() ? returnChar() : ""))) + (isRenderingPretty() ? returnChar() : "") +
                    DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(tabSize)) + "}";
        }

        String parametersString = "";
        if (propertyGraphFetchTree.parameters != null && !propertyGraphFetchTree.parameters.isEmpty())
        {
            parametersString = propertyGraphFetchTree.parameters.stream().map(x -> x.accept(grammarTransformer)).collect(Collectors.joining(", ", "(", ")"));
        }

        String subTypeString = "";
        if (propertyGraphFetchTree.subType != null)
        {
            subTypeString = "->subType(@" + printFullPath(propertyGraphFetchTree.subType) + ")";
        }

        return DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(tabSize)) + aliasString + propertyGraphFetchTree.property + printConstraints(propertyGraphFetchTree.constraints) + parametersString + subTypeString + subTreeString;
    }

    public static String processGraphFetchTree(SubTypeGraphFetchTree subTypeGraphFetchTree, int tabSize, DEPRECATED_PureGrammarComposerCore grammarTransformer)
    {
        String subTreeString = "";
        if (subTypeGraphFetchTree.subTrees != null && !subTypeGraphFetchTree.subTrees.isEmpty())
        {
            subTreeString = "{" + (isRenderingPretty() ? returnChar() : "") +
                    subTypeGraphFetchTree.subTrees.stream().map(x -> DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withIndentation(getTabSize(1)).build().processGraphFetchTree(x, getTabSize(1))).collect(Collectors.joining("," + (isRenderingPretty() ? returnChar() : ""))) + (isRenderingPretty() ? returnChar() : "") +
                    DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(tabSize)) + "}";
        }
        String subTypeString = "->subType(@" + HelperValueSpecificationGrammarComposer.printFullPath(subTypeGraphFetchTree.subTypeClass, grammarTransformer) + ")";

        return DEPRECATED_PureGrammarComposerCore.computeIndentationString(grammarTransformer, getTabSize(1)) + subTypeString + subTreeString;
    }

    public static boolean isRenderingPretty()
    {
        return RenderStyle.PRETTY.equals(renderStyle) || RenderStyle.PRETTY_HTML.equals(renderStyle);
    }

    public static boolean isRenderingHTML()
    {
        return RenderStyle.PRETTY_HTML.equals(renderStyle);
    }

    public static String returnChar()
    {
        return isRenderingHTML() ? "</BR>\n" : "\n";
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, sectionName) ->
        {
            if (!DataQualityGrammarParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof DataQuality)
                {
                    return renderDataQuality((DataQuality) element, context);
                }
                if (element instanceof DataqualityRelationValidation)
                {
                    return renderDataQualityRelationValidation((DataqualityRelationValidation) element, context);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Collections.singletonList((elements, context, composedSections) -> // TODO: use context for render style etc - dont hardcode
        {
            MutableList<PackageableElement> composableElements = Iterate.select(elements, e -> (e instanceof DataQuality || e instanceof  DataqualityRelationValidation), Lists.mutable.empty());
            return composableElements.isEmpty()
                    ? null
                    : new PureFreeSectionGrammarComposerResult(composableElements.asLazy().collect(element -> renderElement(element, context)).makeString("###" + DataQualityGrammarParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderRelationQuery(DataqualityRelationValidation dataqualityRelationValidation, PureGrammarComposerContext context)
    {
        return dataqualityRelationValidation.query.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build()) + ";\n";
    }

    private static String renderValidations(List<RelationValidation> relationValidations, PureGrammarComposerContext context)
    {
        return "[\n" +
                   relationValidations.stream().map(val -> renderValidation(val, context)).collect(Collectors.joining(",\n", "", "\n")) +
                "   ];\n";
    }

    private static String renderValidation(RelationValidation relationValidation, PureGrammarComposerContext context)
    {
        return
                "   {\n" +
                "     name: '" + relationValidation.name + "';\n" +
                        (Objects.nonNull(relationValidation.description) ?
                "     description: '" + relationValidation.description + "';\n" : "") +
                "     assertion: " + renderAssertion(relationValidation, context) + ";\n" +
                        (Objects.nonNull(relationValidation.type) ?
                "     type: " + relationValidation.type + ";\n" : "") +
                "    }";
    }

    private static String renderAssertion(RelationValidation relationValidation, PureGrammarComposerContext context)
    {
        return relationValidation.assertion.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).build());
    }
}
