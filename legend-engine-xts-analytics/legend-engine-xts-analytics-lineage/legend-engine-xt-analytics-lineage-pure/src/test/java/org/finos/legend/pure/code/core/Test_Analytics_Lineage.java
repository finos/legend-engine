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

import junit.framework.TestSuite;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import static org.finos.legend.pure.m3.fct.shared.FCTTools.isFCTTest;

public class Test_Analytics_Lineage
{
    public static TestSuite suite()
    {

        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        Predicate<? super CoreInstance> filter  = ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport()) && !isFCTTest(ci, executionSupport.getProcessorSupport());

        TestSuite suite = new TestSuite();
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::analytics::lineage::tests", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::lineage::scanExecutes::test", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::lineage::scanProject::test", executionSupport.getProcessorSupport(), filter), executionSupport));
        return suite;
    }
}
