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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.ClassView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.GeneralizationView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.PropertyView;
import org.finos.legend.engine.shared.core.operational.Assert;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_ClassView;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_ClassView_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Diagram;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_GeneralizationView;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_GeneralizationView_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Point_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_PropertyView;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_PropertyView_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Rectangle_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_RelationshipViewEnd_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;

import java.util.Objects;

public class HelperDiagramBuilder
{

    private static DiagramCompilerExtension getDiagramCompilerExtensionInstance(CompileContext context)
    {
        return Objects.requireNonNull(ListIterate.selectInstancesOf(context.getCompilerExtensions().getExtensions(), DiagramCompilerExtension.class).getAny(), "Diagram extension is not in scope");
    }

    public static Root_meta_pure_metamodel_diagram_Diagram getDiagram(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        Root_meta_pure_metamodel_diagram_Diagram diagram = getDiagramCompilerExtensionInstance(context).diagramsIndex.get(fullPath);
        Assert.assertTrue(diagram != null, () -> "Can't find diagram '" + fullPath + "'", sourceInformation, EngineErrorType.COMPILATION);
        return diagram;
    }

    public static Root_meta_pure_metamodel_diagram_Diagram resolveDiagram(String fullPath, SourceInformation sourceInformation, CompileContext context)
    {
        return context.resolve(fullPath, sourceInformation, path -> getDiagram(path, sourceInformation, context));
    }

    public static Root_meta_pure_metamodel_diagram_ClassView buildClassView(ClassView classView, CompileContext context)
    {
        return new Root_meta_pure_metamodel_diagram_ClassView_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::ClassView"))
                ._id(classView.id)
                ._class(context.resolveClass(classView._class, classView.classSourceInformation))
                ._hideProperties(classView.hideProperties)
                ._hideStereotypes(classView.hideStereotypes)
                ._hideTaggedValues(classView.hideTaggedValues)
                ._rectangle(new Root_meta_pure_metamodel_diagram_Rectangle_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::Rectangle"))._width(classView.rectangle.width)._height(classView.rectangle.height))
                ._position(new Root_meta_pure_metamodel_diagram_Point_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::Point"))._x(classView.position.x)._y(classView.position.y));
    }

    public static Root_meta_pure_metamodel_diagram_PropertyView buildPropertyView(PropertyView propertyView, CompileContext context, Root_meta_pure_metamodel_diagram_Diagram diagram)
    {
        Root_meta_pure_metamodel_diagram_ClassView from = diagram._classViews().select(view -> view._id().equals(propertyView.sourceView)).getAny();
        Assert.assertTrue(from != null, () -> "Can't find source class view '" + propertyView.sourceView + "'", propertyView.sourceViewSourceInformation, EngineErrorType.COMPILATION);
        Root_meta_pure_metamodel_diagram_ClassView to = diagram._classViews().select(view -> view._id().equals(propertyView.targetView)).getAny();
        Assert.assertTrue(to != null, () -> "Can't find target class view '" + propertyView.targetView + "'", propertyView.targetViewSourceInformation, EngineErrorType.COMPILATION);
        PropertyOwner propertyOwner = context.resolvePropertyOwner(propertyView.property._class, propertyView.property.sourceInformation);
        return new Root_meta_pure_metamodel_diagram_PropertyView_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::PropertyView"))
                // Property Views can hold either class properties or qualified property views
                ._property(HelperModelBuilder.getAllOwnedAppliedProperty(propertyOwner, propertyView.property.property, propertyView.property.sourceInformation, context.pureModel.getExecutionSupport()))
                ._from(new Root_meta_pure_metamodel_diagram_RelationshipViewEnd_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::RelationshipViewEnd"))._classView(from))
                ._to(new Root_meta_pure_metamodel_diagram_RelationshipViewEnd_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::RelationshipViewEnd"))._classView(to))._path(ListIterate.collect(propertyView.line.points, point -> new Root_meta_pure_metamodel_diagram_Point_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::Point"))._x(point.x)._y(point.y)));
    }

    public static Root_meta_pure_metamodel_diagram_GeneralizationView buildGeneralizationView(GeneralizationView generalizationView, Root_meta_pure_metamodel_diagram_Diagram diagram, CompileContext context)
    {
        Root_meta_pure_metamodel_diagram_ClassView from = diagram._classViews().select(view -> view._id().equals(generalizationView.sourceView)).getAny();
        Assert.assertTrue(from != null, () -> "Can't find source class view '" + generalizationView.sourceView + "'", generalizationView.sourceViewSourceInformation, EngineErrorType.COMPILATION);
        Root_meta_pure_metamodel_diagram_ClassView to = diagram._classViews().select(view -> view._id().equals(generalizationView.targetView)).getAny();
        Assert.assertTrue(to != null, () -> "Can't find target class view '" + generalizationView.targetView + "'", generalizationView.targetViewSourceInformation, EngineErrorType.COMPILATION);
        return new Root_meta_pure_metamodel_diagram_GeneralizationView_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::GeneralizationView"))
                ._from(new Root_meta_pure_metamodel_diagram_RelationshipViewEnd_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::RelationshipViewEnd"))._classView(from))
                ._to(new Root_meta_pure_metamodel_diagram_RelationshipViewEnd_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::RelationshipViewEnd"))._classView(to))._path(ListIterate.collect(generalizationView.line.points, point -> new Root_meta_pure_metamodel_diagram_Point_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::Point"))._x(point.x)._y(point.y)));
    }
}

