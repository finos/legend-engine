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
                // json has no btree operator class, so the fixture's semi-structured primary key
                // cannot be created at all.
                .withKeyValue(
                        "meta::relational::tests::semistructured::join::testJoinOnSemiStructuredPropertyWithQPFilter_Connection_1__Boolean_1_",
                        "Error while executing: Create Table FIRM_SCHEMA.FIRM_TABLE")
                .withKeyValue(
                        "meta::relational::tests::semistructured::join::testJoinOnSemiStructuredProperty_Connection_1__Boolean_1_",
                        "Error while executing: Create Table FIRM_SCHEMA.FIRM_TABLE")
                // A union branch mixes a varchar column with a json one.
                .withKeyValue(
                        "meta::relational::tests::semistructured::union::testSemiStructuredUnionMappingWithBindingAndFilter_Connection_1__Boolean_1_",
                        "ERROR: UNION types character varying and json cannot be matched")
                .withKeyValue(
                        "meta::relational::tests::semistructured::union::testSemiStructuredUnionMappingWithBinding_Connection_1__Boolean_1_",
                        "ERROR: UNION types character varying and json cannot be matched")
                // The correlated reference to the outer table is lost inside the generated sub-select.
                .withKeyValue(
                        "meta::relational::tests::semistructured::explode::testComplexProjectFlattenedAndExplodedPropertiesInProject_Connection_1__Boolean_1_",
                        "ERROR: invalid reference to FROM-clause entry for table \"root\"")
                // The value returns as json text containing commas, which the CSV comparison helper
                // then reads as extra columns.
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::relationFunctionMapping::testSemiStructuredPrimitivePropertyArrayIndexing_Connection_1__Boolean_1_",
                        "planExecutionTestUtility.pure")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredPrimitivePropertyArrayIndexing_Connection_1__Boolean_1_",
                        "planExecutionTestUtility.pure")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::relationFunctionMapping::testSemiStructuredArrayElementAccessPrimitive_Connection_1__Boolean_1_",
                        "planExecutionTestUtility.pure")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::testSemiStructuredArrayElementAccessPrimitive_Connection_1__Boolean_1_",
                        "planExecutionTestUtility.pure")
                // Other SQL that Postgres rejects.
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayFilterFirstJoinStrings_Connection_1__Boolean_1_",
                        "ERROR: function json_array_elements(text) does not exist")
                // Runs to completion but returns the wrong values.
                .withKeyValue(
                        "meta::relational::tests::semistructured::extract::testAllDataTypesAccess_Connection_1__Boolean_1_",
                        "actual:   'Id,Legal Name,Est Date,Mnc,Employee Count,Revenue,Last Updat")
                .withKeyValue(
                        "meta::relational::tests::semistructured::extract::testArrayElementNoFlattenAccess_Connection_1__Boolean_1_",
                        "actual:   'Id,Second Line of Address")
                .withKeyValue(
                        "meta::relational::tests::semistructured::extract::testDotAndBracketNotationAccess_Connection_1__Boolean_1_",
                        "actual:   'Id,Dot Only,Bracket Only,Dot & Bracket")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::relationFunctionMapping::testSemiStructuredComplexPropertyArrayIndexingFollowedBySubType_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address 0 Line 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::relationFunctionMapping::testSemiStructuredComplexPropertyArrayIndexing_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address 0 Name,Firm Address 2 Name")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::relationFunctionMapping::testSemiStructuredComplexPropertyFlatteningFollowedBySubType_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address Line 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::relationFunctionMapping::testSemiStructuredMultiFlatten_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address Name,Firm Address Line 0 No,Firm Oth")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredArrayDirectAt_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,First Address Name")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredComplexPropertyArrayIndexingFollowedBySubType_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address 0 Line 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredComplexPropertyArrayIndexing_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address 0 Name,Firm Address 2 Name")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredComplexPropertyFlatteningFollowedBySubType_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Name,Firm Address Line 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::flattening::testSemiStructuredMultiFlatten_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address Name,Firm Address Line 0 No,Firm Oth")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::relationFunctionMapping::testSemiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No,Firm Address Street,Firm A")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::relationFunctionMapping::testSemiStructuredPropertyAccessAtSubClassNested_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No,Firm Address Street,Firm A")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::relationFunctionMapping::testSemiStructuredPropertyAccessAtSubClass_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::testSemiStructuredPropertyAccessAtSubClassNestedUsingProjectWithFunctions_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No,Firm Address Street,Firm A")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::testSemiStructuredPropertyAccessAtSubClassNested_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No,Firm Address Street,Firm A")
                .withKeyValue(
                        "meta::relational::tests::semistructured::inheritance::testSemiStructuredPropertyAccessAtSubClass_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address 0 Line No")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::relationFunctionMapping::testIsEmptyCheckOnSemiStructuredPropertyAccessAfterAt_Connection_1__Boolean_1_",
                        "actual:   'First Name,First Address Line")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::relationFunctionMapping::testSemiStructuredArrayElementAccessComplex_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address Line 0,Firm Address Line 1,Firm Addr")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::testIsEmptyCheckOnSemiStructuredPropertyAccessAfterAt_Connection_1__Boolean_1_",
                        "actual:   'First Name,First Address Line")
                .withKeyValue(
                        "meta::relational::tests::semistructured::simple::testSemiStructuredArrayElementAccessComplex_Connection_1__Boolean_1_",
                        "actual:   'First Name,Firm Address Line 0,Firm Address Line 1,Firm Addr")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::relationFunctionMapping::testSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product 0 Type,Product 1 Type")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::relationFunctionMapping::testSemiStructuredTypeNameFunctionUsageAfterFlattenColSpec_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product Type")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::relationFunctionMapping::testSemiStructuredTypeNameFunctionUsageAfterFlattenFunction_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product Type")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::testSemiStructuredTypeNameFunctionUsageAfterArrayElementAccess_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product 0 Type,Product 1 Type")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::testSemiStructuredTypeNameFunctionUsageAfterFlattenColSpec_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product Type")
                .withKeyValue(
                        "meta::relational::tests::semistructured::typeFunctions::testSemiStructuredTypeNameFunctionUsageAfterFlattenFunction_Connection_1__Boolean_1_",
                        "actual:   'Order Id,Product Type");

        Map<CoreInstance, String> failures = pathToReason.collect(
                (k, v) -> Tuples.pair(executionSupport.getProcessorSupport().package_getByUserPath(k), v));

        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> executor = (test, params) ->
        {
            try
            {
                return PureTestBuilderCompiled.executeFn(test, null, Maps.mutable.empty(), executionSupport, params);
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
