// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.testcases.ingestmode;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.scenarios.NoOpScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.junit.jupiter.api.Test;

public abstract class NoOpTestCases extends BaseTest
{
    NoOpScenarios scenarios = new NoOpScenarios();

    @Test
    void testNoOp()
    {
        TestScenario testScenario = scenarios.NO_OP();
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(testScenario.getIngestMode())
            .relationalSink(getRelationalSink())
            .collectStatistics(true)
            .skipMainAndMetadataDatasetCreation(false)
            .enableConcurrentSafety(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRequestId("123456789")
            .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNoOp(operations);
    }

    public abstract void verifyNoOp(GeneratorResult operations);

    @Test
    void testNoOpUpperCase()
    {
        TestScenario testScenario = scenarios.NO_OP();
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(testScenario.getIngestMode())
                .relationalSink(getRelationalSink())
                .collectStatistics(true)
                .skipMainAndMetadataDatasetCreation(false)
                .enableConcurrentSafety(true)
                .executionTimestampClock(fixedClock_2000_01_01)
                .ingestRequestId("123456789")
                .caseConversion(CaseConversion.TO_UPPER)
                .build();

        GeneratorResult operations = generator.generateOperations(testScenario.getDatasets());
        verifyNoOpUpperCase(operations);
    }

    public abstract void verifyNoOpUpperCase(GeneratorResult operations);

    public abstract RelationalSink getRelationalSink();
}