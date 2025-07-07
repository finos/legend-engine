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
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
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
            one("meta::pure::functions::hash::tests::testMD5Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function md5(text) is not supported - Statement: 'select md5(Text'Hello, World!')'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::hash::tests::testSHA1Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function sha1(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select sha1(Text'Hello, World!')'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::hash::tests::testSHA256Hash_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function sha256(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select sha256(Text'Hello, World!')'", AdapterQualifier.needsInvestigation),

            //ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiDigit_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'0'))'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text''))'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiLower_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'a'))'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiMultiCharString_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'abc'))'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiNewline_Function_1__Boolean_1_", "Unexpected token", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiUpper_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text'A'))'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::ascii::testAsciiWhitespace_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function to_code_points(text) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select array_first(to_code_points(Text' '))'", AdapterQualifier.needsInvestigation),

            //char
            one("meta::pure::functions::string::tests::char::testCharDigits_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(48)'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::char::testCharLower_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(97)'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::char::testCharUpper_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(65)'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::char::testEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(0)'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::char::testNewline_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(10)'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::char::testSpace_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function code_points_to_string(bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select code_points_to_string(32)'", AdapterQualifier.needsInvestigation),

            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64RoundTrip_Function_1__Boolean_1_", "\"[unsupported-api] The function 'encodeBase64' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64RoundTrip_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64_Function_1__Boolean_1_", "\"[unsupported-api] The function 'encodeBase64' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //jaroWinklersimilarity
            pack("meta::pure::functions::string::tests::jaroWinklerSimilarity", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //levenshteindistance
            pack("meta::pure::functions::string::tests::levenshteinDistance", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: Third argument (pad pattern) for LPAD/RPAD cannot be empty - Statement: 'select lpad(Text'abcd', 10, Text'')'", AdapterQualifier.unsupportedFeature),

            //rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: Third argument (pad pattern) for LPAD/RPAD cannot be empty - Statement: 'select rpad(Text'abcd', 10, Text'')'", AdapterQualifier.unsupportedFeature),

            //matches
            pack("meta::pure::functions::string::tests::matches", "\"[unsupported-api] The function 'matches' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //splitpart
            pack("meta::pure::functions::string::tests::splitPart", "NOT_FOUND: [ERROR] function split_part", AdapterQualifier.needsInvestigation),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterAlreadyLower_Function_1__Boolean_1_", "\"\nexpected: 'xOxOxOx'\nactual:   'xxOxOxOx'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterNumber_Function_1__Boolean_1_", "\"\nexpected: '1isOne'\nactual:   '11isOne'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'xoXoXoX'\nactual:   'xXoXoXoX'\"", AdapterQualifier.needsInvestigation),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterAlreadyLower_Function_1__Boolean_1_", "\"\nexpected: 'XoXoXoX'\nactual:   'XXoXoXoX'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterNumber_Function_1__Boolean_1_", "\"\nexpected: '1isOne'\nactual:   '11isOne'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'XOxOxOx'\nactual:   'XxOxOxOx'\"", AdapterQualifier.needsInvestigation)
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
