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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.semistructured;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Relational_Postgres_Semistructured
{
    public static Test suite()
    {
        String testPackage = "meta::relational::tests::pct::postgres::semistructured";
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();

        // Semi-structured scenarios Postgres cannot run end to end. Each entry maps a
        // fully-qualified Pure test to a substring that must appear in the thrown
        // exception, so a test that starts failing differently is reported rather than
        // silently absorbed. Anything not named here is rethrown untouched.
        //
        // The relation-function-mapping collection below (added 2026-10-17) was never
        // previously wired up for Postgres - these fall into three groups:
        //  - Most ModelJoin-based tests hit a SQL alias-numbering bug specific to this
        //    code path: the generated query references e.g. "person_table_0.FIRM_DETAILS"
        //    or "...MANAGERID" but the actual FROM-clause alias is numbered differently
        //    ("person_table_2"), so Postgres rejects the column reference outright. Root
        //    cause not yet investigated - needs real Postgres dialect work, not a one-line
        //    fix. The two `arrayscalar` lambda-routing tests below (added later, exercising
        //    a ModelJoin through a lambda parameter for the first time) hit the same bug
        //    on the association's "ID" column - same code path, new column name.
        //  - array_sum() now has a Postgres DynaFunction translation (array_max/array_min
        //    already did), but the numeric result still can't flow back into a jsonb-typed
        //    expression in some callers: "cannot cast type double precision to jsonb".
        //  - `operator does not exist: integer = boolean` and `char_length(jsonb)` were
        //    both the same root cause (IS [NOT] DISTINCT FROM binds more loosely than '='
        //    in Postgres, same as Spark/Databricks) or a missing jsonb-to-text unwrap -
        //    both fixed directly in postgresExtension.pure, nothing left to skip here.
        MutableMap<String, String> pathToReason = Maps.mutable.<String, String>empty()
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::misc::multibinding::testSemiStructuredRelOpWithJoinPropertyAccessFromMultipleBindingMapping_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::misc::parsejson::testParseJsonInMapping_Connection_1__Boolean_1_",
                          "column person_table_varchar_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::simple::testCombinedComplexAndSemiStructuredPropertyAccess_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::simple::testCombinedComplexAndSemiStructuredPropertyAccessParallel_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::simple::testFilterWithSemiStructuredPropertyAccessAtNestedProperty_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::simple::testSemiStructuredPropertyAccessAtNestedProperty_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::simple::testSemiStructuredPropertyAccessAtNestedPropertyWithProjectFunctions_Connection_1__Boolean_1_",
                          "column person_table_0.MANAGERID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testArithmeticAndConcatOfCollapses_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testCardinalityAndDistinct_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testComplexProjectionWithFnCalls_Connection_1__Boolean_1_",
                          "cannot cast type double precision to jsonb")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testCompoundFilterAndNestedProject_Connection_1__Boolean_1_",
                          "cannot cast type double precision to jsonb")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testCrossSideDeepFilter_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testCrossSideScalarAndFirst_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testCrossSideSizeAndPostCollapse_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testEmptyArrayLhs_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFanOutOneToMany_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterByCrossMappedFirm_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterOnFirmWithDeptExists_Connection_1__Boolean_1_",
                          "column firm_table_0.ID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterThenCardinality_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterThenIndexed_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterThenNumericAgg_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFilterThenPostCollapseChain_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testFoldAndTake_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testIfThenElseOverCollapse_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testIfWithSemiStructuredConditionAndModelJoinBranch_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testIndexedAccess_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testInequalityGtAndGe_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testInequalityNe_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testMembership_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testMissingJsonKey_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNavOnOptionalProp_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNavScalarAndNested_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNestedAtNAndScalarFn_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNestedDirectCollapse_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNestedFilterWithPredicateOnOptional_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNullOnFirmSide_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testNumericAggregations_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testPostCollapseClassNav_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testProjectThroughModelJoinAfterSemiStructuredFilter_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testSemiStructuredExistsAndModelJoinSiblingInFilter_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testSortByVariantAggregation_Connection_1__Boolean_1_",
                          "cannot cast type double precision to jsonb")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testTargetSideFold_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::variant::modelJoin::testTargetSideMapThenFirst_Connection_1__Boolean_1_",
                          "column person_table_0.FIRM_DETAILS does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::arrayscalar::testLambdaOverRoutedCollectionPassedAfterAnotherArgument_Connection_1__Boolean_1_",
                          "column person_table_0.ID does not exist")
            .withKeyValue("meta::relational::tests::mapping::relation::semistructured::arrayscalar::testLambdaOverSemiStructuredCollectionNestedInLambdaOverRoutedCollection_Connection_1__Boolean_1_",
                          "column person_table_0.ID does not exist");

        Map<CoreInstance, String> failures = pathToReason.collect(
                (k, v) -> Tuples.pair(executionSupport.getProcessorSupport().package_getByUserPath(k), v));

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> executor = (test, params) ->
        {
            Object result;
            try
            {
                result = PureTestBuilderCompiled.executeFn(test, null, Maps.mutable.empty(), executionSupport, params);
            }
            // Throwable rather than Exception: a wrong-result test fails with an AssertionError.
            catch (Throwable e)
            {
                String reason = failures.get(test);
                if (reason != null)
                {
                    if (e.getMessage() == null || !e.getMessage().contains(reason))
                    {
                        throw new AssertionError("Expect failure to contains: " + reason, e);
                    }
                    return true;
                }
                throw e;
            }
            // Reached only when the test passed. An entry that no longer reproduces is worse
            // than no entry: it silently suppresses whatever regresses into it next.
            if (failures.containsKey(test))
            {
                throw new AssertionError("Expected this test to fail with: " + failures.get(test) + ", but it passed. Remove the entry.");
            }
            return result;
        };

        return wrapSuite(
                () -> true,
                () -> PureTestBuilder.buildSuite(
                        TestCollection.collectTests(testPackage, executionSupport.getProcessorSupport(),
                                fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport),
                                ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport())
                        ),
                        executor,
                        executionSupport
                ),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
        );
    }
}
