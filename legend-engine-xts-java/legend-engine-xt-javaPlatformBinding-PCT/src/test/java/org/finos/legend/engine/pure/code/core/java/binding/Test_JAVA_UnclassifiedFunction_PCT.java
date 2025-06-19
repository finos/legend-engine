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
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_UnclassifiedFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreUnclassifiedFunctionsCodeRepositoryProvider.unclassifiedFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //ascii
            pack("meta::pure::functions::string::tests::ascii", "meta::pure::functions::string::ascii_String_1__Integer_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),

            //char
            pack("meta::pure::functions::string::tests::char", "meta::pure::functions::string::char_Integer_1__String_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),

            // coalesce
            one("meta::pure::functions::flow::test::coalesce::coalesce_AllEmpty_Function_1__Boolean_1_", "error: invalid method declaration; return type required\n    public static ? execute(IExecutionNodeContext context)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::flow::test::coalesce::coalesce2_AllEmpty_Function_1__Boolean_1_", "error: invalid method declaration; return type required\n    public static ? execute(IExecutionNodeContext context)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::flow::test::coalesce::coalesce3_AllEmpty_Function_1__Boolean_1_", "error: invalid method declaration; return type required\n    public static ? execute(IExecutionNodeContext context)", AdapterQualifier.needsInvestigation),

            //repeatString
            one("meta::pure::functions::string::tests::repeatstring::testRepeatStringNoString_Function_1__Boolean_1_", "error: incompatible types: no instance(s) of type variable(s) T exist so that java.util.List<T> conforms to java.lang.String", AdapterQualifier.needsInvestigation)
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
