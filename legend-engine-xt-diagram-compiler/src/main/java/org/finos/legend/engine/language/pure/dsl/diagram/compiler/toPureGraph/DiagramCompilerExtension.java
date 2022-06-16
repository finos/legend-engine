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

import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.diagram.Diagram;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_diagram_Diagram_Impl;

import java.util.Collections;

public class DiagramCompilerExtension implements CompilerExtension
{
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(Diagram.class, (diagram, context) -> new Root_meta_pure_metamodel_diagram_Diagram_Impl(""),
                (diagram, context) ->
                {
                    diagram.classViews.forEach(view -> HelperDiagramBuilder.processClassView(view, context));
                    diagram.propertyViews.forEach(view -> HelperDiagramBuilder.processPropertyView(view, context, diagram));
                    diagram.generalizationViews.forEach(view -> HelperDiagramBuilder.processGeneralizationView(view, diagram));
                }));
    }
}
