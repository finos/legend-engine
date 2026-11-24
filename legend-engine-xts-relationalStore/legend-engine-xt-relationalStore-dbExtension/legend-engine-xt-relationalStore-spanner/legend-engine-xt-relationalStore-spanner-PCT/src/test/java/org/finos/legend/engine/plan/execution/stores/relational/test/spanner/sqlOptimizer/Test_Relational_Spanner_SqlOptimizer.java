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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.sqlOptimizer;

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

public class Test_Relational_Spanner_SqlOptimizer
{
    public static Test suite()
    {
        String testPackage = "meta::relational::tests::pct::spanner::sqlOptimizer";
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();

        // Fully-qualified Pure test -> a substring that must appear in its failure.
        MutableMap<String, String> pathToReason = Maps.mutable.<String, String>empty()
                // Spanner (PostgreSQL interface) has no CHAR type, so the CHAR fixture tables this test needs cannot be created.
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCharKeysDoNotTransfer_Connection_1__Boolean_1_",
                        "C CHAR(5) NULL")
                // The Spanner printer does not support window columns.
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testDistinctThatCountsOrNumbersRowsKeepsANonUniqueJoin_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testRightSideReadAnywhereKeepsTheJoin_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testRightSubSelectWithoutAUniquenessProofKeepsTheJoin_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testColumnWindowKeysStillMerge_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testLiteralWindowKeyRefusesTheMerge_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::elimination::testEliminationDoesNotMergeAWindowColumnIntoTheOuterWhere_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::elimination::testEliminationRewritesTheWindowOfAMergedSubSelect_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::mappings::testWindowOverARenamedRelationStaysResolvable_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::pushDown::testSubQueryPushDownStopsAtAWindowPartitionedByAnotherColumn_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::trimming::testTrimmingDropsUnreadColumnsBesideAWindowColumnTheParentReads_Connection_1__Boolean_1_",
                        "Window Columns not supported")
                // The Spanner printer has no parseInteger.
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::elimination::testEliminationRefusesToMergeFiltersWhenTheOuterPredicateCanFailOnRowsTheInnerFilterRemoves_Connection_1__Boolean_1_",
                        "The function 'parseInteger'")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::elimination::testEliminationRefusesToMergeWhenTheOuterJoinConditionCanFailOnRowsTheInnerFilterRemoves_Connection_1__Boolean_1_",
                        "The function 'parseInteger'")
                // The dialect's SQL printer has no lateral join, so the query under test cannot be printed.
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::trimming::testEveryRuleKeepsAColumnReadOnlyByALateralSibling_Connection_1__Boolean_1_",
                        "Lateral operation not supported")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::mappings::testEveryFeatureKeepsLateralRelationQueriesRunnable_Connection_1__Boolean_1_",
                        "Lateral operation not supported")
                // The query under test filters on its own SELECT alias, which standard SQL does not resolve in WHERE.
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::trimming::testEveryRuleKeepsAColumnReadOnlyByTheSubSelectsOwnWhereClause_Connection_1__Boolean_1_",
                        "column \"q\" does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::trimming::testEveryRuleKeepsAColumnReadOnlyByAUnionBranchsOwnWhereClause_Connection_1__Boolean_1_",
                        "column \"q\" does not exist")
                // No implicit VARCHAR/INTEGER comparison here, so the query under test cannot run.
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCoercingKeyDoesNotTransfer_Connection_1__Boolean_1_",
                        "operator does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCoercingKeyDoesNotTransferAPattern_Connection_1__Boolean_1_",
                        "operator does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCoercingKeyKeepsTheJoin_Connection_1__Boolean_1_",
                        "operator does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCopyingAComparisonAcrossACoercingKeyWouldChangeRows_Connection_1__Boolean_1_",
                        "operator does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testCopyingAPatternAcrossACoercingKeyWouldChangeRows_Connection_1__Boolean_1_",
                        "operator does not exist")
                .withKeyValue("meta::relational::tests::sqlOptimizer::duckDbRefusals::testRepeatedLeftJoinOnACoercingKeyIsNotMerged_Connection_1__Boolean_1_",
                        "operator does not exist")
                // Spanner's PostgreSQL interface folds unquoted names to lower case, so the input's unquoted inner `v` never matches
                // its quoted outer "V".
                .withKeyValue("meta::relational::tests::sqlOptimizer::semantics::trimming::testTrimmingMatchesAnUnquotedInnerNameToAQuotedOuterReferenceOfAnotherCase_Connection_1__Boolean_1_",
                        "column s.V does not exist");

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
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Spanner).getFirst())
        );
    }
}
