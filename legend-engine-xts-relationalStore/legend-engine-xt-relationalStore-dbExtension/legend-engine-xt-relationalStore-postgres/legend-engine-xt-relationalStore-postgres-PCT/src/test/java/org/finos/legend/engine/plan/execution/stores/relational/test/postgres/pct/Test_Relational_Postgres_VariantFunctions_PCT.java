// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalPostgresPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.VariantCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Postgres_VariantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = VariantCodeRepositoryProvider.variantFunctions;
    private static final Adapter adapter = CoreRelationalPostgresPCTCodeRepositoryProvider.postgresAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromBadString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Invalid Pure Boolean: 'hello'\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromString_Function_1__Boolean_1_", "invalid input syntax for type boolean: \"\"false\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTimeFromWrongString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"DateTime must include time information, got: 2020-01-01\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTime_Function_1__Boolean_1_", "\"\nexpected: %2020-01-01T01:01:00.000+0000\nactual:   %2020-01-01T01:01:00.000000000+0000\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::to::testToFloatFromString_Function_1__Boolean_1_", "invalid input syntax for type double precision: \"\"1.25\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromFloat_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Variant of type 'NUMBER' cannot be converted to Integer\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromStringFloat_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"For input string: \"1.25\"\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromString_Function_1__Boolean_1_", "invalid input syntax for type bigint: \"\"1\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::to::testToNull_Function_1__Boolean_1_", "invalid input syntax for type bigint: \"null\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDateFromWrongString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"StrictDate must be a calendar day, got: 2020\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::to::testToString_Function_1__Boolean_1_", "\"\nexpected: 'Hello'\nactual:   '\"Hello\"'\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toMany::testToManyFromNonArray_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Expect variant that contains an 'ARRAY', but got 'STRING'\"\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testEmpty_Function_1__Boolean_1_", "could not determine polymorphic type because input has type unknown", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfList_Function_1__Boolean_1_", "\"\nexpected: '[[[1]]]'\nactual:   '[1]'\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfMap_Function_1__Boolean_1_", "\"\nexpected: '[{\"hello\":[{\"world\":2}]}]'\nactual:   '{\"hello\":{\"world\":2}}'\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfVariantValues_Function_1__Boolean_1_", "could not determine polymorphic type because input has type unknown", AdapterQualifier.needsInvestigation),

            // map
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariant_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),

            // filter
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariant_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),

            // fold
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariant_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Postgres\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature)

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
        );
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public Adapter getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
