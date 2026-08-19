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
        MutableMap<String, String> pathToReason = Maps.mutable.<String, String>empty()
                // Postgres has no tolerant flatten. json_array_elements rejects anything that is
                // not an array, whereas Snowflake's FLATTEN takes outer => true and yields no rows
                // instead of failing. Here filter(...)->first() narrows the array to a single
                // object, which joinStrings then tries to flatten.
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterFirstJoinStrings_Connection_1__Boolean_1_",
                        "cannot call json_array_elements on a non-array")
                // json has no default btree operator class, so Postgres cannot build the primary
                // key the fixture declares on the semi-structured column, and the table is never
                // created.
                //
                // Mapping Variant to jsonb instead does fix these two -- measured, not assumed --
                // and the migration is small. It is not done because jsonb stores a canonical form
                // and does not preserve object key order, which regresses three PCT tests that
                // round-trip a variant column and that Snowflake, Databricks and DuckDB all pass.
                // The trade is key-order fidelity for these two, and fidelity wins.
                .withKeyValue(
                        "meta::relational::tests::semistructured::join::testJoinOnSemiStructuredProperty_Connection_1__Boolean_1_",
                        "Error while executing: Create Table FIRM_SCHEMA.FIRM_TABLE")
                .withKeyValue(
                        "meta::relational::tests::semistructured::join::testJoinOnSemiStructuredPropertyWithQPFilter_Connection_1__Boolean_1_",
                        "Error while executing: Create Table FIRM_SCHEMA.FIRM_TABLE");

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
