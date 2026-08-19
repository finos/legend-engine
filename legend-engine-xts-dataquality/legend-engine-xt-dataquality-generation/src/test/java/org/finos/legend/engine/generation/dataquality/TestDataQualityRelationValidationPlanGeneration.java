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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.junit.Test;

import java.util.ServiceLoader;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Plan generation for DataQualityRelationValidation. Nothing else in the repository takes a relation
 * validation all the way to an ExecutionPlan, which is how #5091 broke the rowsWith* helpers without
 * turning any suite red.
 */
public class TestDataQualityRelationValidationPlanGeneration
{
    private static final ListIterable<PlanTransformer> TRANSFORMERS =
            Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)).flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

    private static final String QUERY =
            "    query: #>{meta::external::dataquality::tests::domain::db.personTable}#" +
            "->select(~[ID, FIRSTNAME, LASTNAME, ADDRESSID])" +
            "->from(meta::external::dataquality::tests::domain::DataQualityRuntime);\n";

    @Test
    public void testPlanGeneration_rowsWithEmptyColumn()
    {
        String plan = generatePlanJson(
                "emptyLastName",
                "rel|$rel->rowsWithEmptyColumn(~LASTNAME)->assertRelationEmpty(~[FIRSTNAME, LASTNAME])");

        assertTrue(plan, plan.contains("LASTNAME is null"));
    }

    // The shape reported against #5091: the column fed to rowsWithEmptyColumn is produced by a LEFT JOIN
    // and renamed, so it exists only in the joined relation.
    @Test
    public void testPlanGeneration_rowsWithEmptyColumn_afterFilterAndJoin()
    {
        String plan = generatePlanJson(
                "unmatchedAddress",
                "rel|$rel->filter(row|$row.FIRSTNAME->isNotEmpty())" +
                "->join(#>{meta::external::dataquality::tests::domain::db.addressTable}#->select(~[ID])->rename(~ID, ~joinedId), " +
                        "meta::pure::functions::relation::JoinKind.LEFT, {r1, r2 | $r1.ADDRESSID == $r2.joinedId})" +
                "->rowsWithEmptyColumn(~joinedId)" +
                "->assertRelationEmpty(~[FIRSTNAME, LASTNAME])");

        assertTrue(plan, plan.contains("left outer join"));
        assertTrue(plan, plan.contains("\\\"joinedId\\\" is null"));
    }

    // The four helpers whose ColSpec pins a concrete column type reach eval with a compiled-in Z, a
    // different path through the inference than the two <T,Z> helpers above.
    @Test
    public void testPlanGeneration_rowsWithValueOutsideRange()
    {
        String plan = generatePlanJson(
                "addressIdOutOfRange",
                "rel|$rel->rowsWithValueOutsideRange(~ADDRESSID, 0, 100)->assertRelationEmpty(~[FIRSTNAME, LASTNAME])");

        assertTrue(plan, plan.contains("ADDRESSID"));
    }

    private String generatePlanJson(String validationName, String assertion)
    {
        String validation = "###DataQualityValidation\n" +
                "DataQualityRelationValidation meta::dataquality::Validation\n" +
                "{\n" +
                QUERY +
                "    validations: [\n" +
                "       {" +
                "        name: '" + validationName + "';" +
                "        description: '" + validationName + "';" +
                "        assertion: " + assertion + ";" +
                "        type: AGGREGATE;" +
                "       }\n" +
                "    ];\n" +
                "}";

        PureModelContextData modelData = GrammarParseTestUtils.loadPureModelContextFromResources(
                FastList.newListWith(
                        "core_dataquality_test/dataquality_test_model.pure",
                        "core_dataquality_test/dataquality_test_model_legend.txt"),
                validation, TestDataQualityRelationValidationPlanGeneration.class);

        PureModel model = Compiler.compile(modelData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());

        LambdaFunction<?> lambda = DataQualityLambdaGenerator.generateLambda(
                model, "meta::dataquality::Validation", Sets.mutable.of(validationName), false, null, true, false, "test");
        assertNotNull(lambda);

        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions =
                (PureModel p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));

        SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(
                lambda, null, null, null, model, "vX_X_X", PlanPlatform.JAVA, null, routerExtensions.apply(model), TRANSFORMERS);
        assertNotNull(plan);
        assertNotNull(plan.rootExecutionNode);
        try
        {
            return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().writeValueAsString(plan);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
