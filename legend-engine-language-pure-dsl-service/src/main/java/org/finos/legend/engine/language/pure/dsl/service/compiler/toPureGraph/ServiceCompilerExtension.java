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

package org.finos.legend.engine.language.pure.dsl.service.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.Execution;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ServiceTest;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Execution;
import org.finos.legend.pure.generated.Root_meta_legend_service_metamodel_Test;

import java.util.Collections;
import java.util.List;

public interface ServiceCompilerExtension extends CompilerExtension
{
    default List<Function2<Execution, CompileContext, Root_meta_legend_service_metamodel_Execution>> getExtraServiceExecutionProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function3<ServiceTest, Execution, CompileContext, Root_meta_legend_service_metamodel_Test>> getExtraServiceTestProcessors()
    {
        return Collections.emptyList();
    }
}
