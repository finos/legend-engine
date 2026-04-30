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

package org.finos.legend.engine.plan.execution.stores.relational.test.trino.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalTrinoPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Trino_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalTrinoPCTCodeRepositoryProvider.trinoAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            // String - Base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64RoundTrip_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64_Function_1__Boolean_1_", "\"[unsupported-api] The function 'decodeBase64' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64RoundTrip_Function_1__Boolean_1_", "\"[unsupported-api] The function 'encodeBase64' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64_Function_1__Boolean_1_", "\"[unsupported-api] The function 'encodeBase64' (state: [Select, false]) is not supported yet\""),

            // Runtime
            one("meta::pure::functions::runtime::currentUserId::testCurrentUserId_Function_1__Boolean_1_", "mismatched input '('. Expecting:"),

            // String - Ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiDigit_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiLower_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiMultiCharString_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiNewline_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiUpper_Function_1__Boolean_1_", "Function 'ascii' not registered"),
            one("meta::pure::functions::string::tests::ascii::testAsciiWhitespace_Function_1__Boolean_1_", "Function 'ascii' not registered"),

            // String - JaroWinklerSimilarity
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\""),

            // String - LevenshteinDistance
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\""),

            // String - Lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "Padding string must not be empty"),

            // String - RegexpCount
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),

            // String - RegexpExtract
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtractAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtract_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),

            // String - RegexpIndexOf
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_GroupNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),

            // String - RegexpLike
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),

            // String - RegexpReplace
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplaceAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplace_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),

            // String - RepeatString
            one("meta::pure::functions::string::tests::repeatstring::testRepeatStringEmptyString_Function_1__Boolean_1_", "Padding string must not be empty"),
            one("meta::pure::functions::string::tests::repeatstring::testRepeatStringNoString_Function_1__Boolean_1_", "Could not choose a best candidate operator. Explicit type casts must be added."),

            // String - Right
            one("meta::pure::functions::string::tests::right::testRightTooShortString_Function_1__Boolean_1_", "expected: 'ab'\nactual:   ''"),

            // String - Rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "Padding string must not be empty"),

            // String - SplitPart
            one("meta::pure::functions::string::tests::splitPart::testSplitPartEmptyToken_Function_1__Boolean_1_", "expected: 'Hello World'\nactual:   'H'"),

            // String - ToLowerFirstCharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "expected: 'xoXoXoX'\nactual:   'XoXoXoX'"),

            // String - ToUpperFirstCharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "expected: 'XOxOxOx'\nactual:   'xOxOxOx'")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Trino).getFirst())
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
