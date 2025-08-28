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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.pct;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalH2PCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Relational_H2_VariantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.variantFunctions;
    private static final Adapter adapter = CoreRelationalH2PCTCodeRepositoryProvider.H2Adapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonArrayOfFloat_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonArrayOfInteger_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonArrayOfString_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonFloat_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonInteger_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonObject_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonString_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonArrayOfNull_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonArrayOfObject_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLNonTransientException: Exception calling user-defined function: \"legend_h2_extension_json_parse(", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonObject_Function_1__Boolean_1_", "\nexpected: '{\"Hello\":null}'\nactual:   '{}'\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFalse_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromBadString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanTrue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTimeFromWrongString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTime_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToFloatFromInteger_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToFloatFromString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToFloat_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromFloat_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromStringFloat_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToInteger_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToListFromNonArrayVariant_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToListOfIntegers_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToListOfVariants_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToMapFromNonObjectVariant_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithIntegerValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithVariantValues_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToNull_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDateFromWrongString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDate_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStringFromBoolean_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStringFromNumber_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toJson::testObjectToJson_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toJson().\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::toMany::testToManyFromNonArray_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyInteger_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyVariant_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testEmpty_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testFloat_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testFloats_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testInteger_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testIntegers_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfList_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfMap_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfMap_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfVariantValues_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariant().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithMultipleKeys_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithPrimitiveValues_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfPrimitives_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfVariants_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testString_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testStrings_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::navigation::tests::get::testGetFromArray_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::navigation::tests::get::testGetFromObjectWhenKeyDoesNotExists_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::navigation::tests::get::testGetFromObjectWhenKeyExists_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::navigation::tests::get::testGetFromZeroMultiplicityObject_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
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
