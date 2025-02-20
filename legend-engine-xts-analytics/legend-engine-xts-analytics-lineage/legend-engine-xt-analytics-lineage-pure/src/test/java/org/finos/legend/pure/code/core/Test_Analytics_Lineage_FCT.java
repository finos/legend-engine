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
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.test.fct.FCTTestSuitBuilder;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Analytics_Lineage_FCT  extends LineageFCTReport
{

    public static Test suite()
    {

        CompiledExecutionSupport support = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        MutableMap<String, String> exclusions = Maps.mutable.empty();

        TestSuite suite = FCTTestSuitBuilder.buildFCTTestSuiteWithExecutorFunctionFromList(testCollection(support), exclusions, "meta::analytics::lineage::computeTestLineageWrapper_TestParameters_1__Boolean_1_", false, true,support);
        return wrapSuite(
                () -> true,
                () -> suite,
                () -> false,
                Lists.mutable.empty()
        );
    }
}
