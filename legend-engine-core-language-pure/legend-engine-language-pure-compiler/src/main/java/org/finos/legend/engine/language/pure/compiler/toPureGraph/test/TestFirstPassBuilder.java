// Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.test.Test;
import org.finos.legend.engine.protocol.pure.v1.model.test.TestVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Objects;

public class TestFirstPassBuilder implements TestVisitor<org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test>
{
    private final CompileContext context;
    private final ProcessingContext processingContext;

    public TestFirstPassBuilder(CompileContext context, ProcessingContext processingContext)
    {
        this.context = context;
        this.processingContext = processingContext;
    }

    @Override
    public org.finos.legend.pure.m3.coreinstance.meta.pure.test.Test visit(Test test)
    {
        return context.getCompilerExtensions().getExtraTestProcessors().stream()
                .map(processor -> processor.value(test, context, processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new EngineException("Unsupported test element type '" + test.getClass().getSimpleName() + "'", test.sourceInformation, EngineErrorType.COMPILATION));
    }
}
