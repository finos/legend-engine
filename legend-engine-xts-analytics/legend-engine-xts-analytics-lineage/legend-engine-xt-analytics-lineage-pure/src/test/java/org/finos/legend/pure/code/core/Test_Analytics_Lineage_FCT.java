//  Copyright 2024 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.pure.code.core;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.test.fct.FCTReport;
import org.finos.legend.engine.test.fct.FCTTestCollection;
import org.finos.legend.engine.test.fct.FCTTestSuitBuilder;
import org.finos.legend.pure.code.core.relational.Test_Pure_Relational_FCT_Collection;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Analytics_Lineage_FCT extends FCTReport
{

    public static Test suite()
    {

        CompiledExecutionSupport support = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        MutableMap<String, String> exclusions = Maps.mutable.empty();

        TestSuite suite = FCTTestSuitBuilder.buildFCTTestSuiteWithExecutorFunctionFromList(testCollection(support), exclusions, "meta::analytics::lineage::computeTestLineageWrapper_TestParameters_1__Function_1_", false, support);
        return wrapSuite(
                () -> true,
                () -> suite,
                () -> false,
                Lists.mutable.empty()
        );
    }

    private static ImmutableList<FCTTestCollection> testCollection(CompiledExecutionSupport support)
    {      return Lists.immutable.with(
                Test_Pure_Relational_FCT_Collection.buildCollection(support));
    }


    @Override
    public  ImmutableList<FCTTestCollection> getTestCollection()
    {
        return testCollection(PureTestBuilderCompiled.getClassLoaderExecutionSupport());
    }

    @Override
    public String getreportID()
    {
        return "LineageFCT";
    }

    @Override
    public String getStoreID()
    {
        return "relationalStore";
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return Lists.mutable.empty();
    }
}