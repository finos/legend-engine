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

package org.finos.legend.engine.language.pure.dsl.diagram.compiler.toPureGraph;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperModelBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.ClassView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.GeneralizationView;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.PropertyView;
import org.finos.legend.engine.shared.core.operational.Assert;

public class HelperDiagramBuilder
{
    public static void processClassView(ClassView classView, CompileContext context)
    {
        context.resolveClass(classView._class, classView.classSourceInformation);
    }

    public static void processPropertyView(PropertyView propertyView, CompileContext context, Diagram diagram)
    {
        // Property Views can hold either class properties or qualified property views
        HelperModelBuilder.getOwnedAppliedProperty(context, context.resolveClass(propertyView.property._class, propertyView.property.sourceInformation), propertyView.property.property, propertyView.property.sourceInformation, context.pureModel.getExecutionSupport());
        Assert.assertTrue(diagram.classViews.stream().anyMatch(view -> view.id.equals(propertyView.sourceView)), () -> "Can't find source class view '" + propertyView.sourceView + "'", propertyView.sourceViewSourceInformation, EngineErrorType.COMPILATION);
        Assert.assertTrue(diagram.classViews.stream().anyMatch(view -> view.id.equals(propertyView.targetView)), () -> "Can't find target class view '" + propertyView.targetView + "'", propertyView.targetViewSourceInformation, EngineErrorType.COMPILATION);
    }

    public static void processGeneralizationView(GeneralizationView generalizationView, Diagram diagram)
    {
        Assert.assertTrue(diagram.classViews.stream().anyMatch(view -> view.id.equals(generalizationView.sourceView)), () -> "Can't find source class view '" + generalizationView.sourceView + "'", generalizationView.sourceViewSourceInformation, EngineErrorType.COMPILATION);
        Assert.assertTrue(diagram.classViews.stream().anyMatch(view -> view.id.equals(generalizationView.targetView)), () -> "Can't find target class view '" + generalizationView.targetView + "'", generalizationView.targetViewSourceInformation, EngineErrorType.COMPILATION);
    }
}
