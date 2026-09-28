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
import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.factory.Bags;
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

    private static final String INCLUDE_KNOWN_DIVERGENT = "legend.engine.preeval.test.includeKnownDivergent";

    static final ImmutableSet<String> KNOWN_DIVERGENT = Sets.immutable.with(
            "tesColumnEvalOnRelation", "tesColumnEvalOnRelationWithCast", "testFilterFalseConstantSimplification",
            "testFilterFalseSimplification", "testFilterFalseSimplification2", "testFilterFalseSimplificationReturnType",
            "testFilterTrueConstantSimplification", "testFilterTrueSimplification", "testFilterTrueSimplification2", "testGetGenericType",
            "testPrerouting33", "testPrerouting34", "testPrerouting40b", "testPrerouting_castEmptyCollection",
            "testToOneElimination3", "testToOneManyElimination3");

    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        suite.addTest(withImplementation("JAVA", targets(executionSupport)));
        suite.addTest(withImplementation("SHADOW", targets(executionSupport)));
        return suite;
    }

    private static TestSuite targets(CompiledExecutionSupport executionSupport)
    {
        boolean includeKnownDivergent = Boolean.getBoolean(INCLUDE_KNOWN_DIVERGENT);
        MutableBag<String> collected = Bags.mutable.empty();
        TestCollection tests = TestCollection.collectTests("meta::pure::router::preeval::tests", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), ci ->
        {
            if (!PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport()))
            {
                return false;
            }
            String name = ((Function<?>) ci)._functionName();
            collected.add(name);
            return includeKnownDivergent || !KNOWN_DIVERGENT.contains(name);
        });
        ImmutableSet<String> unmatched = KNOWN_DIVERGENT.reject(name -> collected.occurrencesOf(name) == 1);
        if (unmatched.notEmpty())
        {
            throw new IllegalStateException("KNOWN_DIVERGENT names that do not match exactly one collected preeval test: " + unmatched.toSortedList().makeString(", "));
        }
        return PureTestBuilderCompiled.buildSuite(tests, executionSupport);
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
