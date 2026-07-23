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

package org.finos.legend.engine.test.emit.relation;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * JUnit 5 runner for the relation ({@code ~func}) mapping EMIT suite. Discovers
 * EMIT models under {@code relation-emit-models/} on the test classpath and yields
 * one dynamic test per granular operation (init/parse/compile, file generation,
 * individual mapping tests, service plans).
 *
 * <p>Kept as a peer to {@link org.finos.legend.engine.test.emit.relational.RelationalEMITTests}
 * within the same Maven module: same dependency profile, but the two suites use
 * distinct classpath resource roots ({@code relational-emit-models/} vs
 * {@code relation-emit-models/}) so a failure is unambiguously attributable to the
 * classic-relational or the relation-mapping code path.
 */
public class RelationEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("relation-emit-models/");
    }
}

