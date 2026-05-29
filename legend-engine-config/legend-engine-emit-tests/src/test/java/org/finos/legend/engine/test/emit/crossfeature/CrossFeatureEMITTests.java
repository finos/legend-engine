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

package org.finos.legend.engine.test.emit.crossfeature;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * JUnit 5 entry point for cross-feature EMIT fixtures. Discovers every
 * {@code *.emit.yaml} under {@code emit-models/} on this module's test
 * classpath and yields one dynamic container per model, with one leaf
 * test per pipeline task (parse, compile, mapping/service tests, plan
 * generation, …).
 *
 * <p>Fixtures placed here are ones whose feature combination — e.g. a
 * relational-backed service that also carries an external-format binding,
 * a generation specification producing a relational mapping, a
 * multi-execution service driving a function activator — needs the full
 * generation classpath supplied by
 * {@code legend-engine-extensions-collection-generation}, which no
 * per-feature {@code -emit} module pulls in.</p>
 */
public class CrossFeatureEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("emit-models/");
    }
}
