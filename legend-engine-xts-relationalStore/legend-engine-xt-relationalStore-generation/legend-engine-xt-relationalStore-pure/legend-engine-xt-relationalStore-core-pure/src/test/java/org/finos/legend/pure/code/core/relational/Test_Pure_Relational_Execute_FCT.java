/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

package org.finos.legend.pure.code.core.relational;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.FCTTestSuitBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.fct.FCTTestSuitBuilder.EXECUTE_FUNCTION;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Pure_Relational_Execute_FCT extends FCTReport
{
    public static Test suite()
    {
        CompiledExecutionSupport support = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        support.getConsole().disable();
        MutableMap<String, String> exclusions =  FCTReport.explodeExpectedFailures(getExpectedFailures(),support.getProcessorSupport());
        return wrapSuite(
                () -> true,  //
                () -> FCTTestSuitBuilder.buildFCTTestSuiteWithExecutorFunctionFromList(testCollection(), exclusions, "meta::relational::fct::relationalExecuteWrapperLegendQuery_TestParameters_1__Function_1_", "meta::relational::fct::relationalRunTime_FCTMappingTest_1__Runtime_1_", "meta::relational::fct::relationalTestSetUp_FCTMappingTest_1__Runtime_1_",false, support),
                () -> false,
                Lists.mutable.empty()
        );
    }


    private static  MutableList<ExclusionSpecification> getExpectedFailures()
    {
        return Lists.mutable.empty();
    }

    private static ImmutableList<TestCollection> testCollection()
    {  CompiledExecutionSupport support = getClassLoaderExecutionSupport();
        return Lists.immutable.with(
                Test_Pure_Relational_FCT_Collection.buildCollection(support));
    }

    @Override
    public ImmutableList<TestCollection> getTestCollection()
    {
        return testCollection();
    }

    @Override
    public String getreportID()
    {
        return "RelationalFCTExecution";
    }

    @Override
    public String getStoreID()
    {
        return "relationalStore";
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return getExpectedFailures();
    }
}
