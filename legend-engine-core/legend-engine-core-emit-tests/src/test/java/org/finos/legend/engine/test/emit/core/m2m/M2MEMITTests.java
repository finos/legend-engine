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

package org.finos.legend.engine.test.emit.core.m2m;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * JUnit 5 runner for the <b>model-to-model mapping</b> EMIT suite: Pure class mappings
 * whose embedded test suites execute against the in-memory store over ModelStore test
 * data, so each mapping feature is proven through plan generation and execution rather
 * than only through compilation.
 *
 * <p>The sibling Pure language suite lives under {@code grammar-emit-models/} and is
 * driven by {@link org.finos.legend.engine.test.emit.core.grammar.GrammarEMITTests}.
 * The two use separate resource roots so each subject area can be run on its own.
 */
public class M2MEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("m2m-emit-models/");
    }
}
