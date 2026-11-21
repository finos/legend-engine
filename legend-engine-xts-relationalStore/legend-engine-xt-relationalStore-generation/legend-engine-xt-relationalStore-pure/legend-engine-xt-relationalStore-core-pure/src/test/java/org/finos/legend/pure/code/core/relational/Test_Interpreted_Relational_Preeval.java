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

package org.finos.legend.pure.code.core.relational;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

import java.util.Enumeration;

public class Test_Interpreted_Relational_Preeval
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    private static final String TEST_NAME = "testPrerouting42";

    public static TestSuite suite()
    {
        TestSuite all = PureTestBuilderInterpreted.buildSuite("meta::pure::router::preeval::tests");
        Test target = findSingleTest(all);
        TestSuite suite = new TestSuite();
        suite.addTest(withImplementation("PURE", target));
        suite.addTest(withImplementation("JAVA", target));
        suite.addTest(withImplementation("SHADOW", target));
        return suite;
    }

    private static Test findSingleTest(TestSuite source)
    {
        Test[] matched = new Test[1];
        int[] count = new int[1];
        collect(source, matched, count);
        if (count[0] != 1)
        {
            throw new IllegalStateException("Expected exactly one collected preeval test named '" + TEST_NAME + "', found " + count[0]);
        }
        return matched[0];
    }

    private static void collect(Test test, Test[] matched, int[] count)
    {
        if (test instanceof TestSuite)
        {
            for (Enumeration<Test> tests = ((TestSuite) test).tests(); tests.hasMoreElements(); )
            {
                collect(tests.nextElement(), matched, count);
            }
        }
        else if (test instanceof TestCase && TEST_NAME.equals(((TestCase) test).getName()))
        {
            matched[0] = test;
            count[0]++;
        }
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
