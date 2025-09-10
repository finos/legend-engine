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

package org.finos.legend.engine.plan.execution.stores.relational.test.athena.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalAthenaPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Athena_UnclassifiedFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreRelationalAthenaPCTCodeRepositoryProvider.athenaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //Ascii
            one("meta::pure::functions::string::tests::ascii::testAsciiNewline_Function_1__Boolean_1_", "Unexpected token"),

            //Failures due to substring issue
            one("meta::pure::functions::string::tests::tolowerfirstcharacter::TestToLowerFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'xoXoXoX'\nactual:   'XoXoXoX'\""),
            one("meta::pure::functions::string::tests::toupperfirstcharacter::TestToUpperFirstCharacter_Function_1__Boolean_1_", "\"\nexpected: 'XOxOxOx'\nactual:   'xOxOxOx'\""),

            //Jarowinkler Similarity
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityEqual_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - jarowinkler_similarity\""),
            one("meta::pure::functions::string::tests::jaroWinklerSimilarity::testJaroWinklerSimilarityNotEqual_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - jarowinkler_similarity\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Athena).getFirst())
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
