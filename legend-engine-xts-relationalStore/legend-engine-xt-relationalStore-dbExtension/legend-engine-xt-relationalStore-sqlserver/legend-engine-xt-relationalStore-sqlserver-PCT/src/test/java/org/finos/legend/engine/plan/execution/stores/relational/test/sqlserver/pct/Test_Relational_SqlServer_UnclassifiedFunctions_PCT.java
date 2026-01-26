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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSqlServerCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_SqlServer_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalSqlServerCodeRepositoryProvider.sqlserverAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "\"\nexpected: [0]\nactual:   []\""),

            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'base64_decode' is not a recognized built-in function name."),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64RoundTrip_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'base64_encode' is not a recognized built-in function name."),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'base64_decode' is not a recognized built-in function name."),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64RoundTrip_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'base64_decode' is not a recognized built-in function name."),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'base64_encode' is not a recognized built-in function name."),

            // coalesce
            one("meta::pure::functions::flow::test::coalesce::coalesce2_AllEmpty_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: At least one of the arguments to COALESCE must be an expression that is not the NULL constant."),
            one("meta::pure::functions::flow::test::coalesce::coalesce3_AllEmpty_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: At least one of the arguments to COALESCE must be an expression that is not the NULL constant."),
            one("meta::pure::functions::flow::test::coalesce::coalesce_AllEmpty_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: At least one of the arguments to COALESCE must be an expression that is not the NULL constant."),

            // currentuserid
            one("meta::pure::functions::runtime::currentUserId::testCurrentUserId_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near ')'."),

            //jarowinklersimilarity
            pack("meta::pure::functions::string::tests::jaroWinklerSimilarity", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //levenshteindistance
            pack("meta::pure::functions::string::tests::levenshteinDistance", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //matches
            pack("meta::pure::functions::string::tests::matches", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::matches::testMatchesNoMatch_Function_1__Boolean_1_", "\"[unsupported-api] The function 'matches' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::matches::testMatches_Function_1__Boolean_1_", "\"[unsupported-api] The function 'matches' (state: [Select, false]) is not supported yet\""),

            // lpad
            one("meta::pure::functions::string::tests::lpad::testLpadStringLongerThanLength_Function_1__Boolean_1_", "\"\nexpected: ['abc']\nactual:   []\""),

            // rpad
            one("meta::pure::functions::string::tests::rpad::testRpadMultiChar_Function_1__Boolean_1_", "\"\nexpected: 'ppxox'\nactual:   'ppxoxoxo'\""),
            one("meta::pure::functions::string::tests::rpad::testRpadStringLongerThanLength_Function_1__Boolean_1_", "\"\nexpected: ['abc']\nactual:   []\""),

            // regexp
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtractAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtract_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_GroupNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplaceAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplace_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),

            //splitpart
            one("meta::pure::functions::string::tests::splitPart::testSplitPartEmptyString_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Procedure or function string_split has too many arguments specified."),
            one("meta::pure::functions::string::tests::splitPart::testSplitPartEmptyToken_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Procedure or function string_split has too many arguments specified."),
            one("meta::pure::functions::string::tests::splitPart::testSplitPartTypicalToken_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Procedure or function string_split has too many arguments specified."),
            one("meta::pure::functions::string::tests::splitPart::testSplitPartWithNoSplit_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Procedure or function string_split has too many arguments specified."),
            one("meta::pure::functions::string::tests::splitPart::testSplitPart_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Procedure or function string_split has too many arguments specified."),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'xoXoXoX'\nactual:   'XoXoXoX'\""),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'XOxOxOx'\nactual:   'xOxOxOx'\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.SqlServer).getFirst())
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
