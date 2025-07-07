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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalPostgresPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Postgres_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalPostgresPCTCodeRepositoryProvider.postgresAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiNewline_Function_1__Boolean_1_", "Unexpected token", AdapterQualifier.unsupportedFeature),

            //char
            one("meta::pure::functions::string::tests::char::testEmptyChar_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: null character not permitted", AdapterQualifier.needsInvestigation),

            //base64
            one("meta::pure::functions::string::tests::base64::testDecodeBase64NoPadding_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: invalid base64 end sequence\n  Hint: Input data is missing padding, is truncated, or is otherwise corrupted.", AdapterQualifier.unsupportedFeature),

            //jarowinklersimilarity
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'jaroWinklerSimilarity' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //levenshteindistance
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::levenshteinDistance::testLevenshteinDistanceNotEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'levenshteinDistance' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //tolowerfirstcharacter
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'xoXoXoX'\nactual:   'XoXoXoX'\"", AdapterQualifier.needsInvestigation),

            //toupperfirstcharacter
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'XOxOxOx'\nactual:   'xOxOxOx'\"", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
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
