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

package org.finos.legend.engine.test.emit.service;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * JUnit 5 runner for the <b>service shapes</b> EMIT suite: services whose execution
 * shape is the subject — multi-execution routing by env key, test data shared between
 * services through a {@code Data} element — plus the deprecated {@code test: Single}
 * block that EMIT Phase 5's legacy service test runner drives, which has no example
 * anywhere else in the catalog.
 *
 * <p>Only service-bearing models belong here. The legacy <i>mapping</i> test runner is
 * the sibling Phase 5 path but involves no Service, so its example lives with the
 * mappings it exercises — {@code relational-legacy-mapping-test} in the relational
 * suite.
 *
 * <p>All models are backed by an in-memory H2 relational mapping so the tests execute
 * rather than only compile. The module hosts a single subject area, so it keeps the
 * conventional {@code emit-models/} root and needs no {@code includedRelativeSubpaths}
 * override in the server pom (see {@code docs/emit/emit.md} §5.4).
 */
public class ServiceEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("emit-models/");
    }
}
