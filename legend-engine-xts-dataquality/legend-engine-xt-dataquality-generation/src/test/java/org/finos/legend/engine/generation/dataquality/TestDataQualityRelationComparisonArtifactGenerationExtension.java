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

package org.finos.legend.engine.generation.dataquality;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.dsl.generation.extension.Artifact;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestDataQualityRelationComparisonArtifactGenerationExtension
{
    private static final String RELATION_COMPARISON = "###DataQualityValidation\n" +
            "DataQualityRelationComparison meta::dataquality::TestRelationComparison\n" +
            "{\n" +
            "  source: |#>{meta::external::dataquality::tests::domain::db.personTable}#->select(~[FIRSTNAME, AGE])->from(meta::external::dataquality::tests::domain::DataQualityRuntime);\n" +
            "  target: |#>{meta::external::dataquality::tests::domain::db.personTable}#->select(~[FIRSTNAME, AGE])->from(meta::external::dataquality::tests::domain::DataQualityRuntime);\n" +
            "  keys: [FIRSTNAME];\n" +
            "  strategy: MD5Hash;\n" +
            "}";

    @Test
    public void testCanGenerate()
    {
        PureModelContextData pureModelContextData = load(RELATION_COMPARISON);
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        DataQualityRelationComparisonArtifactGenerationExtension extension = new DataQualityRelationComparisonArtifactGenerationExtension();
        assertEquals(DataQualityRelationComparisonArtifactGenerationExtension.ROOT_PATH, extension.getKey());
        assertTrue(extension.canGenerate(model.getPackageableElement("meta::dataquality::TestRelationComparison")));
        assertFalse(extension.canGenerate(model.getPackageableElement("meta::external::dataquality::tests::domain::Person")));
    }

    @Test
    public void testGenerateProducesReconExecutionPlanArtifact()
    {
        PureModelContextData pureModelContextData = load(RELATION_COMPARISON);
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        DataQualityRelationComparisonArtifactGenerationExtension extension = new DataQualityRelationComparisonArtifactGenerationExtension();
        List<Artifact> outputs = extension.generate(model.getPackageableElement("meta::dataquality::TestRelationComparison"), model, pureModelContextData, "vX_X_X");
        assertEquals(1, outputs.size());
        Artifact planArtifact = outputs.get(0);
        assertEquals(DataQualityRelationComparisonArtifactGenerationExtension.EXECUTION_PLAN_FILE_NAME, planArtifact.path);
        assertEquals("json", planArtifact.format);
        assertNotNull(planArtifact.content);
        assertTrue(planArtifact.content.contains("rootExecutionNode"));
    }

    @Test
    public void testGenerateSwallowsExceptionAndReturnsEmpty()
    {
        PureModelContextData pureModelContextData = load(RELATION_COMPARISON);
        PureModel model = Compiler.compile(pureModelContextData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        DataQualityRelationComparisonArtifactGenerationExtension extension = new DataQualityRelationComparisonArtifactGenerationExtension();
        List<Artifact> outputs = extension.generate(model.getPackageableElement("meta::external::dataquality::tests::domain::Person"), model, pureModelContextData, "vX_X_X");
        assertTrue(outputs.isEmpty());
    }

    private static PureModelContextData load(String code)
    {
        return GrammarParseTestUtils.loadPureModelContextFromResources(
                FastList.newListWith(
                        "core_dataquality_test/dataquality_test_model.pure",
                        "core_dataquality_test/dataquality_test_model_legend.txt"),
                code, TestDataQualityRelationComparisonArtifactGenerationExtension.class);
    }
}
