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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Diagram;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Diagram_Impl;

import java.util.Collections;

public class DiagramCompilerExtension implements CompilerExtension
{
    protected final MutableMap<String, Root_meta_pure_metamodel_diagram_Diagram> diagramsIndex = Maps.mutable.empty();

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(Diagram.class,
                (diagram, context) ->
                {
                    Root_meta_pure_metamodel_diagram_Diagram metamodel = new Root_meta_pure_metamodel_diagram_Diagram_Impl("", null, context.pureModel.getClass("meta::pure::metamodel::diagram::Diagram"))._name(diagram.name);
                    this.diagramsIndex.put(context.pureModel.buildPackageString(diagram._package, diagram.name), metamodel);
                    return metamodel;
                },
                (diagram, context) ->
                {
                    Root_meta_pure_metamodel_diagram_Diagram metamodel = this.diagramsIndex.get(context.pureModel.buildPackageString(diagram._package, diagram.name));
                    metamodel._classViews(ListIterate.collect(diagram.classViews, view -> HelperDiagramBuilder.buildClassView(view, context)));
                    metamodel._propertyViews(ListIterate.collect(diagram.propertyViews, view -> HelperDiagramBuilder.buildPropertyView(view, context, metamodel)));
                    metamodel._generalizationViews(ListIterate.collect(diagram.generalizationViews, view -> HelperDiagramBuilder.buildGeneralizationView(view, metamodel, context)));
                }));
    }
}
