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

package org.finos.legend.engine.plan.execution.stores.relational.test.clickhouse.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalClickHousePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_ClickHouse_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalClickHousePCTCodeRepositoryProvider.clickhouseAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // DecodeBase64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "java.sql.SQLException: Code: 117. DB::Exception: Failed to base64Decode input 'SGVsbG8sIFdvcmxkIQ': In scope SELECT base64Decode('SGVsbG8sIFdvcmxkIQ')."),

            // Lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: 'abcd'\nactual:   '      abcd'\""),

            // RegexpCount
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpCount::testRegexpCount_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),

            // RegexpExtract
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtractAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpExtract::testRegexpExtract_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),

            // RegexpIndexOf
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_GroupNumber_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),

            // RegexpLike
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_NonNewlineSensitive_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),

            // RegexpReplace
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplaceAll_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::regexpReplace::testregexpReplace_Function_1__Boolean_1_", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),

            // Rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: 'abcd'\nactual:   'abcd      '\""),

            // SplitPart
            one("meta::pure::functions::string::tests::splitPart::testSplitPartEmptyToken_Function_1__Boolean_1_", "\"\nexpected: 'Hello World'\nactual:   'H'\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.ClickHouse).getFirst())
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
