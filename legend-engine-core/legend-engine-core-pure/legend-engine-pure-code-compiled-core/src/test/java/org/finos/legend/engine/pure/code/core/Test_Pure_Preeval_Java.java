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

package org.finos.legend.engine.pure.code.core;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Pure_Preeval_Java
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    static final ImmutableSet<String> SKELETON = Sets.immutable.with(
            "testPrerouting5", "testFilterNoSimplification", "testToOneManyElimination2", "testToOneElimination2",
            "testFromWith", "testFromStopFunctions", "testPreroutingRemoveUnnecessaryStatements", "testLambdaParamOverride",
            "testPrerouting_parameterAssignedToVariable", "testPrerouting9", "testLambdaParamOverride2");
    static final ImmutableSet<String> REACTIVATION = Sets.immutable.empty();
    static final ImmutableSet<String> LAMBDA_HOLDERS = Sets.immutable.empty();
    static final ImmutableSet<String> DEFERRED_TO_P2 = Sets.immutable.empty();

    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        suite.addTest(withImplementation("JAVA", targets(executionSupport)));
        suite.addTest(withImplementation("SHADOW", targets(executionSupport)));
        return suite;
    }

    static ImmutableSet<String> enabledTargets()
    {
        return SKELETON.newWithAll(REACTIVATION).newWithAll(LAMBDA_HOLDERS).newWithoutAll(DEFERRED_TO_P2);
    }

    private static TestSuite targets(CompiledExecutionSupport executionSupport)
    {
        ImmutableSet<String> enabled = enabledTargets();
        return PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::router::preeval::tests", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), ci -> enabled.contains(((Function<?>) ci)._functionName()) && PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport())), executionSupport);
    }

    private static Test withImplementation(String implementation, Test tests)
    {
        return new TestSetup(tests)
        {
            private String previous;

            @Override
            protected void setUp()
            {
                this.previous = System.getProperty(PROPERTY);
                System.setProperty(PROPERTY, implementation);
            }

            @Override
            protected void tearDown()
            {
                if (this.previous == null)
                {
                    System.clearProperty(PROPERTY);
                }
                else
                {
                    System.setProperty(PROPERTY, this.previous);
                }
            }
        };
    }
}
