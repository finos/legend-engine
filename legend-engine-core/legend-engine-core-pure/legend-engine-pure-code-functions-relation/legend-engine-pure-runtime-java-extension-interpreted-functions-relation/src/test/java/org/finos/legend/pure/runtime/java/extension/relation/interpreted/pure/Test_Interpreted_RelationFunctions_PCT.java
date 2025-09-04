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

package org.finos.legend.pure.runtime.java.extension.relation.interpreted.pure;

import junit.framework.Test;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

public class Test_Interpreted_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = PlatformCodeRepositoryProvider.nativeAdapter;
    private static final String platform = "interpreted";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            // multiplicity many, when is single value does not print []
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_",
                    "\"\nexpected: '#TDS\n   id,payload,divBy2\n   1,[1,2,3],[2]\n   2,[4,5,6],[4,6]\n   3,[7,8,9],[8]\n   4,[10,11,12],[10,12]\n   5,[13,14,15],[14]\n#'\nactual:   '#TDS\n   id,payload,divBy2\n   1,[1,2,3],2\n   2,[4,5,6],[4,6]\n   3,[7,8,9],8\n   4,[10,11,12],[10,12]\n   5,[13,14,15],14\n#'\""),

            // inference problem
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_",
                    "Error instantiating the type 'SortInfo<T>'. Could not resolve type for the property 'column': ColSpec<T>"),

            one("meta::pure::functions::relation::tests::composition::testExtendAddOnNull_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,newCol\n   null,0,null\n   8,1,8\n   null,1,8\n   null,1,8\n   1,2,6\n   5,2,6\n   3,3,10\n   7,3,10\n   4,4,4\n   9,5,9\n#'\nactual:   '#TDS\n   id,grp,newCol\n   null,0,0\n   8,1,8\n   null,1,8\n   null,1,8\n   1,2,6\n   5,2,6\n   3,3,10\n   7,3,10\n   4,4,4\n   9,5,9\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::composition::testExtendFilterOutNull_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,80\n   200,3,30,80\n   200,1,10,80\n   200,1,10,80\n   100,3,30,60\n   100,2,20,60\n   100,1,10,60\n   0,1,10,20\n   0,1,10,20\n#'\nactual:   '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,110\n   200,3,30,110\n   200,1,10,110\n   200,1,10,110\n   100,3,30,110\n   100,2,20,110\n   100,1,10,110\n   0,1,10,50\n   0,1,10,50\n#'\"", AdapterQualifier.needsInvestigation),
            // empty vs null
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,payload,fromRow,toRow,sliced\n   0,null,1,2,null\n   1,[1,2,3,4,5,6],2,3,[3]\n   2,[1,2,3,4,5,6],0,4,[1,2,3,4]\n   3,[1,2,3,4,5,6],-3,-1,[]\n#'\nactual:   '#TDS\n   id,payload,fromRow,toRow,sliced\n   0,null,1,2,null\n   1,[1,2,3,4,5,6],2,3,3\n   2,[1,2,3,4,5,6],0,4,[1,2,3,4]\n   3,[1,2,3,4,5,6],-3,-1,null\n#'\"", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return PureTestBuilderInterpreted.buildPCTTestSuite(reportScope, expectedFailures, adapter);
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
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
