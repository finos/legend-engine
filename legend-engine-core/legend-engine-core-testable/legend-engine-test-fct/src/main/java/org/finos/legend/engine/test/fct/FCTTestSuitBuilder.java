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

package org.finos.legend.engine.test.fct;

import junit.framework.TestSuite;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.pure.m3.execution.test.TestCollection.collectTests;

public class FCTTestSuitBuilder
{
    public static TestSuite buildFCTTestSuiteWithExecutorFunction(TestCollection collection,  MutableMap<String, String> exclusions, String function, ExecutionSupport executionSupport)
    {
        PureTestBuilder.F2<CoreInstance, MutableList<Object>, Object> testExecutor = (a, b) ->
                PureTestBuilderCompiled.executeFn(a, function, exclusions, executionSupport, b);
        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilder.buildSuite(collection, testExecutor, executionSupport));
        return suite;
    }

    public static TestCollection buildFCTTestCollection(String path, ProcessorSupport processorSupport)
    {
        return collectTests(path, processorSupport, (node) ->
        {
            return isFCTTest(node, processorSupport);
        });
    }

    public static boolean isFCTTest(CoreInstance node, ProcessorSupport processorSupport)
    {
        return Profile.hasStereotype(node, "meta::pure::test::fct::FCT", "test", processorSupport);
    }

}