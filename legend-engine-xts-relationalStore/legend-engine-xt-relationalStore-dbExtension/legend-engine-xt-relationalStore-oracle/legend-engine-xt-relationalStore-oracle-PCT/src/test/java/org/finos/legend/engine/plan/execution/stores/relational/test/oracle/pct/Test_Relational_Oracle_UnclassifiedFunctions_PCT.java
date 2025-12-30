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

public class Test_Relational_Oracle_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //char
            one("meta::pure::functions::string::tests::ascii::testAsciiEmptyChar_Function_1__Boolean_1_", "\"\nexpected: [0]\nactual:   []\"", AdapterQualifier.needsInvestigation),

            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"CONVERT_FROM\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64RoundTrip_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"CONVERT_FROM\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testDecodeBase64_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"CONVERT_FROM\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64RoundTrip_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"ENCODE\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::base64::testEncodeBase64_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"ENCODE\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),

            // hash
            one("meta::pure::functions::hash::tests::testMD5Hash_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"MD5\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::hash::tests::testSHA1Hash_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"ENCODE\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::hash::tests::testSHA256Hash_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"ENCODE\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),

            // left
            pack("meta::pure::functions::string::tests::left", "java.sql.SQLSyntaxErrorException: ORA-00904: \"LEFT\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsImplementation),

            // lpad
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: ['abcd']\nactual:   []\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::lpad::testLpadEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['???']\nactual:   []\"", AdapterQualifier.needsInvestigation),

            // matches
            one("meta::pure::functions::string::tests::matches::testMatchesNoMatch_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00911: invalid character\n\nhttps://docs.oracle.com/error-help/db/ora-00911/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::matches::testMatches_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00911: invalid character\n\nhttps://docs.oracle.com/error-help/db/ora-00911/", AdapterQualifier.needsInvestigation),

            //jarowinklersimilarity
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //levenshteindistance
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacterEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\"", AdapterQualifier.needsInvestigation),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacterEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['']\nactual:   []\"", AdapterQualifier.needsInvestigation),

            //currentUserId
            one("meta::pure::functions::runtime::currentUserId::testCurrentUserId_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"CURRENT_USER\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsInvestigation),

            // regexp
            pack("meta::pure::functions::string::tests::regexpCount", "\"[unsupported-api] The function 'regexpCount' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::string::tests::regexpExtract", "\"[unsupported-api] The function 'regexpExtract' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::string::tests::regexpIndexOf", "\"[unsupported-api] The function 'regexpIndexOf' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::string::tests::regexpLike", "\"[unsupported-api] The function 'regexpLike' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::string::tests::regexpReplace", "\"[unsupported-api] The function 'regexpReplace' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // repeat string
            pack("meta::pure::functions::string::tests::repeatstring", "java.sql.SQLSyntaxErrorException: ORA-00904: \"REPEAT\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsInvestigation),

            // right
            pack("meta::pure::functions::string::tests::right", "java.sql.SQLSyntaxErrorException: ORA-00904: \"RIGHT\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsInvestigation),

            // rpad
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyChar_Function_1__Boolean_1_", "\"\nexpected: ['abcd']\nactual:   []\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::rpad::testRpadEmptyString_Function_1__Boolean_1_", "\"\nexpected: ['???']\nactual:   []\"", AdapterQualifier.needsInvestigation),

            // split part
            pack("meta::pure::functions::string::tests::splitPart", "java.sql.SQLSyntaxErrorException: ORA-00904: \"SPLIT_PART\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/", AdapterQualifier.needsInvestigation)
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
