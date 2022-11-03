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

package org.finos.legend.engine.language.pure.grammar.from;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DiagramParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.ClassView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.GeneralizationView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.PropertyView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Line;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Point;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.geometry.Rectangle;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.PropertyPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

public class DiagramParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;

    public DiagramParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(DiagramParserGrammar.DefinitionContext ctx)
    {
        ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()), this.section.imports);
        ctx.diagram().stream().map(this::visitDiagram).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private Diagram visitDiagram(DiagramParserGrammar.DiagramContext ctx)
    {
        Diagram diagram = new Diagram();
        diagram.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        diagram._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        diagram.classViews = ctx.classView() == null ? new ArrayList<>() : ListIterate.collect(ctx.classView(), this::visitClassView);
        diagram.propertyViews = ctx.classView() == null ? new ArrayList<>() : ListIterate.collect(ctx.propertyView(), this::visitPropertyView);
        diagram.generalizationViews = ctx.classView() == null ? new ArrayList<>() : ListIterate.collect(ctx.generalizationView(), this::visitGeneralizationView);
        diagram.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return diagram;
    }

    private ClassView visitClassView(DiagramParserGrammar.ClassViewContext ctx)
    {
        ClassView classView = new ClassView();
        classView.id = ctx.viewId().getText();
        classView.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // class
        DiagramParserGrammar.ClassViewClassPropContext classPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.classViewClassProp(), "class", classView.sourceInformation);
        classView._class = PureGrammarParserUtility.fromQualifiedName(classPropContext.qualifiedName().packagePath() == null ? Collections.emptyList() : classPropContext.qualifiedName().packagePath().identifier(), classPropContext.qualifiedName().identifier());
        classView.classSourceInformation = walkerSourceInformation.getSourceInformation(classPropContext.qualifiedName());
        // position
        DiagramParserGrammar.ClassViewPositionPropContext positionPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.classViewPositionProp(), "position", classView.sourceInformation);
        classView.position = visitPoint(positionPropContext.numberPair());
        // rectangle
        DiagramParserGrammar.ClassViewRectanglePropContext rectanglePropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.classViewRectangleProp(), "rectangle", classView.sourceInformation);
        classView.rectangle = visitRectangle(rectanglePropContext.numberPair());
        // hide properties flag (optional)
        DiagramParserGrammar.ClassViewHidePropertiesPropContext hidePropertiesPropContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.classViewHidePropertiesProp(), "hideProperties", classView.sourceInformation);
        if (hidePropertiesPropContext != null && Boolean.parseBoolean(hidePropertiesPropContext.BOOLEAN().getText()))
        {
            classView.hideProperties = true;
        }
        // hide tagged values flag (optional)
        DiagramParserGrammar.ClassViewHideTaggedValuePropContext hideTaggedValuePropContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.classViewHideTaggedValueProp(), "hideTaggedValue", classView.sourceInformation);
        if (hideTaggedValuePropContext != null && Boolean.parseBoolean(hideTaggedValuePropContext.BOOLEAN().getText()))
        {
            classView.hideTaggedValues = true;
        }
        // hide stereotypes flag (optional)
        DiagramParserGrammar.ClassViewHideStereotypePropContext hideStereotypePropContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.classViewHideStereotypeProp(), "hideStereotype", classView.sourceInformation);
        if (hideStereotypePropContext != null && Boolean.parseBoolean(hideStereotypePropContext.BOOLEAN().getText()))
        {
            classView.hideStereotypes = true;
        }
        return classView;
    }

    private PropertyView visitPropertyView(DiagramParserGrammar.PropertyViewContext ctx)
    {
        PropertyView propertyView = new PropertyView();
        propertyView.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // Property
        PropertyPointer propertyPointer = new PropertyPointer();
        DiagramParserGrammar.PropertyHolderViewPropertyPropContext propertyPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.propertyHolderViewPropertyProp(), "property", propertyView.sourceInformation);
        propertyPointer.propertyOwner = PureGrammarParserUtility.fromQualifiedName(propertyPropContext.qualifiedName().packagePath() == null ? Collections.emptyList() : propertyPropContext.qualifiedName().packagePath().identifier(), propertyPropContext.qualifiedName().identifier());
        propertyPointer.property = PureGrammarParserUtility.fromIdentifier(propertyPropContext.identifier());
        propertyPointer.sourceInformation = walkerSourceInformation.getSourceInformation(propertyPropContext.qualifiedName());
        propertyView.property = propertyPointer;
        // Line
        Line line = new Line();
        DiagramParserGrammar.RelationshipViewPointsPropContext pointsPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewPointsProp(), "points", propertyView.sourceInformation);
        line.points = ListIterate.collect(pointsPropContext.numberPair(), this::visitPoint);
        propertyView.line = line;
        // Source
        DiagramParserGrammar.RelationshipViewSourcePropContext sourcePropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewSourceProp(), "source", propertyView.sourceInformation);
        propertyView.sourceView = sourcePropContext.viewId().getText();
        propertyView.sourceViewSourceInformation = walkerSourceInformation.getSourceInformation(sourcePropContext.viewId());
        // Target
        DiagramParserGrammar.RelationshipViewTargetPropContext targetPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewTargetProp(), "target", propertyView.sourceInformation);
        propertyView.targetView = targetPropContext.viewId().getText();
        propertyView.targetViewSourceInformation = walkerSourceInformation.getSourceInformation(targetPropContext.viewId());
        return propertyView;
    }

    private GeneralizationView visitGeneralizationView(DiagramParserGrammar.GeneralizationViewContext ctx)
    {
        GeneralizationView generalizationView = new GeneralizationView();
        generalizationView.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        // Line
        Line line = new Line();
        DiagramParserGrammar.RelationshipViewPointsPropContext pointsPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewPointsProp(), "points", generalizationView.sourceInformation);
        line.points = ListIterate.collect(pointsPropContext.numberPair(), this::visitPoint);
        generalizationView.line = line;
        // Source
        DiagramParserGrammar.RelationshipViewSourcePropContext sourcePropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewSourceProp(), "source", generalizationView.sourceInformation);
        generalizationView.sourceView = sourcePropContext.viewId().getText();
        generalizationView.sourceViewSourceInformation = walkerSourceInformation.getSourceInformation(sourcePropContext.viewId());
        // Target
        DiagramParserGrammar.RelationshipViewTargetPropContext targetPropContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.relationshipViewTargetProp(), "target", generalizationView.sourceInformation);
        generalizationView.targetView = targetPropContext.viewId().getText();
        generalizationView.targetViewSourceInformation = walkerSourceInformation.getSourceInformation(targetPropContext.viewId());
        return generalizationView;
    }

    private Point visitPoint(DiagramParserGrammar.NumberPairContext ctx)
    {
        return new Point(Double.parseDouble(ctx.number().get(0).getText()), Double.parseDouble(ctx.number().get(1).getText()));
    }

    private Rectangle visitRectangle(DiagramParserGrammar.NumberPairContext ctx)
    {
        return new Rectangle(Double.parseDouble(ctx.number().get(0).getText()), Double.parseDouble(ctx.number().get(1).getText()));
    }
}
