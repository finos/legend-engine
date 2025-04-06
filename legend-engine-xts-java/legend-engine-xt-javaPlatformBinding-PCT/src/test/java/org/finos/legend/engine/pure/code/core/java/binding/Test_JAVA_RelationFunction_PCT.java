// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_RelationFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            pack("meta::pure::functions::relation::tests::concatenate", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::drop", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::extend", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::cumulativeDistribution", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::denseRank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::first", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::lag", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::last", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::lead", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::nth", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::ntile", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::percentRank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::rank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::rowNumber", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::distinct", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::filter", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::groupBy", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::join", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::limit", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::pivot", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::project", "\"meta::pure::functions::relation::project_C_MANY__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProject_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::rename", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::select", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::size", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::slice", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::sort", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::asOfJoin", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            pack("meta::pure::functions::relation::tests::composition", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::testWindowFunctionsAfterProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::sort_Relation_1__SortInfo_MANY__Relation_1_ is not supported yet!\""),
            pack("meta::pure::functions::relation::tests::write", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"")
    );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
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
