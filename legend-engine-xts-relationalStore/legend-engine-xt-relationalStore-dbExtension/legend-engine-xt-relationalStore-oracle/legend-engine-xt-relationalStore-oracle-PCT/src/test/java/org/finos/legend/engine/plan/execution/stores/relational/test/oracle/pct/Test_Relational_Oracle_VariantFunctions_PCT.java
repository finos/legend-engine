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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalOraclePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.VariantCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Oracle_VariantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = VariantCodeRepositoryProvider.variantFunctions;
    private static final Adapter adapter = CoreRelationalOraclePCTCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            one("meta::pure::functions::variant::convert::tests::fromJson::testJsonObject_Function_1__Boolean_1_", "\"\nexpected: '{\"Hello\":null}'\nactual:   '{\"Hello\":{\"oracleJsonType\":\"NULL\"}}'\""),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFalse_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromBadString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Invalid Pure Boolean: 'hello'\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanFromString_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToBooleanTrue_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToClassWithInheritance_Function_1__Boolean_1_", "\"mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::to::Pet\""),
            one("meta::pure::functions::variant::convert::tests::to::testToClass_Function_1__Boolean_1_", "\"mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::to::Person\""),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTimeFromWrongString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"DateTime must include time information, got: 2020-01-01\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToDateTime_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00932: inconsistent datatypes: expected TIMESTAMP got JSON\n\nhttps://docs.oracle.com/error-help/db/ora-00932/"),
            one("meta::pure::functions::variant::convert::tests::to::testToFloatFromInteger_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00905: missing keyword\n\nhttps://docs.oracle.com/error-help/db/ora-00905/"),
            one("meta::pure::functions::variant::convert::tests::to::testToFloatFromString_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00905: missing keyword\n\nhttps://docs.oracle.com/error-help/db/ora-00905/"),
            one("meta::pure::functions::variant::convert::tests::to::testToFloat_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00905: missing keyword\n\nhttps://docs.oracle.com/error-help/db/ora-00905/"),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromFloat_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Integer is not managed yet!\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromStringFloat_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"For input string: \"1.25\"\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToIntegerFromString_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToInteger_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToListFromNonArrayVariant_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"List<Variant> is not managed yet!\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToListOfIntegers_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToListOfVariants_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToNull_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDateFromWrongString_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"StrictDate must be a calendar day, got: 2020\"\""),
            one("meta::pure::functions::variant::convert::tests::to::testToStrictDate_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00932: inconsistent datatypes: expected DATE got JSON\n\nhttps://docs.oracle.com/error-help/db/ora-00932/"),
            one("meta::pure::functions::variant::convert::tests::to::testToString_Function_1__Boolean_1_", "\"\nexpected: 'Hello'\nactual:   '\"Hello\"'\""),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithVariantValues_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\""),
            one("meta::pure::functions::variant::convert::tests::to::testToMapWithIntegerValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\""),

            one("meta::pure::functions::variant::convert::tests::toMany::testToClassWithInheritance_Function_1__Boolean_1_", "\"mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::toMany::Pet\""),
            one("meta::pure::functions::variant::convert::tests::toMany::testToClass_Function_1__Boolean_1_", "\"mapping missing and cannot construct return type for class: meta::pure::functions::variant::convert::tests::toMany::Person\""),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyFromNonArray_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Expect variant that contains an 'ARRAY', but got 'STRING'\"\""),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyInteger_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),
            one("meta::pure::functions::variant::convert::tests::toMany::testToManyVariant_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00902: invalid datatype\n\nhttps://docs.oracle.com/error-help/db/ora-00902/"),

            pack("meta::pure::functions::variant::convert::tests::toJson", "[unsupported-api] The function 'toJson' (state: [Select, false]) is not supported yet"),

            pack("meta::pure::functions::variant::convert::tests::toVariant", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::convert::tests::toVariant::testListOfMap_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfMap_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapOfVariantValues_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithMultipleKeys_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::convert::tests::toVariant::testMapWithPrimitiveValues_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),

            // map
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::tests::collection::map::testMap_FromVariant_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),

            // filter
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariant_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::tests::collection::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet"),

            //fold
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::collection::fold::testFold_FromVariant_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),

            pack("meta::pure::functions::variant::navigation::tests::get", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),

            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_manyToMany_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_manyToOne_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_oneToMany_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_manyToMany_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_manyToOne_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_oneToMany_Function_1__Boolean_1_", "[unsupported-api] The function 'array_flatten' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedQualifiedProperty_oneToOne_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessQualifiedProperty_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessNestedProperty_oneToOne_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassAndAccessProperty_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndAccessProperty_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndInstanceOf_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndInstanceOf_withTypeLookup_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceUsingCustomTypePropertyAndClassToType_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceUsingCustomTypeProperty_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle"),
            one("meta::pure::functions::variant::tests::convert::to::model::testToClassWithInheritanceAndInstanceOf_withTypeLookup_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Oracle")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Oracle).getFirst())
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
