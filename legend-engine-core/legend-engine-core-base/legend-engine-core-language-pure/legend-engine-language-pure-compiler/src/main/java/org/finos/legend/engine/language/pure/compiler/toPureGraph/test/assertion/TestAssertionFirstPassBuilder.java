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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion;

import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertionVisitor;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_TestAssertion;

import java.util.Objects;

public class TestAssertionFirstPassBuilder implements TestAssertionVisitor<Root_meta_pure_test_assertion_TestAssertion>
{
    private final CompileContext context;
    private final ProcessingContext processingContext;

    public TestAssertionFirstPassBuilder(CompileContext context, ProcessingContext processingContext)
    {
        this.context = context;
        this.processingContext = processingContext;
    }

    @Override
    public Root_meta_pure_test_assertion_TestAssertion visit(TestAssertion testAssertion)
    {
        return context.getCompilerExtensions().getExtraTestAssertionProcessors().stream()
                .map(processor -> processor.value(testAssertion, context, processingContext))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new EngineException("Unsupported test element type '" + testAssertion.getClass().getSimpleName() + "'", testAssertion.sourceInformation, EngineErrorType.COMPILATION));
    }
}
