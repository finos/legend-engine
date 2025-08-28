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

package org.finos.legend.engine.query.sql.reversePCT;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.code.core.CoreExternalSQLReversePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.generated.Root_meta_external_query_sql_reversePCT_framework_ReversesForSource;
import org.finos.legend.pure.generated.core_external_query_sql_reverse_pct_grammar_grammar;
import org.finos.legend.pure.generated.core_external_query_sql_reverse_pct_relation_relation;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import static org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled.getClassLoaderExecutionSupport;

public class Test_Query_SQL_RelationFunctions_Reverse_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalSQLReversePCTCodeRepositoryProvider.SQLAdapter;
    private static final String platform = "compiled";
    private static final RichIterable<? extends Root_meta_external_query_sql_reversePCT_framework_ReversesForSource> reverseInfo = core_external_query_sql_reverse_pct_relation_relation.Root_meta_external_query_sql_reversePCT_framework_relation_reverses__ReversesForSource_MANY_(getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader()));
    private static final MutableList<ExclusionSpecification> expectedFailures = ExpectedFailuresBuilder.build(reverseInfo);

    static
    {
        expectedFailures.addAll(ExpectedFailuresBuilder.getMissingExpectedFailures(PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter), reverseInfo));
    }

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.empty()
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
