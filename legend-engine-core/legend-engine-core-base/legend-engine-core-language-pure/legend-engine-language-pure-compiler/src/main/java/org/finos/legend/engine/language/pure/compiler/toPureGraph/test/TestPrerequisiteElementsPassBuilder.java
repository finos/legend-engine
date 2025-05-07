// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestVisitor;

import java.util.Set;

public class TestPrerequisiteElementsPassBuilder implements TestVisitor<Set<PackageableElementPointer>>
{
    private final CompileContext context;
    private final Set<PackageableElementPointer> prerequisiteElements;

    public TestPrerequisiteElementsPassBuilder(CompileContext context, Set<PackageableElementPointer> prerequisiteElements)
    {
        this.context = context;
        this.prerequisiteElements = prerequisiteElements;
    }

    @Override
    public Set<PackageableElementPointer> visit(Test test)
    {
        this.context.getCompilerExtensions().getExtraTestPrerequisiteElementsPassProcessors().forEach(processor -> processor.value(this.prerequisiteElements, test, this.context));
        return this.prerequisiteElements;
    }
}
