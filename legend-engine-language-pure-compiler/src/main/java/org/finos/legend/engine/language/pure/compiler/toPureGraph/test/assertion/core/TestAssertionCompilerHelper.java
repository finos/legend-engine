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

package org.finos.legend.engine.language.pure.compiler.toPureGraph.test.assertion.core;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ProcessingContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.data.EmbeddedDataFirstPassBuilder;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.AssertAllRows;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_AssertAllRows_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_EqualToJson_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_EqualTo_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_test_assertion_TestAssertion;

public class TestAssertionCompilerHelper
{
    public static Root_meta_pure_test_assertion_TestAssertion compileCoreTestAssertionTypes(TestAssertion testAssertion, CompileContext context, ProcessingContext processingContext)
    {
        if (testAssertion instanceof EqualTo)
        {
            EqualTo equalTo = (EqualTo) testAssertion;
            return new Root_meta_pure_test_assertion_EqualTo_Impl("")
                    ._expected(equalTo.expected.accept(new ValueSpecificationBuilder(context, Lists.mutable.empty(), new ProcessingContext(""))));
        }
        else if (testAssertion instanceof EqualToJson)
        {
            EqualToJson equalToJson = (EqualToJson) testAssertion;

            return new Root_meta_pure_test_assertion_EqualToJson_Impl("")
                    ._expected(equalToJson.expected.accept(new EmbeddedDataFirstPassBuilder(context, processingContext)));
        }
        else if (testAssertion instanceof AssertAllRows)
        {
            AssertAllRows assertAllRows = (AssertAllRows) testAssertion;

            return new Root_meta_pure_test_assertion_AssertAllRows_Impl("")
                ._expected(assertAllRows.expected.accept(new EmbeddedDataFirstPassBuilder(context, processingContext)));
        }
        else
        {
            return null;
        }
    }
}
