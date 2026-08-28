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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.semistructured;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Relational_Databricks_Semistructured
{
    public static Test suite()
    {
        String testPackage = "meta::relational::tests::pct::databricks::semistructured";
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();

        // Remaining Databricks semi-structured failures after the databricksExtension.pure
        // fixes (see the fold/map/filter lambda builders, joinStrings dynaFn, and
        // explode try_cast changes). Each entry: fully-qualified Pure test function path
        // -> a substring that must appear in the thrown exception's message. See
        // docs/pct/expected-failures-howto.md for the qualifier taxonomy referenced in
        // the inline comments below. If a run produces a different error message the
        // executor will re-throw wrapped in an AssertionError so we don't silently mask
        // regressions.
        MutableMap<String, String> pathToReason = Maps.mutable.<String, String>empty()
                // [needsInvestigation] Router bug: generated SQL references `Id`
                // unqualified when both `blocks_1.Id` (from the exploded array) and
                // `root.Id` (from the base row) are in scope after array explode + group-by.
                // Needs an alias-injection pass in the aggregate router for exploded
                // sub-selects, not a Databricks-specific fix.
                .withKeyValue(
                        "meta::relational::tests::semistructured::explode::testAggregationAggregateExplodedPropertyUsingGroupBy_Connection_1__Boolean_1_",
                        "[AMBIGUOUS_REFERENCE]")
                // [needsInvestigation] Union-mapping-with-binding drops non-Databricks
                // input rows -- expected 3 firms (A/B/D) but only firm_D returned.
                // Likely a binding-selection bug in the union router; needs the actual
                // generated SQL to diagnose further.
                .withKeyValue(
                        "meta::relational::tests::semistructured::union::testSemiStructuredUnionMappingWithBindingAndFilter_Connection_1__Boolean_1_",
                        "actual:   'Firm/FirmName\\nfirm_D'")
                // [needsInvestigation] Union-mapping-with-binding: expected 6 firms
                // (A/B/C/D/E/F) but returned 3 real firms + 3 nulls. Same class of
                // issue as testSemiStructuredUnionMappingWithBindingAndFilter above.
                .withKeyValue(
                        "meta::relational::tests::semistructured::union::testSemiStructuredUnionMappingWithBinding_Connection_1__Boolean_1_",
                        "actual:   'Firm/FirmName\\nfirm_D\\nfirm_E\\nfirm_F\\nnull\\nnull\\nnull'")
                // [needsInvestigation] Filtering a semi-structured array then indexing it
                // with at(0): filtering to zero matches still reaches the index-access SQL,
                // which throws a hard out-of-bounds error instead of the NULL Pure's at()
                // semantics expect for an empty collection. Needs a `get()`/try_element_at-
                // style tolerant index rewrite in the Databricks array-index codegen.
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterAtIndex_Connection_1__Boolean_1_",
                        "[INVALID_ARRAY_INDEX]");

        Map<CoreInstance, String> failures = pathToReason.collect(
                (k, v) -> Tuples.pair(executionSupport.getProcessorSupport().package_getByUserPath(k), v));

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> executor = (test, params) ->
        {
            try
            {
                return PureTestBuilderCompiled.executeFn(test, null, Maps.mutable.empty(), executionSupport, params);
            }
            // Throwable rather than Exception: a wrong-result test fails with an AssertionError.
            // Anything not named in pathToReason is still rethrown untouched.
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
        };

        TestConnectionIntegration databricks = TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Databricks).getFirst();

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
                // The reset must follow the integration in this list: ServersState.start() runs the
                // resources in order, and it needs the connection the integration builds in start().
                Lists.mutable.with((TestServerResource) databricks, new DatabricksFixtureSchemaReset(databricks))
        );
    }
}
