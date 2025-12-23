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

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalH2PCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.VariantCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_H2_VariantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = VariantCodeRepositoryProvider.variantFunctions;
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
            one("meta::pure::functions::variant::convert::tests::to::testToMapFromNonObjectVariant_Function_1__Boolean_1_", "Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Map<String, Variant> is not managed yet!\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithIntegerValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithVariantValues_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToNull_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDateFromWrongString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDate_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStringFromBoolean_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToStringFromNumber_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::to::testToString_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toJson::testObjectToJson_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toJson().", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::toMany::testToManyFromNonArray_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyInteger_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyVariant_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testEmpty_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testFloat_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testFloats_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testInteger_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testIntegers_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfList_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testStrings_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\""),

            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfMap_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfMap_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfVariantValues_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariant().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithMultipleKeys_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithPrimitiveValues_Function_1__Boolean_1_", "Couldn't find DynaFunction to Postgres model translation for toVariantObject().", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfPrimitives_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfVariants_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::convert::tests::toVariant::testString_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariant().\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::navigation::tests::get::testGetFromArray_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::navigation::tests::get::testGetFromObjectWhenKeyExists_Function_1__Boolean_1_", "com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'world': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::navigation::tests::get::testNestedGet_mapToArray_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::variant::navigation::tests::get::testNestedGet_arrayToMap_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),

            // fold
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariantAsPrimitive_Function_1__Boolean_1_", "Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariant_Function_1__Boolean_1_", "Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda", AdapterQualifier.needsImplementation),

            // map
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariant_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda", AdapterQualifier.needsImplementation),

            // filter
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariant_Function_1__Boolean_1_", "Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::variant::convert::tests::to::testToClass_Function_1__Boolean_1_", "mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::to::Person"),
            one("meta::pure::functions::variant::convert::tests::to::testToClassWithInheritance_Function_1__Boolean_1_", "mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::to::Pet"),
            one("meta::pure::functions::variant::convert::tests::toMany::testToClass_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),
            one("meta::pure::functions::variant::convert::tests::toMany::testToClassWithInheritance_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),

            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_manyToMany_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_manyToOne_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_oneToMany_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_oneToOne_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_manyToMany_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_manyToOne_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_oneToMany_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_oneToOne_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessQualifiedProperty_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndAccessProperty_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndInstanceOf_withTypeLookup_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndInstanceOf_Function_1__Boolean_1_", "Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceUsingCustomTypeProperty_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceUsingCustomTypePropertyAndClassToType_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessProperty_Function_1__Boolean_1_", "Match failure: DataTypeInfoObject instanceOf DataTypeInfo")

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
