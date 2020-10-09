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
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.DiagramParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.ClassView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.GeneralizationView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.PropertyView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Line;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Point;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Rectangle;

import java.util.List;
import java.util.Set;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.appendTabString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class DiagramGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!DiagramParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof Diagram)
                {
                    return renderDiagram((Diagram) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<Set<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<Diagram> composableElements = ListIterate.selectInstancesOf(FastList.newList(elements), Diagram.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, DiagramGrammarComposerExtension::renderDiagram).makeString("###" + DiagramParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderDiagram(Diagram diagram)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("Diagram").append(" ").append(PureGrammarComposerUtility.convertPath(diagram.getPath())).append("\n{\n");
        if (!diagram.classViews.isEmpty())
        {
            builder.append(LazyIterate.collect(diagram.classViews, DiagramGrammarComposerExtension::renderClassView).makeString(""));
        }
        if (!diagram.propertyViews.isEmpty())
        {
            builder.append(LazyIterate.collect(diagram.propertyViews, DiagramGrammarComposerExtension::renderPropertyView).makeString(""));
        }
        if (!diagram.generalizationViews.isEmpty())
        {
            builder.append(LazyIterate.collect(diagram.generalizationViews, DiagramGrammarComposerExtension::renderGeneralizationView).makeString(""));
        }
        return builder.append("}").toString();
    }

    private static String renderClassView(ClassView classView)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getTabString()).append("classView").append(" ").append(classView.id).append("\n");
        builder.append(getTabString()).append("{\n");
        appendTabString(builder, 2).append("class: ").append(PureGrammarComposerUtility.convertPath(classView._class)).append(";\n");
        appendTabString(builder, 2).append("position: ").append(renderPoint(classView.position)).append(";\n");
        appendTabString(builder, 2).append("rectangle: ").append(renderRectangle(classView.rectangle)).append(";\n");
        if (classView.hideProperties)
        {
            builder.append(getTabString(2)).append("hideProperties: true").append(";\n");
        }
        if (classView.hideTaggedValues)
        {
            builder.append(getTabString(2)).append("hideTaggedValue: true").append(";\n");
        }
        if (classView.hideStereotypes)
        {
            builder.append(getTabString(2)).append("hideStereotype: true").append(";\n");
        }
        builder.append(getTabString()).append("}\n");
        return builder.toString();
    }

    public static String renderPropertyView(PropertyView propertyView)
    {
        return getTabString() + "propertyView" + "\n" +
                getTabString() + "{\n" +
                getTabString(2) + "property: " + PureGrammarComposerUtility.convertPath(propertyView.property._class) + "." + PureGrammarComposerUtility.convertIdentifier(propertyView.property.property) + ";\n" +
                getTabString(2) + "source: " + propertyView.sourceView + ";\n" +
                getTabString(2) + "target: " + propertyView.targetView + ";\n" +
                getTabString(2) + "points: " + renderLine(propertyView.line) + ";\n" +
                getTabString() + "}\n";
    }

    public static String renderGeneralizationView(GeneralizationView generalizationView)
    {
        return getTabString() + "generalizationView" + "\n" +
                getTabString() + "{\n" +
                getTabString(2) + "source: " + generalizationView.sourceView + ";\n" +
                getTabString(2) + "target: " + generalizationView.targetView + ";\n" +
                getTabString(2) + "points: " + renderLine(generalizationView.line) + ";\n" +
                getTabString() + "}\n";
    }

    private static String renderLine(Line line)
    {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (!line.points.isEmpty())
        {
            builder.append(LazyIterate.collect(line.points, DiagramGrammarComposerExtension::renderPoint).makeString(","));
        }
        builder.append("]");
        return builder.toString();
    }

    private static String renderPoint(Point point)
    {
        return "(" + point.x + "," + point.y + ")";
    }

    private static String renderRectangle(Rectangle rectangle)
    {
        return "(" + rectangle.width + "," + rectangle.height + ")";
    }
}
