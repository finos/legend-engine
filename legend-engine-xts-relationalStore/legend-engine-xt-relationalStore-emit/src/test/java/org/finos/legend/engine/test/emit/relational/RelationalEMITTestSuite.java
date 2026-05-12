// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.test.emit.relational;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * Example wiring of {@link EMITTestSuiteBuilder} into JUnit 5 for the relational
 * store. Discovers EMIT models under {@code emit-models/} on the test classpath
 * and yields one dynamic test per granular operation (init/parse/compile,
 * file generation, individual mapping tests, service plans).
 */
public class RelationalEMITTestSuite
{
    @TestFactory
    Stream<DynamicTest> emit()
    {
        return EMITTestSuiteBuilder.taskStream("emit-models/");
    }
}
