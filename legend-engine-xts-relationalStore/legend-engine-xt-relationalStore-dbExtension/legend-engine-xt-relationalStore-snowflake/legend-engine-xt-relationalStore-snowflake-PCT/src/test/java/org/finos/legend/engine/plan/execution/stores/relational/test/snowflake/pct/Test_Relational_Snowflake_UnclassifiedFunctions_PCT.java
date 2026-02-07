// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.snowflake.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSnowflakeCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Snowflake_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalSnowflakeCodeRepositoryProvider.snowflakeAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: The following string is not a legal base64-encoded value: 'SGVsbG8sIFdvcmxkIQ'", AdapterQualifier.unsupportedFeature),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterAlreadyLower_Function_1__Boolean_1_", "\"\nexpected: 'xOxOxOx'\nactual:   'xxOxOxOx'\""),
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterNumber_Function_1__Boolean_1_", "\"\nexpected: '1isOne'\nactual:   '11isOne'\""),
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'xoXoXoX'\nactual:   'xXoXoXoX'\""),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterAlreadyLower_Function_1__Boolean_1_", "\"\nexpected: 'XoXoXoX'\nactual:   'XXoXoXoX'\""),
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterNumber_Function_1__Boolean_1_", "\"\nexpected: '1isOne'\nactual:   '11isOne'\""),
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'XOxOxOx'\nactual:   'XxOxOxOx'\""),

            // 0-indexed (Pure) vs 1-indexed (SQL standard)
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_Function_1__Boolean_1_", "\"\nexpected: 3\nactual:   4\""),
            one("meta::pure::functions::string::tests::regexpIndexOf::testRegexpIndexOf_GroupNumber_Function_1__Boolean_1_", "\"\nexpected: 3\nactual:   4\""),

            // Multiline regexp parameter needs to be investigated
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_CaseInsensitive_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::string::tests::regexpLike::testRegexpLike_Multiline_NonNewlineSensitive_Function_1__Boolean_1_", "\"Assert failed\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Snowflake).getFirst())
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
