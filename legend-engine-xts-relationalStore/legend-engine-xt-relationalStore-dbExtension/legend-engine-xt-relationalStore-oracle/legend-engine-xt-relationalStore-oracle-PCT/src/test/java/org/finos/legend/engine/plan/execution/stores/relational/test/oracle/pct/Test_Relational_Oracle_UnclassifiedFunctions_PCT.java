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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalOraclePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Oracle_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalOraclePCTCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //char
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "\"\nexpected: [0]\nactual:   []\""),

            //base64


            // hash
            one("meta::pure::functions::hash::tests::testMD5Hash_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"MD5\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/"),
            one("meta::pure::functions::hash::tests::testSHA1Hash_Function_1__Boolean_1_", "Failed to parse input: null"),
            one("meta::pure::functions::hash::tests::testSHA256Hash_Function_1__Boolean_1_", "Failed to parse input: null"),

            // left
            one("meta::pure::functions::string::tests::left::testLeftEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\""),

            // lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: ['abcd']\nactual:   []\""),
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['???']\nactual:   []\""),

            // matches
            one("meta::pure::functions::string::tests::matches::testMatchesNoMatch_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00911: invalid character\n\nhttps://docs.oracle.com/error-help/db/ora-00911/"),
            one("meta::pure::functions::string::tests::matches::testMatches_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00911: invalid character\n\nhttps://docs.oracle.com/error-help/db/ora-00911/"),

            //jarowinklersimilarity
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\""),

            //levenshteindistance
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\""),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\""),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\""),

            // regexp
            pack("meta::pure::functions::string::tests::regexpCount", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::string::tests::regexpExtract", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::string::tests::regexpIndexOf", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::string::tests::regexpLike", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::string::tests::regexpReplace", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\""),

            // repeat string
            pack("meta::pure::functions::string::tests::repeatstring", "java.sql.SQLSyntaxErrorException: ORA-00904: \"REPEAT\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/"),

            // right
            one("meta::pure::functions::string::tests::right::testRightEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\""),

            // rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: ['abcd']\nactual:   []\""),
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['???']\nactual:   []\""),

            // split part
            pack("meta::pure::functions::string::tests::splitPart", "java.sql.SQLSyntaxErrorException: ORA-00904: \"SPLIT_PART\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/")
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
