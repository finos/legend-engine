//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.code.core.relational;

import junit.framework.TestSuite;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import static org.finos.legend.pure.m3.fct.shared.FCTTools.isFCTTest;

public class Test_Pure_Relational
{
    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        Predicate<? super CoreInstance> filter  = ci -> PureTestBuilder.satisfiesConditions(ci, executionSupport.getProcessorSupport()) && !isFCTTest(ci, executionSupport.getProcessorSupport());
        //NOTE- we are not collecting parameterized test collection in meta::relational here
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::relational", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::alloy::objectReference", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::alloy::service::execution", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::lineage", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::graphFetch", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::router", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::alloy::connections", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::tds", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::executionPlan", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::mapping", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::milestoning", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::protocols::pure", executionSupport.getProcessorSupport(), filter), executionSupport));
        suite.addTest(PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::relational::tests::csv", executionSupport.getProcessorSupport(), filter), executionSupport));

        return suite;
    }
}
