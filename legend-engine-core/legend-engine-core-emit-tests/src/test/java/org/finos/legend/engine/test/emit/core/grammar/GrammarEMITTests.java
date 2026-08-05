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

package org.finos.legend.engine.test.emit.core.grammar;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * JUnit 5 runner for the <b>Pure language</b> EMIT suite: models exercising domain
 * grammar constructs — constraints, measures, profiles, qualified properties,
 * inheritance, functions and multi-level association traversal. These models carry no
 * store and no mapping, so they run parse and compile only; a failure is attributable
 * to the language construct rather than to a mapping strategy that happens to carry it.
 *
 * <p>The sibling model-to-model suite lives under {@code m2m-emit-models/} and is
 * driven by {@link org.finos.legend.engine.test.emit.core.m2m.M2MEMITTests}. The two
 * use separate resource roots so each subject area can be run on its own.
 */
public class GrammarEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("grammar-emit-models/");
    }
}
