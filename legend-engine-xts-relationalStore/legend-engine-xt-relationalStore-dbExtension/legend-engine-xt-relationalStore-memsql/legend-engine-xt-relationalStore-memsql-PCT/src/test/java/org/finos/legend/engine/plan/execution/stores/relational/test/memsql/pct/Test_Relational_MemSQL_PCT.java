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

package org.finos.legend.engine.plan.execution.stores.relational.test.memsql.pct;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.code.core.CoreScenarioQuantCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalMemSQLCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.code.core.VariantCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_MemSQL_PCT
{
    private static final Adapter MEMSQL_ADAPTER = CoreRelationalMemSQLCodeRepositoryProvider.memsqlAdapter;
    private static final String PLATFORM = "compiled";

    public static Test suite()
    {
        // build PCT test suite for all scopes
        return wrapSuite(
                () -> true,
                () ->
                {
                    TestSuite suite = new TestSuite(Test_Relational_MemSQL_PCT.class.getName());
                    suite.addTest(namedPCTSurveyorSuite(EssentialFunctions.class.getSimpleName(), EssentialFunctions.REPORT_SCOPE, EssentialFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(GrammarFunctions.class.getSimpleName(), GrammarFunctions.REPORT_SCOPE, GrammarFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(RelationFunctions.class.getSimpleName(), RelationFunctions.REPORT_SCOPE, RelationFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(ScenarioQuantFunctions.class.getSimpleName(), ScenarioQuantFunctions.REPORT_SCOPE, ScenarioQuantFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(StandardFunctions.class.getSimpleName(), StandardFunctions.REPORT_SCOPE, StandardFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(UnclassifiedFunctions.class.getSimpleName(), UnclassifiedFunctions.REPORT_SCOPE, UnclassifiedFunctions.MANIFEST_PATH));
                    suite.addTest(namedPCTSurveyorSuite(VariantFunctions.class.getSimpleName(), VariantFunctions.REPORT_SCOPE, VariantFunctions.MANIFEST_PATH));
                    return suite;
                },
                () -> false,
                memsqlTestServerResources()
        );
    }

    // build single scope test suite
    private static Test buildSingleScopeSuite(String displayName, ReportScope reportScope, String manifestPath)
    {
        return wrapSuite(
                () -> true,
                () -> namedPCTSurveyorSuite(displayName, reportScope, manifestPath),
                () -> false,
                memsqlTestServerResources()
        );
    }

    // Wraps suite so IntelliJ / Surefire output shows a human-friendly label
    private static TestSuite namedPCTSurveyorSuite(String displayName, ReportScope reportScope, String manifestPath)
    {
        TestSuite pctSuite = PureTestBuilderCompiled.buildPCTSurveyorSuite(reportScope, manifestPath);
        pctSuite.setName("Surveyor PCT Tests for " + displayName);
        return pctSuite;
    }

    private static MutableList<TestServerResource> memsqlTestServerResources()
    {
        return Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.MemSQL).getFirst());
    }

    private abstract static class MemSQLPCTReportConfiguration extends PCTReportConfiguration
    {
        private final ReportScope reportScope;
        private final String manifestPath;

        protected MemSQLPCTReportConfiguration(ReportScope reportScope, String manifestPath)
        {
            this.reportScope = reportScope;
            this.manifestPath = manifestPath;
        }

        @Override
        public MutableList<ExclusionSpecification> expectedFailures()
        {
            return buildExpectedFailures(this.manifestPath);
        }

        @Override
        public ReportScope getReportScope()
        {
            return this.reportScope;
        }

        @Override
        public Adapter getAdapter()
        {
            return MEMSQL_ADAPTER;
        }

        @Override
        public String getPlatform()
        {
            return PLATFORM;
        }
    }

    public static class EssentialFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = PlatformCodeRepositoryProvider.essentialFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/EssentialFunctions_manifest.json";

        public EssentialFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(EssentialFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class GrammarFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = PlatformCodeRepositoryProvider.grammarFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/GrammarFunctions_manifest.json";

        public GrammarFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(GrammarFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class RelationFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = RelationCodeRepositoryProvider.relationFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/RelationFunctions_manifest.json";

        public RelationFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(RelationFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class ScenarioQuantFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = CoreScenarioQuantCodeRepositoryProvider.scenario_Quant_Functions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/ScenarioQuantFunctions_manifest.json";

        public ScenarioQuantFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(ScenarioQuantFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class StandardFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/StandardFunctions_manifest.json";

        public StandardFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(StandardFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class UnclassifiedFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/UnclassifiedFunctions_manifest.json";

        public UnclassifiedFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(UnclassifiedFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }

    public static class VariantFunctions extends MemSQLPCTReportConfiguration
    {
        private static final ReportScope REPORT_SCOPE = VariantCodeRepositoryProvider.variantFunctions;
        private static final String MANIFEST_PATH = "pct-manifests/relational-memsql/VariantFunctions_manifest.json";

        public VariantFunctions()
        {
            super(REPORT_SCOPE, MANIFEST_PATH);
        }

        public static Test suite()
        {
            return buildSingleScopeSuite(VariantFunctions.class.getSimpleName(), REPORT_SCOPE, MANIFEST_PATH);
        }
    }
}
