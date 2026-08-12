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

package org.finos.legend.engine.language.dataquality.api;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.generation.dataquality.DataQualityRelationComparisonArtifactGenerationExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextConcrete;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ParameterValue;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDataQualityReconArtifactDefaults
{
    @Test
    public void testMatchesDefaults()
    {
        assertTrue(DataQualityExecute.matchesReconArtifactDefaults(defaultInput()));
    }

    @Test
    public void testAnyDeviationFromDefaultsFails()
    {
        // Each mutation individually disqualifies the request from using the pre-generated recon plan artifact.
        assertMismatch(i -> i.packagePath = null);
        assertMismatch(i -> i.model = null);
        assertMismatch(i -> i.model = new PureModelContextConcrete());
        assertMismatch(i -> i.runSourceQuery = true);
        assertMismatch(i -> i.runTargetQuery = true);
        assertMismatch(i -> i.queryLimit = 100L);
        assertMismatch(i -> i.defectLimit = null);
        assertMismatch(i -> i.defectLimit = DataQualityRelationComparisonArtifactGenerationExtension.DEFAULT_DEFECT_LIMIT + 1);
        assertMismatch(i -> i.includeColumnValues = !DataQualityRelationComparisonArtifactGenerationExtension.INCLUDE_COLUMN_VALUES);
        assertMismatch(i -> i.enrichDQColumns = !DataQualityRelationComparisonArtifactGenerationExtension.ENRICH_DQ_COLUMNS);
        assertMismatch(i -> i.sourceLambdaParameterValues = Collections.singletonList(new ParameterValue()));
        assertMismatch(i -> i.targetLambdaParameterValues = FastList.newListWith(new ParameterValue()));
    }

    private static void assertMismatch(Consumer<DataQualityReconInput> mutator)
    {
        DataQualityReconInput input = defaultInput();
        mutator.accept(input);
        assertFalse("Expected mismatch after mutation", DataQualityExecute.matchesReconArtifactDefaults(input));
    }

    private static DataQualityReconInput defaultInput()
    {
        DataQualityReconInput input = new DataQualityReconInput();
        input.packagePath = "meta::dataquality::TestRelationComparison";
        input.model = new PureModelContextPointer();
        input.defectLimit = DataQualityRelationComparisonArtifactGenerationExtension.DEFAULT_DEFECT_LIMIT;
        input.includeColumnValues = DataQualityRelationComparisonArtifactGenerationExtension.INCLUDE_COLUMN_VALUES;
        input.enrichDQColumns = DataQualityRelationComparisonArtifactGenerationExtension.ENRICH_DQ_COLUMNS;
        return input;
    }
}
