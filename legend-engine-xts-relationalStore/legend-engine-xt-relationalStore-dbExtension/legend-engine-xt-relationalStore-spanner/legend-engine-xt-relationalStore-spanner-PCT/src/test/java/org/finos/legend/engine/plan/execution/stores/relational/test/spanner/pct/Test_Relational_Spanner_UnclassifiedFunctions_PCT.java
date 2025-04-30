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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Spanner_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.spannerAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //hash
            one("meta::pure::functions::hash::tests::testMD5Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function md5(text) is not supported - Statement: 'select md5(Text'Hello, World!')'"),
            one("meta::pure::functions::hash::tests::testSHA1Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function sha1(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select sha1(Text'Hello, World!')'"),
            one("meta::pure::functions::hash::tests::testSHA256Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function sha256(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select sha256(Text'Hello, World!')'"),

            //ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiDigit_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'0'))'"),
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text''))'"),
            one("meta::pure::functions::string::tests::ascii::testAsciiLower_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'a'))'"),
            one("meta::pure::functions::string::tests::ascii::testAsciiMultiCharString_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'abc'))'"),
            one("meta::pure::functions::string::tests::ascii::testAsciiNewline_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::string::tests::ascii::testAsciiUpper_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'A'))'"),
            one("meta::pure::functions::string::tests::ascii::testAsciiWhitespace_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text' '))'"),

            //char
            one("meta::pure::functions::string::tests::char::testCharDigits_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(48)'"),
            one("meta::pure::functions::string::tests::char::testCharLower_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(97)'"),
            one("meta::pure::functions::string::tests::char::testCharUpper_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(65)'"),
            one("meta::pure::functions::string::tests::char::testEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(0)'"),
            one("meta::pure::functions::string::tests::char::testNewLine_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(32)'"),

            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64RoundTrip_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'encodeBase64_String_1__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'decodeBase64_String_1__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'encodeBase64_String_1__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //jaroWinklersimilarity
            pack("meta::pure::functions::string::tests::jaroWinklerSimilarity", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\""),

            //levenshteindistance
            pack("meta::pure::functions::string::tests::levenshteinDistance", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\""),

            //lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: Third argument (pad pattern) for LPAD/RPAD cannot be empty - Statement: 'select lpad(Text'abcd', 10, Text'')'"),

            //rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: Third argument (pad pattern) for LPAD/RPAD cannot be empty - Statement: 'select rpad(Text'abcd', 10, Text'')'"),

            //matches
            pack("meta::pure::functions::string::tests::matches", "\"[unsupported-api] The function 'matches' (state: [Select, false]) is not supported yet\""),

            //splitpart
            pack("meta::pure::functions::string::tests::splitPart", "NOT_FOUND: [ERROR] function split_part")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Spanner).getFirst())
        );
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
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
