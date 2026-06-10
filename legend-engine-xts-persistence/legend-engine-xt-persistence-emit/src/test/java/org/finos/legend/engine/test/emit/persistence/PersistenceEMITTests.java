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

package org.finos.legend.engine.test.emit.persistence;

import org.finos.legend.engine.test.emit.junit.EMITTestSuiteBuilder;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

/**
 * EMIT coverage for persistence models. Each {@code *.emit.yaml} under
 * {@code emit-models/} becomes one {@link DynamicContainer} whose leaves run
 * the full EMIT pipeline (parse → compile → … → test execution).
 *
 * <p>Note that these dynamic tests pass <em>vacuously</em> if no persistence
 * test is ever discovered: a model that yields no Test tasks simply contributes
 * no failing leaf. The guard against that silent regression lives in
 * {@link TestPersistenceEMITTestSuiteBuilder} (for this JUnit integration) and
 * {@link TestPersistenceEMITRunner} (for the standalone runner).
 */
public class PersistenceEMITTests
{
    @TestFactory
    Stream<DynamicContainer> emit()
    {
        return EMITTestSuiteBuilder.testContainers("emit-models/");
    }
}
